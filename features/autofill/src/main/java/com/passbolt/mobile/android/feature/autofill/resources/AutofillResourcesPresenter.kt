package com.passbolt.mobile.android.feature.autofill.resources

import android.service.autofill.Dataset
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.core.commonresource.ResourceInteractor
import com.passbolt.mobile.android.core.commonresource.ResourceListUiModel
import com.passbolt.mobile.android.core.commonresource.ResourceTypeFactory
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.mvp.session.runAuthenticatedOperation
import com.passbolt.mobile.android.database.usecase.GetLocalResourcesUseCase
import com.passbolt.mobile.android.feature.autofill.StructureParser
import com.passbolt.mobile.android.feature.autofill.service.ParsedStructure
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
    private val structureParser: StructureParser,
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
    private lateinit var parsedStructure: Set<ParsedStructure>
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private var allItemsList: List<ResourceModel> = emptyList()
    private var currentSearchText: String = ""
    private val mode = AutofillMode.ACCESSIBILITY
    private var uri: String? = null
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

    override fun userAuthenticated() {
        userAvatarUrl = getSelectedAccountDataUseCase.execute(Unit).avatarUrl
            .also { view?.displaySearchAvatar(it) }
        fetchResources()
    }

    override fun refreshSwipe() {
        fetchResources()
    }

    override fun searchAvatarClick() {
        view?.navigateToManageAccount()
    }

    private fun fetchResources() {
        scope.launch {
            when (val result = runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                resourcesInteractor.fetchResourcesWithTypes()
            }) {
                is ResourceInteractor.Output.Failure -> fetchingResourcesFailure()
                is ResourceInteractor.Output.Success -> fetchingResourcesSuccess(result.resources)
            }
        }
    }

    private fun returnAccessibility(password: String, username: String) {
        view?.returnData(username, password, uri)
    }

    private fun returnAutofill(password: String, username: String) {
        val usernameParsedAssistStructure = structureParser.extractHint(View.AUTOFILL_HINT_USERNAME, parsedStructure)
        val passwordParsedAssistStructure = structureParser.extractHint(View.AUTOFILL_HINT_PASSWORD, parsedStructure)

        if (passwordParsedAssistStructure == null || usernameParsedAssistStructure == null) {
            view?.navigateBack()
            return
        }

        val dataSet = createDataSet(
            usernameParsedAssistStructure.id,
            passwordParsedAssistStructure.id,
            username,
            password
        )
        view?.returnData(dataSet)
    }

    private fun returnDataSet(password: String, username: String) {
        when (mode) {
            AutofillMode.ACCESSIBILITY -> returnAccessibility(password, username)
            AutofillMode.AUTOFILL -> returnAutofill(password, username)
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

    private fun getSuggested() =
        uri?.let { uri ->
            val domain = domainProvider.getHost(uri)
            val suggested = allItemsList.filter { domainProvider.getHost(it.url) == domain }
            if (suggested.isEmpty()) {
                fetchLinksApi(uri)
            } else {
                suggested
            }
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
                returnDataSet(password, resourceModel.username)
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

    override fun argsReceived(uri: String?) {
        uri?.let {
            this.uri = it
        }
    }

    private fun fetchLinksApi(domain: String): List<ResourceModel>? {
        return null
        // TODO
        /* return when (val links = checkUrlLinksUseCase.execute(CheckUrlLinksUseCase.Input(domain))) {
             CheckUrlLinksUseCase.Output.Failure -> {
                 null
             }
             is CheckUrlLinksUseCase.Output.Success -> {
                 null
             }
         }*/
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

    private fun createDataSet(
        usernameId: AutofillId,
        passwordId: AutofillId,
        usernameValue: String,
        password: String
    ) =
        Dataset.Builder()
            .setValue(
                usernameId,
                AutofillValue.forText(usernameValue)
            )
            .setValue(
                passwordId,
                AutofillValue.forText(password)
            )
            .build()

    enum class SearchInputEndIconMode {
        AVATAR,
        CLEAR
    }
}
