package com.passbolt.mobile.android.feature.home.screen

import androidx.annotation.VisibleForTesting
import com.passbolt.mobile.android.common.extension.areListsEmpty
import com.passbolt.mobile.android.common.search.Searchable
import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.core.commonresource.ResourceTypeFactory
import com.passbolt.mobile.android.core.commonresource.usecase.DeleteResourceUseCase
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.database.usecase.GetLocalResourcesAndFoldersUseCase
import com.passbolt.mobile.android.database.usecase.GetLocalResourcesUseCase
import com.passbolt.mobile.android.database.usecase.GetLocalSubFolderResourcesFilteredUseCase
import com.passbolt.mobile.android.database.usecase.GetLocalSubFoldersForFolderUseCase
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.feature.home.screen.interactor.HomeDataInteractor
import com.passbolt.mobile.android.feature.home.screen.model.SearchInputEndIconMode
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.parser.SecretParser
import com.passbolt.mobile.android.mappers.ResourceMenuModelMapper
import com.passbolt.mobile.android.storage.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.ui.Folder
import com.passbolt.mobile.android.ui.FolderModel
import com.passbolt.mobile.android.ui.FolderModelWithChildrenCount
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourcesDisplayView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
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

/**
 * Presenter responsible for managing the home resource list. The general flow is to fetch resources and resource types
 * from the backend on start and update the database. Then when applying different views (all, favourite,
 * shared with me, etc.) the reload is done from the database only. To refresh from backend again users can do the
 * swipe to refresh gesture.
 */
