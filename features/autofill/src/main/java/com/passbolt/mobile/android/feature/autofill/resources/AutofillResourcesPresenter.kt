package com.passbolt.mobile.android.feature.autofill.resources

import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.core.commonresource.ResourceInteractor
import com.passbolt.mobile.android.core.commonresource.ResourceListUiModel
import com.passbolt.mobile.android.core.commonresource.ResourceTypeFactory
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.database.usecase.GetLocalResourcesUseCase
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.parser.SecretParser
import com.passbolt.mobile.android.storage.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.accounts.GetAccountsUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
class AutofillResourcesPresenter(
    coroutineLaunchContext: CoroutineLaunchContext,
    private val getLocalResourcesUse: GetLocalResourcesUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val fetchAndUpdateDatabaseUseCase: FetchAndUpdateDatabaseUseCase,
    private val domainProvider: DomainProvider,
    private val resourcesInteractor: ResourceInteractor,
    private val resourceSearch: SearchableMatcher,
    private val secretInteractor: SecretInteractor,
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val resourceTypeFactory: ResourceTypeFactory,
    private val secretParser: SecretParser,
    private val getAccountsUseCase: GetAccountsUseCase
) : BaseAuthenticatedPresenter<AutofillResourcesContract.View>(coroutineLaunchContext),
    AutofillResourcesContract.Presenter {

    override var view: AutofillResourcesContract.View? = null

    private var uri: String? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private var allItemsList: List<ResourceModel> = emptyList()
    private var currentSearchText: String = ""
    private var userAvatarUrl: String? = null
    private val searchInputEndIconMode
        get() = if (currentSearchText.isBlank()) SearchInputEndIconMode.AVATAR else SearchInputEndIconMode.CLEAR

    override fun attach(view: AutofillResourcesContract.View) {
        super<BaseAuthenticatedPresenter>.attach(view)
        if (getAccountsUseCase.execute(Unit).users.isNotEmpty()) {
            view.navigateToAuth()
        } else {
            view.navigateToSetup()
        }
    }

    override fun argsReceived(uri: String?) {
        this.uri = uri
    }

    override fun userAuthenticated() {
        userAvatarUrl = getSelectedAccountDataUseCase.execute(Unit).avatarUrl
            .also { view?.displaySearchAvatar(it) }
        fetchResources()
    }

    override fun refreshSwipe() {
        fetchResources(withShowingListProgress = false)
    }

    override fun searchAvatarClick() {
        view?.navigateToManageAccount()
    }

    private fun fetchResources(withShowingListProgress: Boolean = true, doAfterFetch: (() -> Unit)? = null) {
        scope.launch {
            view?.hideUpdateButton()
            if (withShowingListProgress) {
                view?.showProgress()
            }
            when (val result = runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                resourcesInteractor.fetchResourcesWithTypes()
            }) {
                is ResourceInteractor.Output.Failure -> fetchingResourcesFailure()
                is ResourceInteractor.Output.Success -> {
                    fetchingResourcesSuccess(result.resources)
                    doAfterFetch?.invoke()
                }
            }
        }
    }

    private suspend fun fetchingResourcesSuccess(list: List<ResourceModel>) {
        allItemsList = list
        updateLocalDatabase()
        if (list.isEmpty()) {
            view?.showEmptyList()
        } else {
            showItems()
        }
        view?.showUpdateButton()
    }

    private suspend fun fetchingResourcesFailure() {
        val accountId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        allItemsList = getLocalResourcesUse.execute(UserIdInput(accountId)).resources
        if (allItemsList.isNullOrEmpty()) {
            view?.showFullScreenError()
        } else {
            showItems()
            view?.showGeneralError()
        }
    }

    private fun showItems() {
        val itemsList = mutableListOf<ResourceListUiModel>()
        getSuggested()?.let {
            itemsList.add(ResourceListUiModel.Header("Suggested password"))
            itemsList.addAll(it.map { ResourceListUiModel.Data(it) })
            itemsList.add(ResourceListUiModel.Header("Other password"))
        }
        itemsList.addAll(allItemsList.map { ResourceListUiModel.Data(it) })
        view?.showResources(itemsList)
    }

    private fun getSuggested() = if (!uri.isNullOrBlank()) {
        uri?.let { uri ->
            val domain = domainProvider.getHost(uri)
            val suggested = allItemsList.filter {
                it.url?.let {
                    domainProvider.getHost(it) == domain
                } ?: false
            }

            suggested
        }
    } else {
        null
    }

    private suspend fun updateLocalDatabase() {
        fetchAndUpdateDatabaseUseCase.execute(FetchAndUpdateDatabaseUseCase.Input(allItemsList))
    }

    private fun showItemLoader(resourceId: String) {
        allItemsList.find { it.resourceId == resourceId }?.loaderVisible = true
        allItemsList.filter { it.resourceId != resourceId }.forEach { it.clickable = false }
        showItems()
    }

    private fun hideItemLoader(resourceId: String) {
        allItemsList.find { it.resourceId == resourceId }?.loaderVisible = false
        allItemsList.forEach { it.clickable = true }
        showItems()
    }

    override fun itemClick(resourceModel: ResourceModel) {
        showItemLoader(resourceModel.resourceId)
        scope.launch {
            val resourceTypeEnum = resourceTypeFactory.getResourceTypeEnum(resourceModel.resourceTypeId)
            doAfterFetchAndDecrypt(resourceModel.resourceId, {
                hideItemLoader(resourceModel.resourceId)
                val password = secretParser.extractPassword(resourceTypeEnum, it)
                view?.autofillReturn(resourceModel.username.orEmpty(), password, uri)
            }) {
                hideItemLoader(resourceModel.resourceId)
                view?.showGeneralError()
            }
        }
    }

    private suspend fun doAfterFetchAndDecrypt(
        resourceId: String,
        action: (ByteArray) -> Unit,
        errorAction: () -> Unit
    ) {
        when (val output =
            runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                secretInteractor.fetchAndDecrypt(resourceId)
            }
        ) {
            is SecretInteractor.Output.DecryptFailure -> errorAction.invoke()
            is SecretInteractor.Output.FetchFailure -> errorAction.invoke()
            is SecretInteractor.Output.Success -> {
                action(output.decryptedSecret)
            }
        }
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

    private fun filterList() {
        val filtered = allItemsList.filter {
            resourceSearch.matches(it, currentSearchText)
        }
        if (filtered.isEmpty()) {
            view?.showSearchEmptyList()
        } else {
            view?.showResources(filtered.map { ResourceListUiModel.Data(it) })
        }
    }

    override fun searchClearClick() {
        view?.clearSearchInput()
    }

    override fun closeClick() {
        view?.navigateToHome()
    }

    override fun newResourceCreated(newResourceId: String?) {
        view?.showResourceAddedSnackbar()
        view?.showProgress()
        fetchResources(withShowingListProgress = true) {
            newResourceId?.let { newResourceId ->
                allItemsList.indexOfFirst { it.resourceId == newResourceId }.let { index ->
                    view?.scrollResourcesToPosition(index)
                    itemClick(allItemsList[index])
                }
            }
        }
    }

    enum class SearchInputEndIconMode {
        AVATAR,
        CLEAR
    }
}
