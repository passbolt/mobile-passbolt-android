package com.passbolt.mobile.android.feature.home.screen

import androidx.annotation.VisibleForTesting
import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.core.commonresource.ResourceInteractor
import com.passbolt.mobile.android.core.commonresource.ResourceTypeFactory
import com.passbolt.mobile.android.core.commonresource.usecase.DeleteResourceUseCase
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.mvp.session.runAuthenticatedOperation
import com.passbolt.mobile.android.feature.autofill.resources.FetchAndUpdateDatabaseUseCase
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.parser.SecretParser
import com.passbolt.mobile.android.mappers.ResourceMenuModelMapper
import com.passbolt.mobile.android.storage.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */
class HomePresenter(
    coroutineLaunchContext: CoroutineLaunchContext,
    private val resourcesInteractor: ResourceInteractor,
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val fetchAndUpdateDatabaseUseCase: FetchAndUpdateDatabaseUseCase,
    private val secretInteractor: SecretInteractor,
    private val resourceMatcher: SearchableMatcher,
    private val resourceTypeFactory: ResourceTypeFactory,
    private val secretParser: SecretParser,
    private val resourceMenuModelMapper: ResourceMenuModelMapper,
    private val deleteResourceUseCase: DeleteResourceUseCase
) : BaseAuthenticatedPresenter<HomeContract.View>(coroutineLaunchContext), HomeContract.Presenter {

    override var view: HomeContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private var currentSearchText: String = ""
    private var allItemsList: List<ResourceModel> = emptyList()
    private var currentMoreMenuResource: ResourceModel? = null
    private var userAvatarUrl: String? = null
    private val searchInputEndIconMode
        get() = if (currentSearchText.isBlank()) SearchInputEndIconMode.AVATAR else SearchInputEndIconMode.CLEAR

    override fun attach(view: HomeContract.View) {
        super<BaseAuthenticatedPresenter>.attach(view)
        runWhileShowingListProgress { fetchResources() }
        userAvatarUrl = getSelectedAccountDataUseCase.execute(Unit).avatarUrl
            .also { view.displaySearchAvatar(it) }
    }

    override fun userAuthenticated() {
        runWhileShowingListProgress { fetchResources() }
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }

    override fun searchClearClick() {
        view?.clearSearchInput()
    }

    override fun searchTextChange(text: String) {
        currentSearchText = text
        processSearchIconChange()
        filterList()
    }

    private fun processSearchIconChange() {
        when (searchInputEndIconMode) {
            SearchInputEndIconMode.AVATAR -> view?.displaySearchAvatar(userAvatarUrl)
            SearchInputEndIconMode.CLEAR -> view?.displaySearchClearIcon()
        }
    }

    private suspend fun fetchResources() {
        when (val result =
            runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                resourcesInteractor.fetchResourcesWithTypes()
            }) {
            is ResourceInteractor.Output.Failure -> {
                view?.showError()
            }
            is ResourceInteractor.Output.Success -> {
                allItemsList = result.resources
                fetchAndUpdateDatabaseUseCase.execute(FetchAndUpdateDatabaseUseCase.Input(allItemsList))
                displayResources()
            }
        }
        view?.hideRefreshProgress()
    }

    private fun runWhileShowingListProgress(action: suspend () -> Unit) {
        scope.launch {
            view?.showProgress()
            action()
            view?.hideProgress()
        }
    }

    private fun displayResources() {
        if (allItemsList.isEmpty()) {
            view?.showEmptyList()
        } else {
            if (currentSearchText.isEmpty()) {
                view?.showPasswords(allItemsList)
            } else {
                filterList()
            }
        }
    }

    private fun filterList() {
        val filtered = allItemsList.filter {
            resourceMatcher.matches(it, currentSearchText)
        }
        if (filtered.isEmpty()) {
            view?.showSearchEmptyList()
        } else {
            view?.showPasswords(filtered)
        }
    }

    override fun refreshClick() {
        runWhileShowingListProgress { fetchResources() }
    }

    override fun refreshSwipe() {
        scope.launch {
            fetchResources()
        }
    }

    override fun moreClick(resourceModel: ResourceModel) {
        currentMoreMenuResource = resourceModel
        view?.navigateToMore(resourceMenuModelMapper.map(resourceModel))
    }

    override fun itemClick(resourceModel: ResourceModel) {
        view?.navigateToDetails(resourceModel)
    }

    override fun menuLaunchWebsiteClick() {
        currentMoreMenuResource?.let {
            if (!it.url.isNullOrEmpty()) {
                view?.openWebsite(it.url!!)
            }
        }
    }

    override fun menuCopyUsernameClick() {
        currentMoreMenuResource?.let {
            view?.addToClipboard(USERNAME_LABEL, it.username.orEmpty())
        }
    }

    override fun menuCopyUrlClick() {
        currentMoreMenuResource?.let {
            view?.addToClipboard(URL_LABEL, it.url.orEmpty())
        }
    }

    override fun newResourceAdded() {
        view?.showResourceAddedSnackbar()
        scope.launch {
            fetchResources()
        }
    }

    override fun menuCopyPasswordClick() {
        scope.launch {
            val resourceTypeEnum = resourceTypeFactory.getResourceTypeEnum(currentMoreMenuResource!!.resourceTypeId)
            doAfterFetchAndDecrypt { decryptedSecret ->
                val password = secretParser.extractPassword(resourceTypeEnum, decryptedSecret)
                view?.addToClipboard(SECRET_LABEL, password)
            }
        }
    }

    override fun menuCopyDescriptionClick() {
        scope.launch {
            when (val resourceTypeEnum = resourceTypeFactory.getResourceTypeEnum(
                currentMoreMenuResource!!.resourceTypeId
            )) {
                ResourceTypeFactory.ResourceTypeEnum.SIMPLE_PASSWORD -> {
                    view?.addToClipboard(DESCRIPTION_LABEL, currentMoreMenuResource!!.description.orEmpty())
                }
                ResourceTypeFactory.ResourceTypeEnum.PASSWORD_WITH_DESCRIPTION -> {
                    doAfterFetchAndDecrypt { decryptedSecret ->
                        val description = secretParser.extractDescription(resourceTypeEnum, decryptedSecret)
                        view?.addToClipboard(DESCRIPTION_LABEL, description)
                    }
                }
            }
        }
    }

    override fun searchAvatarClick() {
        view?.navigateToManageAccount()
    }

    private suspend fun doAfterFetchAndDecrypt(action: (ByteArray) -> Unit) {
        when (val output =
            runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                secretInteractor.fetchAndDecrypt(requireNotNull(currentMoreMenuResource?.resourceId))
            }
        ) {
            is SecretInteractor.Output.DecryptFailure -> view?.showDecryptionFailure()
            is SecretInteractor.Output.FetchFailure -> view?.showFetchFailure()
            is SecretInteractor.Output.Success -> {
                action(output.decryptedSecret)
            }
        }
    }

    override fun menuDeleteClick() {
        currentMoreMenuResource?.let { sadResource ->
            view?.hideResourceMoreMenu()
            runWhileShowingListProgress {
                when (val response = deleteResourceUseCase
                    .execute(DeleteResourceUseCase.Input(sadResource.resourceId))) {
                    is DeleteResourceUseCase.Output.Success -> {
                        resourceDeleted(sadResource.name)
                    }
                    is DeleteResourceUseCase.Output.Failure<*> -> {
                        Timber.e(response.response.exception)
                        view?.showGeneralError()
                    }
                }
            }
        }
    }

    override fun resourceDeleted(resourceName: String) {
        runWhileShowingListProgress {
            fetchResources()
        }
        view?.showResourceDeletedSnackbar(resourceName)
    }

    enum class SearchInputEndIconMode {
        AVATAR,
        CLEAR
    }

    companion object {
        @VisibleForTesting
        const val SECRET_LABEL = "Secret"
        const val DESCRIPTION_LABEL = "Description"

        private const val USERNAME_LABEL = "Username"
        private const val URL_LABEL = "Url"
    }
}