@Suppress("TooManyFunctions") // TODO MOB-321
class HomePresenter(
    coroutineLaunchContext: CoroutineLaunchContext,
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val secretInteractor: SecretInteractor,
    private val searchableMatcher: SearchableMatcher,
    private val resourceTypeFactory: ResourceTypeFactory,
    private val secretParser: SecretParser,
    private val resourceMenuModelMapper: ResourceMenuModelMapper,
    private val deleteResourceUseCase: DeleteResourceUseCase,
    private val getLocalResourcesUseCase: GetLocalResourcesUseCase,
    private val getLocalSubFoldersForFolderUseCase: GetLocalSubFoldersForFolderUseCase,
    private val getLocalResourcesAndFoldersUseCase: GetLocalResourcesAndFoldersUseCase,
    private val getLocalResourcesFiltered: GetLocalSubFolderResourcesFilteredUseCase
) : BaseAuthenticatedPresenter<HomeContract.View>(coroutineLaunchContext), HomeContract.Presenter, KoinComponent {

    override var view: HomeContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private val dataRefreshJob = SupervisorJob()
    private val dataRefreshScope = CoroutineScope(dataRefreshJob + coroutineLaunchContext.ui)
    private lateinit var dataRefreshStatusFlow: Flow<DataRefreshStatus.Finished>

    private var currentSearchText: String = ""
    private lateinit var activeView: ResourcesDisplayView
    private lateinit var currentFolder: Folder
    private var currentFolderName: String? = null
    private var hasPreviousBackEntry = false

    private var resourceList: List<ResourceModel> = emptyList()
    private var foldersList: List<FolderModelWithChildrenCount> = emptyList()

    private var filteredSubFolderResources: List<ResourceModel> = emptyList()
    private var filteredSubFolders: List<FolderModelWithChildrenCount> = emptyList()

    private var currentMoreMenuResource: ResourceModel? = null
    private var userAvatarUrl: String? = null
    private val searchInputEndIconMode
        get() = if (currentSearchText.isBlank()) SearchInputEndIconMode.AVATAR else SearchInputEndIconMode.CLEAR

    override fun viewCreate(fullDataRefreshStatusFlow: Flow<DataRefreshStatus.Finished>) {
        dataRefreshStatusFlow = fullDataRefreshStatusFlow
    }

    override fun argsRetrieved(
        activeHomeView: ResourcesDisplayView,
        activeFolderId: String?,
        activeFolderName: String?,
        hasPreviousBackStackEntries: Boolean
    ) {
        activeView = activeHomeView
        currentFolderName = activeFolderName
        currentFolder = activeFolderId?.let { Folder.Child(it) } ?: Folder.Root
        hasPreviousBackEntry = hasPreviousBackStackEntries

        view?.apply {
            hideAddButton()
            if (!activeFolderName.isNullOrBlank()) {
                showChildFolderTitle(activeFolderName)
            } else {
                showHomeScreenTitle(activeView)
            }
            showProgress()
        }

        handleBackArrowVisibility()
        loadUserAvatar()
        collectDataRefreshStatus()
    }

    private fun collectDataRefreshStatus() {
        dataRefreshScope.launch {
            dataRefreshStatusFlow.collect {
                Timber.d("Received new home data")
                when (it.output) {
                    is HomeDataInteractor.Output.Failure -> view?.showError()
                    is HomeDataInteractor.Output.Success -> {
                        if (shouldShowAddButton()) {
                            view?.showAddButton()
                        }
                        showActiveHomeView()
                    }
                }
                view?.apply {
                    hideProgress()
                    hideRefreshProgress()
                }
            }
        }
    }

    // currently show add button in root folder only (and in all other views)
    private fun shouldShowAddButton() =
        if (activeView != ResourcesDisplayView.FOLDERS) {
            true
        } else {
            currentFolder is Folder.Root
        }

    private fun handleBackArrowVisibility() {
        if (hasPreviousBackEntry) {
            view?.showBackArrow()
        } else {
            view?.hideBackArrow()
        }
    }

    private fun loadUserAvatar() {
        userAvatarUrl = getSelectedAccountDataUseCase.execute(Unit).avatarUrl
            .also { view?.displaySearchAvatar(it) }
    }

    private suspend fun showActiveHomeView() {
        when (activeView) {
            ResourcesDisplayView.FOLDERS -> showResourcesAndFoldersFromDatabase()
            else -> showResourcesFromDatabase()
        }
    }

    override fun userAuthenticated() {
        initRefresh()
    }

    override fun detach() {
        dataRefreshScope.coroutineContext.cancelChildren()
        scope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }

    override fun searchClearClick() {
        view?.clearSearchInput()
    }

    override fun searchTextChange(text: String) {
        currentSearchText = text
        processSearchIconChange()
        filterHomeData()
    }

    private fun processSearchIconChange() {
        when (searchInputEndIconMode) {
            SearchInputEndIconMode.AVATAR -> view?.displaySearchAvatar(userAvatarUrl)
            SearchInputEndIconMode.CLEAR -> view?.displaySearchClearIcon()
        }
    }

    private suspend fun showResourcesFromDatabase() {
        resourceList = getLocalResourcesUseCase.execute(GetLocalResourcesUseCase.Input(activeView)).resources
        foldersList = emptyList()
        displayHomeData()
    }

    private suspend fun showResourcesAndFoldersFromDatabase() {
        when (
            val result = getLocalResourcesAndFoldersUseCase.execute(
                GetLocalResourcesAndFoldersUseCase.Input(currentFolder)
            )
        ) {
            is GetLocalResourcesAndFoldersUseCase.Output.Failure -> {
                Timber.d("Exception during getting resources and folders. Navigating to root")
                view?.navigateToRootHomeFromChildHome(ResourcesDisplayView.FOLDERS)
            }
            is GetLocalResourcesAndFoldersUseCase.Output.Success -> {
                foldersList = result.folders
                resourceList = result.resources
            }
        }

        displayHomeData()
    }

    private fun displayHomeData() {
        if (resourceList.isEmpty() && foldersList.isEmpty()) {
            view?.showEmptyList()
        } else {
            if (currentSearchText.isEmpty()) {
                view?.showItems(
                    resourceList,
                    foldersList,
                    filteredSubFolders,
                    filteredSubFolderResources,
                    HomeFragment.HeaderSectionConfiguration(
                        isInCurrentFolderSectionVisible = false, isInSubFoldersSectionVisible = false
                    )
                )
            } else {
                filterHomeData()
            }
        }
    }

    private fun filterHomeData() {
        scope.launch {
            val filteredResources = filterSearchableList(resourceList, currentSearchText)
            val filteredFolders = filterSearchableList(foldersList, currentSearchText)

            if (activeView == ResourcesDisplayView.FOLDERS) {
                populateSubFoldersFilteringResults()
            }
            if (areListsEmpty(filteredResources, filteredFolders, filteredSubFolders, filteredSubFolderResources)
            ) {
                view?.showSearchEmptyList()
            } else {
                view?.showItems(
                    filteredResources,
                    filteredFolders,
                    filteredSubFolders,
                    filteredSubFolderResources,
                    HomeFragment.HeaderSectionConfiguration(
                        isInCurrentFolderSectionVisible = !areListsEmpty(filteredResources, filteredFolders),
                        isInSubFoldersSectionVisible = !areListsEmpty(filteredSubFolderResources, filteredSubFolders),
                        currentFolderName
                    )
                )
            }
        }
    }

    private suspend fun populateSubFoldersFilteringResults() {
        if (currentSearchText.isNotBlank()) {
            filteredSubFolders = getAllSubFolders()
            filteredSubFolderResources = getSubFoldersResources()
        } else {
            filteredSubFolders = emptyList()
            filteredSubFolderResources = emptyList()
        }
    }

    private suspend fun getSubFoldersResources() = getLocalResourcesFiltered.execute(
        GetLocalSubFolderResourcesFilteredUseCase.Input(
            filteredSubFolders.map { it.folderModel.folderId }, currentSearchText
        )
    ).resources

    private suspend fun getAllSubFolders() = getLocalSubFoldersForFolderUseCase.execute(
        GetLocalSubFoldersForFolderUseCase.Input(currentFolder, currentSearchText)
    ).folders

    private fun <T : Searchable> filterSearchableList(list: List<T>, currentSearchText: String) =
        list.filter {
            searchableMatcher.matches(it, currentSearchText)
        }

    override fun refreshClick() {
        initRefresh()
    }

    override fun refreshSwipe() {
        view?.hideAddButton()
        view?.performRefreshUsingRefreshExecutor()
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
        view?.navigateToSwitchAccount()
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
        view?.showDeleteConfirmationDialog()
    }

    override fun deleteResourceConfirmed() {
        view?.showProgress()
        scope.launch {
            currentMoreMenuResource?.let { sadResource ->
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
        initRefresh()
        view?.showResourceDeletedSnackbar(resourceName)
    }

    override fun resourceEdited(resourceName: String) {
        initRefresh()
        view?.showResourceEditedSnackbar(resourceName)
    }

    override fun newResourceCreated() {
        initRefresh()
        view?.showResourceAddedSnackbar()
    }

    private fun initRefresh() {
        view?.apply {
            showProgress()
            performRefreshUsingRefreshExecutor()
        }
    }

    override fun menuEditClick() {
        view?.navigateToEdit(requireNotNull(currentMoreMenuResource))
    }

    override fun switchAccountManageAccountClick() {
        view?.navigateToManageAccounts()
    }

    override fun filtersClick() {
        view?.showFiltersMenu(activeView)
    }

    private fun navigateToHomeView(activeView: ResourcesDisplayView) {
        if (!hasPreviousBackEntry) {
            view?.navigateRootHomeFromRootHome(activeView)
        } else {
            view?.navigateToRootHomeFromChildHome(activeView)
        }
    }

    override fun allItemsClick() {
        navigateToHomeView(ResourcesDisplayView.ALL)
    }

    override fun favouritesClick() {
        navigateToHomeView(ResourcesDisplayView.FAVOURITES)
    }

    override fun recentlyModifiedClick() {
        navigateToHomeView(ResourcesDisplayView.RECENTLY_MODIFIED)
    }

    override fun sharedWithMeClick() {
        navigateToHomeView(ResourcesDisplayView.SHARED_WITH_ME)
    }

    override fun ownedByMeClick() {
        navigateToHomeView(ResourcesDisplayView.OWNED_BY_ME)
    }

    override fun foldersClick() {
        navigateToHomeView(ResourcesDisplayView.FOLDERS)
    }

    override fun folderItemClick(folderModel: FolderModel) {
        view?.navigateToChildFolder(folderModel.folderId, folderModel.name, activeView)
        view?.showBackArrow()
    }

    override fun createResourceClick() {
        view?.navigateToCreateResource(
            when (val folder = currentFolder) {
                is Folder.Child -> folder.folderId
                is Folder.Root -> null
            }
        )
    }

    companion object {
        @VisibleForTesting
        const val SECRET_LABEL = "Secret"
        const val DESCRIPTION_LABEL = "Description"

        private const val USERNAME_LABEL = "Username"
        private const val URL_LABEL = "Url"
    }
}
