package com.passbolt.mobile.android.feature.home.screen

import androidx.annotation.VisibleForTesting
import com.passbolt.mobile.android.common.extension.areListsEmpty
import com.passbolt.mobile.android.common.search.Searchable
import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.core.commonresource.ResourceTypeFactory
import com.passbolt.mobile.android.core.commonresource.usecase.DeleteResourceUseCase
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.database.usecase.GetLocalGroupsUseCase
import com.passbolt.mobile.android.database.usecase.GetLocalResourcesAndFoldersUseCase
import com.passbolt.mobile.android.database.usecase.GetLocalResourcesUseCase
import com.passbolt.mobile.android.database.usecase.GetLocalResourcesWithGroupUseCase
import com.passbolt.mobile.android.database.usecase.GetLocalResourcesWithTagUseCase
import com.passbolt.mobile.android.database.usecase.GetLocalSubFolderResourcesFilteredUseCase
import com.passbolt.mobile.android.database.usecase.GetLocalSubFoldersForFolderUseCase
import com.passbolt.mobile.android.database.usecase.GetLocalTagsUseCase
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.feature.home.screen.interactor.HomeDataInteractor
import com.passbolt.mobile.android.feature.home.screen.model.HomeDisplayView
import com.passbolt.mobile.android.feature.home.screen.model.SearchInputEndIconMode
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.parser.SecretParser
import com.passbolt.mobile.android.mappers.ResourceMenuModelMapper
import com.passbolt.mobile.android.storage.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.ui.Folder
import com.passbolt.mobile.android.ui.FolderWithCount
import com.passbolt.mobile.android.ui.GroupWithCount
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.TagWithCount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
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
    private val getLocalResourcesFiltered: GetLocalSubFolderResourcesFilteredUseCase,
    private val getLocalTagsUseCase: GetLocalTagsUseCase,
    private val getLocalResourcesWithTagUseCase: GetLocalResourcesWithTagUseCase,
    private val getLocalGroupsUseCase: GetLocalGroupsUseCase,
    private val getLocalResourcesWithGroupsUseCase: GetLocalResourcesWithGroupUseCase
) : BaseAuthenticatedPresenter<HomeContract.View>(coroutineLaunchContext), HomeContract.Presenter, KoinComponent {

    override var view: HomeContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private val dataRefreshJob = SupervisorJob()
    private val dataRefreshScope = CoroutineScope(dataRefreshJob + coroutineLaunchContext.ui)
    private val filteringJob = SupervisorJob()
    private val filteringScope = CoroutineScope(filteringJob + coroutineLaunchContext.ui)

    private lateinit var dataRefreshStatusFlow: Flow<DataRefreshStatus.Finished>
    private lateinit var homeView: HomeDisplayView
    private var currentSearchText = MutableStateFlow("")
    private var hasPreviousBackEntry = false

    private var resourceList: List<ResourceModel> = emptyList()
    private var foldersList: List<FolderWithCount> = emptyList()
    private var tagsList: List<TagWithCount> = emptyList()
    private var groupsList: List<GroupWithCount> = emptyList()

    private var filteredSubFolderResources: List<ResourceModel> = emptyList()
    private var filteredSubFolders: List<FolderWithCount> = emptyList()

    private var currentMoreMenuResource: ResourceModel? = null
    private var userAvatarUrl: String? = null
    private val searchInputEndIconMode
        get() = if (currentSearchText.value.isBlank()) SearchInputEndIconMode.AVATAR else SearchInputEndIconMode.CLEAR

    override fun viewCreate(fullDataRefreshStatusFlow: Flow<DataRefreshStatus.Finished>) {
        dataRefreshStatusFlow = fullDataRefreshStatusFlow
    }

    override fun argsRetrieved(homeDisplayView: HomeDisplayView?, hasPreviousEntry: Boolean) {
        homeView = homeDisplayView ?: HomeDisplayView.default()
        hasPreviousBackEntry = hasPreviousEntry

        view?.apply {
            hideAddButton()
            processScreenTitle(this)
            showProgress()
        }

        handleBackArrowVisibility()
        loadUserAvatar()
        collectDataRefreshStatus()
        collectFilteringRefreshes()
    }

    private fun processScreenTitle(view: HomeContract.View) {
        when (val currentHomeView = homeView) {
            is HomeDisplayView.Folders -> processFoldersTitle(currentHomeView, view)
            is HomeDisplayView.Tags -> processTagsTitle(currentHomeView, view)
            is HomeDisplayView.Groups -> processGroupsTitle(currentHomeView, view)
            else -> view.showHomeScreenTitle(currentHomeView)
        }
    }

    private fun processGroupsTitle(currentHomeView: HomeDisplayView.Groups, view: HomeContract.View) {
        if (currentHomeView.activeGroupId != null) {
            view.showGroupTitle(requireNotNull(currentHomeView.activeGroupName))
        } else {
            view.showHomeScreenTitle(currentHomeView)
        }
    }

    private fun processTagsTitle(currentHomeView: HomeDisplayView.Tags, view: HomeContract.View) {
        if (currentHomeView.activeTagId != null && currentHomeView.isActiveTagShared != null) {
            view.showTagTitle(
                requireNotNull(currentHomeView.activeTagName),
                requireNotNull(currentHomeView.isActiveTagShared)
            )
        } else {
            view.showHomeScreenTitle(currentHomeView)
        }
    }

    private fun processFoldersTitle(currentHomeView: HomeDisplayView.Folders, view: HomeContract.View) {
        when (currentHomeView.activeFolder) {
            is Folder.Child -> view.showChildFolderTitle(
                requireNotNull(currentHomeView.activeFolderName),
                requireNotNull(currentHomeView.isActiveFolderShared)
            )
            is Folder.Root -> view.showHomeScreenTitle(currentHomeView)
        }
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

    private fun collectFilteringRefreshes() {
        filteringScope.launch {
            currentSearchText
                .drop(1) // initial empty value
                .collectLatest {
                    Timber.d("New search text received")
                    processSearchIconChange()
                    filterHomeData()
                }
        }
    }

    // currently show add button in root folder only (and in all other views)
    private fun shouldShowAddButton() =
        if (homeView !is HomeDisplayView.Folders) {
            true
        } else {
            (homeView as HomeDisplayView.Folders).activeFolder is Folder.Root
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
        when (val currentHomeView = homeView) {
            is HomeDisplayView.Folders -> showResourcesAndFoldersFromDatabase(currentHomeView)
            is HomeDisplayView.Tags -> showTagsFromDatabase(currentHomeView)
            is HomeDisplayView.Groups -> showGroupsFromDatabase(currentHomeView)
            else -> showResourcesFromDatabase()
        }
    }

    override fun userAuthenticated() {
        initRefresh()
    }

    override fun detach() {
        dataRefreshScope.coroutineContext.cancelChildren()
        filteringScope.coroutineContext.cancelChildren()
        scope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }

    override fun searchClearClick() {
        view?.clearSearchInput()
    }

    override fun searchTextChange(text: String) {
        currentSearchText.value = text
    }

    private fun processSearchIconChange() {
        when (searchInputEndIconMode) {
            SearchInputEndIconMode.AVATAR -> view?.displaySearchAvatar(userAvatarUrl)
            SearchInputEndIconMode.CLEAR -> view?.displaySearchClearIcon()
        }
    }

    private suspend fun showResourcesFromDatabase() {
        resourceList = getLocalResourcesUseCase.execute(GetLocalResourcesUseCase.Input(homeView)).resources
        foldersList = emptyList()
        tagsList = emptyList()
        groupsList = emptyList()
        displayHomeData()
    }

    private suspend fun showTagsFromDatabase(tags: HomeDisplayView.Tags) {
        if (tags.activeTagId == null) { // tags root - list of tags
            resourceList = emptyList()
            tagsList = getLocalTagsUseCase.execute(Unit)
            foldersList = emptyList()
            groupsList = emptyList()
        } else { // resources with active tag
            tagsList = emptyList()
            foldersList = emptyList()
            groupsList = emptyList()
            resourceList = getLocalResourcesWithTagUseCase.execute(GetLocalResourcesWithTagUseCase.Input(tags))
                .resources
        }
        displayHomeData()
    }

    private suspend fun showGroupsFromDatabase(groups: HomeDisplayView.Groups) {
        if (groups.activeGroupId == null) { // groups root - list of groups
            resourceList = emptyList()
            tagsList = emptyList()
            foldersList = emptyList()
            groupsList = getLocalGroupsUseCase.execute(Unit)
        } else { // resources shared with group
            tagsList = emptyList()
            foldersList = emptyList()
            groupsList = emptyList()
            resourceList = getLocalResourcesWithGroupsUseCase.execute(
                GetLocalResourcesWithGroupUseCase.Input(groups)
            ).resources
        }
        displayHomeData()
    }

    private suspend fun showResourcesAndFoldersFromDatabase(folders: HomeDisplayView.Folders) {
        tagsList = emptyList()
        groupsList = emptyList()
        when (
            val result = getLocalResourcesAndFoldersUseCase.execute(
                GetLocalResourcesAndFoldersUseCase.Input(folders.activeFolder)
            )
        ) {
            is GetLocalResourcesAndFoldersUseCase.Output.Failure -> {
                Timber.d("Exception during getting resources and folders. Navigating to root")
                this.view?.navigateToRootHomeFromChildHome(HomeDisplayView.folderRoot())
            }
            is GetLocalResourcesAndFoldersUseCase.Output.Success -> {
                foldersList = result.folders
                resourceList = result.resources
            }
        }

        displayHomeData()
    }

    private fun displayHomeData() {
        if (areListsEmpty(resourceList, foldersList, tagsList, groupsList)) {
            view?.showEmptyList()
        } else {
            if (currentSearchText.value.isEmpty()) {
                view?.showItems(
                    resourceList,
                    foldersList,
                    tagsList,
                    groupsList,
                    filteredSubFolders,
                    filteredSubFolderResources,
                    HomeFragment.HeaderSectionConfiguration(
                        isInCurrentFolderSectionVisible = false, isInSubFoldersSectionVisible = false
                    )
                )
            } else {
                filteringScope.launch {
                    Timber.d("Applying existing search criteria")
                    processSearchIconChange()
                    filterHomeData()
                }
            }
        }
    }

    private suspend fun filterHomeData() {
        val filteredResources = filterSearchableList(resourceList, currentSearchText.value)
        val filteredFolders = filterSearchableList(foldersList, currentSearchText.value)
        val filteredTags = filterSearchableList(tagsList, currentSearchText.value)
        val filteredGroups = filterSearchableList(groupsList, currentSearchText.value)

        homeView.apply {
            if (this is HomeDisplayView.Folders) {
                populateSubFoldersFilteringResults(this)
            }
        }

        if (areListsEmpty(
                filteredResources,
                filteredFolders,
                filteredTags,
                filteredGroups,
                filteredSubFolders,
                filteredSubFolderResources
            )
        ) {
            view?.showSearchEmptyList()
        } else {
            view?.showItems(
                filteredResources,
                filteredFolders,
                filteredTags,
                filteredGroups,
                filteredSubFolders,
                filteredSubFolderResources,
                HomeFragment.HeaderSectionConfiguration(
                    isInCurrentFolderSectionVisible =
                    homeView is HomeDisplayView.Folders && !areListsEmpty(filteredResources, filteredFolders),
                    isInSubFoldersSectionVisible =
                    homeView is HomeDisplayView.Folders && !areListsEmpty(
                        filteredSubFolderResources,
                        filteredSubFolders
                    ),
                    (homeView as? HomeDisplayView.Folders)?.activeFolderName
                )
            )
        }
    }

    private suspend fun populateSubFoldersFilteringResults(folders: HomeDisplayView.Folders) {
        if (currentSearchText.value.isNotBlank()) {
            // resources need to be shown for all child folders
            val allSubFolders = getAllSubFolders(folders)
            // direct child folders are shown in top section; in filters show only child folders level>=1
            val subFoldersChildren = allSubFolders.filter { it.parentId != folders.activeFolder.folderId }

            filteredSubFolders = filterSearchableList(subFoldersChildren, currentSearchText.value)
            filteredSubFolderResources = getSubFoldersFilteredResources(allSubFolders)
        } else {
            filteredSubFolders = emptyList()
            filteredSubFolderResources = emptyList()
        }
    }

    private suspend fun getSubFoldersFilteredResources(allSubFolders: List<FolderWithCount>) =
        getLocalResourcesFiltered.execute(
            GetLocalSubFolderResourcesFilteredUseCase.Input(
                allSubFolders.map { it.folderId }, currentSearchText.value
            )
        ).resources

    private suspend fun getAllSubFolders(folders: HomeDisplayView.Folders) =
        getLocalSubFoldersForFolderUseCase.execute(
            GetLocalSubFoldersForFolderUseCase.Input(folders.activeFolder)
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
        view?.showFiltersMenu(homeView)
    }

    private fun navigateToHomeView(homeView: HomeDisplayView) {
        if (!hasPreviousBackEntry) {
            view?.navigateRootHomeFromRootHome(homeView)
        } else {
            view?.navigateToRootHomeFromChildHome(homeView)
        }
    }

    override fun allItemsClick() {
        navigateToHomeView(HomeDisplayView.AllItems)
    }

    override fun favouritesClick() {
        navigateToHomeView(HomeDisplayView.Favourites)
    }

    override fun recentlyModifiedClick() {
        navigateToHomeView(HomeDisplayView.RecentlyModified)
    }

    override fun sharedWithMeClick() {
        navigateToHomeView(HomeDisplayView.SharedWithMe)
    }

    override fun ownedByMeClick() {
        navigateToHomeView(HomeDisplayView.OwnedByMe)
    }

    override fun foldersClick() {
        navigateToHomeView(HomeDisplayView.folderRoot())
    }

    override fun tagsClick() {
        navigateToHomeView(HomeDisplayView.tagsRoot())
    }

    override fun groupsClick() {
        navigateToHomeView(HomeDisplayView.groupsRoot())
    }

    override fun folderItemClick(folderModel: FolderWithCount) {
        view?.navigateToChild(
            HomeDisplayView.Folders(
                Folder.Child(folderModel.folderId),
                folderModel.name,
                folderModel.isShared
            )
        )
    }

    override fun tagItemClick(tag: TagWithCount) {
        view?.navigateToChild(
            HomeDisplayView.Tags(
                tag.id,
                tag.slug,
                tag.isShared
            )
        )
    }

    override fun groupItemClick(group: GroupWithCount) {
        view?.navigateToChild(
            HomeDisplayView.Groups(
                group.groupId,
                group.groupName
            )
        )
    }

    override fun createResourceClick() {
        view?.navigateToCreateResource(
            when (val currentHomeView = homeView) {
                is HomeDisplayView.Folders -> currentHomeView.activeFolder.folderId
                else -> null
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
