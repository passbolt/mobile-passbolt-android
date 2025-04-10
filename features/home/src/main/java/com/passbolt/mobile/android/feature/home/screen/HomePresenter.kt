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

package com.passbolt.mobile.android.feature.home.screen

import com.passbolt.mobile.android.common.extension.areListsEmpty
import com.passbolt.mobile.android.common.search.Searchable
import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.core.autofill.urlmatcher.AutofillUrlMatcher
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderDetailsUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalResourcesAndFoldersUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalSubFolderResourcesFilteredUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalSubFoldersForFolderUseCase
import com.passbolt.mobile.android.core.commongroups.usecase.db.GetLocalGroupsWithShareItemsCountUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.base.DataRefreshViewReactivePresenter
import com.passbolt.mobile.android.core.idlingresource.DeleteResourceIdlingResource
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider
import com.passbolt.mobile.android.core.preferences.usecase.GetHomeDisplayViewPrefsUseCase
import com.passbolt.mobile.android.core.rbac.usecase.GetRbacRulesUseCase
import com.passbolt.mobile.android.core.resources.actions.ResourceCommonActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourcePropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.performCommonResourceAction
import com.passbolt.mobile.android.core.resources.actions.performResourcePropertyAction
import com.passbolt.mobile.android.core.resources.actions.performSecretPropertyAction
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcesFilteredByTagUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcesUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcesWithGroupUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcesWithTagUseCase
import com.passbolt.mobile.android.core.tags.usecase.db.GetLocalTagsUseCase
import com.passbolt.mobile.android.feature.home.screen.model.HeaderSectionConfiguration
import com.passbolt.mobile.android.feature.home.screen.model.SearchInputEndIconMode
import com.passbolt.mobile.android.mappers.HomeDisplayViewMapper
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.homeSlugs
import com.passbolt.mobile.android.ui.Folder
import com.passbolt.mobile.android.ui.FolderMoreMenuModel
import com.passbolt.mobile.android.ui.FolderWithCountAndPath
import com.passbolt.mobile.android.ui.GroupWithCount
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import com.passbolt.mobile.android.ui.RbacRuleModel.ALLOW
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.TagWithCount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import timber.log.Timber

@Suppress("TooManyFunctions", "LargeClass", "LongParameterList") // TODO MOB-321
class HomePresenter(
    coroutineLaunchContext: CoroutineLaunchContext,
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val searchableMatcher: SearchableMatcher,
    private val getLocalResourcesUseCase: GetLocalResourcesUseCase,
    private val getLocalResourcesFilteredByTag: GetLocalResourcesFilteredByTagUseCase,
    private val getLocalSubFoldersForFolderUseCase: GetLocalSubFoldersForFolderUseCase,
    private val getLocalResourcesAndFoldersUseCase: GetLocalResourcesAndFoldersUseCase,
    private val getLocalResourcesFiltered: GetLocalSubFolderResourcesFilteredUseCase,
    private val getLocalTagsUseCase: GetLocalTagsUseCase,
    private val getLocalResourcesWithTagUseCase: GetLocalResourcesWithTagUseCase,
    private val getLocalGroupsWithShareItemsCountUseCase: GetLocalGroupsWithShareItemsCountUseCase,
    private val getLocalResourcesWithGroupsUseCase: GetLocalResourcesWithGroupUseCase,
    private val getHomeDisplayViewPrefsUseCase: GetHomeDisplayViewPrefsUseCase,
    private val homeModelMapper: HomeDisplayViewMapper,
    private val autofillMatcher: AutofillUrlMatcher,
    private val getLocalFolderUseCase: GetLocalFolderDetailsUseCase,
    private val deleteResourceIdlingResource: DeleteResourceIdlingResource,
    private val totpParametersProvider: TotpParametersProvider,
    private val getRbacRulesUseCase: GetRbacRulesUseCase
) : DataRefreshViewReactivePresenter<HomeContract.View>(coroutineLaunchContext), HomeContract.Presenter,
    KoinComponent {

    override var view: HomeContract.View? = null
    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(job + coroutineLaunchContext.ui)
    private val filteringJob = SupervisorJob()
    private val filteringScope = CoroutineScope(filteringJob + coroutineLaunchContext.ui)
    private lateinit var homeView: HomeDisplayViewModel

    private var currentSearchText = MutableStateFlow("")
    private var hasPreviousBackEntry = false
    private lateinit var showSuggestedModel: ShowSuggestedModel
    private var suggestedResourceList: List<ResourceModel> = emptyList()

    private var resourceList: List<ResourceModel> = emptyList()
    private var foldersList: List<FolderWithCountAndPath> = emptyList()
    private var tagsList: List<TagWithCount> = emptyList()
    private var groupsList: List<GroupWithCount> = emptyList()
    private var filteredSubFolderResources: List<ResourceModel> = emptyList()

    private var filteredSubFolders: List<FolderWithCountAndPath> = emptyList()
    private var currentMoreMenuResource: ResourceModel? = null

    private var userAvatarUrl: String? = null
    private val searchInputEndIconMode
        get() = if (currentSearchText.value.isBlank()) SearchInputEndIconMode.AVATAR else SearchInputEndIconMode.CLEAR

    private val resourcePropertiesActionsInteractor: ResourcePropertiesActionsInteractor
        get() = get { parametersOf(requireNotNull(currentMoreMenuResource)) }
    private val secretPropertiesActionsInteractor: SecretPropertiesActionsInteractor
        get() = get {
            parametersOf(requireNotNull(currentMoreMenuResource), needSessionRefreshFlow, sessionRefreshedFlow)
        }
    private val resourceCommonActionsInteractor: ResourceCommonActionsInteractor
        get() = get {
            parametersOf(requireNotNull(currentMoreMenuResource), needSessionRefreshFlow, sessionRefreshedFlow)
        }

    private var refreshInProgress: Boolean = true

    override fun argsRetrieved(
        showSuggestedModel: ShowSuggestedModel,
        homeDisplayView: HomeDisplayViewModel?,
        hasPreviousEntry: Boolean,
        shouldShowCloseButton: Boolean,
        shouldShowResourceMoreMenu: Boolean
    ) {
        val filterPreferences = getHomeDisplayViewPrefsUseCase.execute(Unit)
        this.homeView = homeDisplayView ?: homeModelMapper.map(
            filterPreferences.userSetHomeView,
            filterPreferences.lastUsedHomeView
        )
        this.showSuggestedModel = showSuggestedModel
        this.hasPreviousBackEntry = hasPreviousEntry

        view?.let {
            processSearchHint(it)
        }

        showActiveHomeView()

        handleCloseVisibility(shouldShowCloseButton)
        handleBackArrowVisibility()
        handleMoreMenuIconVisibility(shouldShowResourceMoreMenu)
        loadUserAvatar()

        collectFilteringRefreshes()
    }

    private fun handleMoreMenuIconVisibility(shouldShowResourceMoreMenu: Boolean) {
        if (shouldShowFolderMoreMenu(shouldShowResourceMoreMenu)) {
            view?.showFolderMoreMenuIcon()
        } else {
            view?.hideFolderMoreMenuIcon()
        }
    }

    private fun handleCloseVisibility(shouldShowCloseButton: Boolean) {
        if (shouldShowCloseButton) {
            view?.showCloseButton()
        }
    }

    private fun processSearchHint(view: HomeContract.View) {
        when (homeView) {
            is HomeDisplayViewModel.AllItems -> view.showAllItemsSearchHint()
            else -> view.showDefaultSearchHint()
        }
    }

    private fun processScreenTitle(view: HomeContract.View) {
        when (val currentHomeView = homeView) {
            is HomeDisplayViewModel.Folders -> processFoldersTitle(currentHomeView, view)
            is HomeDisplayViewModel.Tags -> processTagsTitle(currentHomeView, view)
            is HomeDisplayViewModel.Groups -> processGroupsTitle(currentHomeView, view)
            else -> view.showHomeScreenTitle(currentHomeView)
        }
    }

    private fun processGroupsTitle(currentHomeView: HomeDisplayViewModel.Groups, view: HomeContract.View) {
        if (currentHomeView.activeGroupId != null) {
            view.showGroupTitle(requireNotNull(currentHomeView.activeGroupName))
        } else {
            view.showHomeScreenTitle(currentHomeView)
        }
    }

    private fun processTagsTitle(currentHomeView: HomeDisplayViewModel.Tags, view: HomeContract.View) {
        if (currentHomeView.activeTagId != null && currentHomeView.isActiveTagShared != null) {
            view.showTagTitle(
                requireNotNull(currentHomeView.activeTagName),
                requireNotNull(currentHomeView.isActiveTagShared)
            )
        } else {
            view.showHomeScreenTitle(currentHomeView)
        }
    }

    private fun processFoldersTitle(currentHomeView: HomeDisplayViewModel.Folders, view: HomeContract.View) {
        when (currentHomeView.activeFolder) {
            is Folder.Child -> view.showChildFolderTitle(
                requireNotNull(currentHomeView.activeFolderName),
                requireNotNull(currentHomeView.isActiveFolderShared)
            )
            is Folder.Root -> view.showHomeScreenTitle(currentHomeView)
        }
    }

    override fun refreshStartAction() {
        view?.hideCreateButton()
    }

    override fun refreshSuccessAction() {
        coroutineScope.launch {
            if (shouldShowCreateButton()) {
                view?.showCreateButton()
            }
            refreshInProgress = false
            showActiveHomeView()
        }
    }

    override fun refreshFailureAction() {
        view?.showDataRefreshError()
    }

    private fun collectFilteringRefreshes() {
        filteringScope.launch {
            currentSearchText
                .drop(1) // initial empty value
                .collectLatest {
                    Timber.d("New search text received")
                    processSearchIconChange()
                    runWithHandlingMissingItem({
                        filterHomeData()
                    }, resultIfActionFails = Unit)
                }
        }
    }

    private suspend fun shouldShowCreateButton(): Boolean {
        homeView.let {
            // currently do not show add button on tags and groups
            if (it is HomeDisplayViewModel.Tags || it is HomeDisplayViewModel.Groups) {
                return false
            }
            // show only in folder with update permission
            if (it is HomeDisplayViewModel.Folders) {
                return when (val currentFolder = it.activeFolder) {
                    is Folder.Child ->
                        runWithHandlingMissingItem({
                            getLocalFolderUseCase.execute(GetLocalFolderDetailsUseCase.Input(currentFolder.folderId))
                                .folder.permission in setOf(ResourcePermission.OWNER, ResourcePermission.UPDATE)
                        }, resultIfActionFails = false)

                    is Folder.Root -> true
                }
            }
        }
        return true
    }

    private suspend fun <T> runWithHandlingMissingItem(action: suspend () -> T, resultIfActionFails: T): T {
        return try {
            action()
        } catch (exception: NullPointerException) {
            // the current filtering item (tag, folder, group)
            // was deleted from other application instance and full refresh was done while being on that item
            // in that case navigate to selected filter root and show info message
            navigateToHomeView(
                when (homeView) {
                    is HomeDisplayViewModel.AllItems -> HomeDisplayViewModel.AllItems
                    is HomeDisplayViewModel.Favourites -> HomeDisplayViewModel.Favourites
                    is HomeDisplayViewModel.Folders -> HomeDisplayViewModel.folderRoot()
                    is HomeDisplayViewModel.Groups -> HomeDisplayViewModel.groupsRoot()
                    is HomeDisplayViewModel.OwnedByMe -> HomeDisplayViewModel.OwnedByMe
                    is HomeDisplayViewModel.RecentlyModified -> HomeDisplayViewModel.RecentlyModified
                    is HomeDisplayViewModel.SharedWithMe -> HomeDisplayViewModel.SharedWithMe
                    is HomeDisplayViewModel.Tags -> HomeDisplayViewModel.tagsRoot()
                    is HomeDisplayViewModel.Expiry -> HomeDisplayViewModel.Expiry
                }
            )
            view?.showContentNotAvailable()
            resultIfActionFails
        }
    }

    // show in child folders only
    private fun shouldShowFolderMoreMenu(shouldShowResourceMoreMenu: Boolean) =
        shouldShowResourceMoreMenu && homeView.let {
            it is HomeDisplayViewModel.Folders && it.activeFolder is Folder.Child
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

    private fun showActiveHomeView() {
        coroutineScope.launch {
            view?.let { processScreenTitle(it) }
            runWithHandlingMissingItem({
                suggestedResourceList = if (shouldShowSuggested()) {
                    getLocalResourcesUseCase.execute(GetLocalResourcesUseCase.Input(homeSlugs))
                        .resources
                        .filter {
                            val autofillUrl = (showSuggestedModel as? ShowSuggestedModel.Show)?.suggestedUri
                            autofillMatcher.isMatching(autofillUrl, it)
                        }
                } else {
                    emptyList()
                }
                when (
                    val currentHomeView = homeView) {
                    is HomeDisplayViewModel.Folders -> showResourcesAndFoldersFromDatabase(currentHomeView)
                    is HomeDisplayViewModel.Tags -> showTagsFromDatabase(currentHomeView)
                    is HomeDisplayViewModel.Groups -> showGroupsFromDatabase(currentHomeView)
                    else -> showResourcesFromDatabase()
                }
            }, resultIfActionFails = Unit)
        }
    }

    private fun shouldShowSuggested() = when (val activeHomeView = homeView) {
        is HomeDisplayViewModel.AllItems -> true
        is HomeDisplayViewModel.Favourites -> true
        is HomeDisplayViewModel.Folders -> when (activeHomeView.activeFolder) {
            is Folder.Child -> false
            is Folder.Root -> true
        }
        is HomeDisplayViewModel.Groups -> activeHomeView.activeGroupId == null // groups root
        is HomeDisplayViewModel.OwnedByMe -> true
        is HomeDisplayViewModel.RecentlyModified -> true
        is HomeDisplayViewModel.SharedWithMe -> true
        is HomeDisplayViewModel.Tags -> activeHomeView.activeTagId == null // tags root
        is HomeDisplayViewModel.Expiry -> true
    }

    override fun userAuthenticated() {
        initRefresh()
    }

    override fun detach() {
        filteringScope.coroutineContext.cancelChildren()
        coroutineScope.coroutineContext.cancelChildren()
        super<DataRefreshViewReactivePresenter>.detach()
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
        resourceList = getLocalResourcesUseCase.execute(
            GetLocalResourcesUseCase.Input(
                homeSlugs,
                homeView
            )
        ).resources
        foldersList = emptyList()
        tagsList = emptyList()
        groupsList = emptyList()
        displayHomeData()
    }

    private suspend fun showTagsFromDatabase(tags: HomeDisplayViewModel.Tags) {
        val tagsRbac = getRbacRulesUseCase.execute(Unit).rbacModel.tagsUseRule
        if (tagsRbac == ALLOW) {
            if (tags.activeTagId == null) { // tags root - list of tags
                resourceList = emptyList()
                tagsList = getLocalTagsUseCase.execute(Unit)
                foldersList = emptyList()
                groupsList = emptyList()
            } else { // resources with active tag
                tagsList = emptyList()
                foldersList = emptyList()
                groupsList = emptyList()
                resourceList = getLocalResourcesWithTagUseCase.execute(
                    GetLocalResourcesWithTagUseCase.Input(
                        tags,
                        homeSlugs
                    )
                ).resources
            }
            displayHomeData()
        } else {
            // if tags were disabled by rbac after selecting fallback to all items
            homeView = HomeDisplayViewModel.AllItems
            showActiveHomeView()
        }
    }

    private suspend fun showGroupsFromDatabase(groups: HomeDisplayViewModel.Groups) {
        if (groups.activeGroupId == null) { // groups root - list of groups
            resourceList = emptyList()
            tagsList = emptyList()
            foldersList = emptyList()
            groupsList = getLocalGroupsWithShareItemsCountUseCase.execute(Unit)
        } else { // resources shared with group
            tagsList = emptyList()
            foldersList = emptyList()
            groupsList = emptyList()
            resourceList = getLocalResourcesWithGroupsUseCase.execute(
                GetLocalResourcesWithGroupUseCase.Input(
                    groups,
                    homeSlugs
                )
            ).resources
        }
        displayHomeData()
    }

    private suspend fun showResourcesAndFoldersFromDatabase(folders: HomeDisplayViewModel.Folders) {
        val foldersRbac = getRbacRulesUseCase.execute(Unit).rbacModel.foldersUseRule
        if (foldersRbac == ALLOW) {
            tagsList = emptyList()
            groupsList = emptyList()
            when (
                val result = getLocalResourcesAndFoldersUseCase.execute(
                    GetLocalResourcesAndFoldersUseCase.Input(
                        folders.activeFolder,
                        homeSlugs
                    )
                )
            ) {
                is GetLocalResourcesAndFoldersUseCase.Output.Failure -> {
                    Timber.d("Exception during getting resources and folders. Navigating to root")
                    this.view?.navigateToRootHomeFromChildHome(HomeDisplayViewModel.folderRoot())
                }
                is GetLocalResourcesAndFoldersUseCase.Output.Success -> {
                    foldersList = result.folders
                    resourceList = result.resources
                }
            }

            displayHomeData()
        } else {
            // if folders were disabled by rbac after selecting fallback to all items
            homeView = HomeDisplayViewModel.AllItems
            showActiveHomeView()
        }
    }

    private fun displayHomeData() {
        if (areListsEmpty(resourceList, foldersList, tagsList, groupsList, suggestedResourceList)) {
            view?.showEmptyList()
        } else {
            if (currentSearchText.value.isEmpty()) {
                view?.showItems(
                    suggestedResourceList,
                    resourceList,
                    foldersList,
                    tagsList,
                    groupsList,
                    filteredSubFolders,
                    filteredSubFolderResources,
                    HeaderSectionConfiguration(
                        isInCurrentFolderSectionVisible = false,
                        isInSubFoldersSectionVisible = false,
                        isOtherItemsSectionVisible = !areListsEmpty(
                            resourceList,
                            foldersList,
                            tagsList,
                            groupsList,
                            filteredSubFolders,
                            filteredSubFolderResources
                        ) && showSuggestedModel is ShowSuggestedModel.Show,
                        isSuggestedSectionVisible = suggestedResourceList.isNotEmpty()
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
        var filteredResources = filterSearchableList(resourceList, currentSearchText.value)
        // filtered resources + additionally append resources that have tag that matches filter
        if (homeView is HomeDisplayViewModel.AllItems) {
            filteredResources = (filteredResources + getResourcesFilteredByTag())
                .distinctBy { it.resourceId }
        }
        val filteredFolders = filterSearchableList(foldersList, currentSearchText.value)
        val filteredTags = filterSearchableList(tagsList, currentSearchText.value)
        val filteredGroups = filterSearchableList(groupsList, currentSearchText.value)

        homeView.apply {
            if (this is HomeDisplayViewModel.Folders) {
                populateSubFoldersFilteringResults(this)
            }
        }

        if (areListsEmpty(
                filteredResources, filteredFolders, filteredTags, filteredGroups,
                filteredSubFolders, filteredSubFolderResources
            )
        ) {
            view?.showSearchEmptyList()
        } else {
            view?.showItems(
                suggestedResources = emptyList(),
                filteredResources,
                filteredFolders,
                filteredTags,
                filteredGroups,
                filteredSubFolders,
                filteredSubFolderResources,
                HeaderSectionConfiguration(
                    isInCurrentFolderSectionVisible =
                        homeView is HomeDisplayViewModel.Folders && !areListsEmpty(filteredResources, filteredFolders),
                    isInSubFoldersSectionVisible =
                        homeView is HomeDisplayViewModel.Folders && !areListsEmpty(
                            filteredSubFolderResources,
                            filteredSubFolders
                        ),
                    (homeView as? HomeDisplayViewModel.Folders)?.activeFolderName,
                    isSuggestedSectionVisible = false,
                    isOtherItemsSectionVisible = false
                )
            )
        }
    }

    private suspend fun getResourcesFilteredByTag() = getLocalResourcesFilteredByTag.execute(
        GetLocalResourcesFilteredByTagUseCase.Input(
            currentSearchText.value,
            homeSlugs
        )
    ).resources

    private suspend fun populateSubFoldersFilteringResults(folders: HomeDisplayViewModel.Folders) {
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

    private suspend fun getSubFoldersFilteredResources(allSubFolders: List<FolderWithCountAndPath>) =
        getLocalResourcesFiltered.execute(
            GetLocalSubFolderResourcesFilteredUseCase.Input(
                allSubFolders.map { it.folderId },
                currentSearchText.value,
                homeSlugs
            )
        ).resources

    private suspend fun getAllSubFolders(folders: HomeDisplayViewModel.Folders) =
        getLocalSubFoldersForFolderUseCase.execute(
            GetLocalSubFoldersForFolderUseCase.Input(folders.activeFolder)
        ).folders

    private fun <T : Searchable> filterSearchableList(list: List<T>, currentSearchText: String) =
        list.filter {
            searchableMatcher.matches(it, currentSearchText)
        }

    override fun refreshSwipe() {
        refreshInProgress = true
        view?.apply {
            hideCreateButton()
            fullDataRefreshExecutor.performFullDataRefresh()
        }
    }

    override fun resourceMoreClick(resourceModel: ResourceModel) {
        currentMoreMenuResource = resourceModel
        view?.navigateToMore(resourceModel.resourceId, resourceModel.metadataJsonModel.name)
    }

    override fun itemClick(resourceModel: ResourceModel) {
        view?.navigateToDetails(resourceModel)
    }

    override fun menuCopyUsernameClick() {
        coroutineScope.launch {
            performResourcePropertyAction(
                action = { resourcePropertiesActionsInteractor.provideUsername() },
                doOnResult = { view?.addToClipboard(it.label, it.result, it.isSecret) }
            )
        }
    }

    override fun menuLaunchWebsiteClick() {
        coroutineScope.launch {
            performResourcePropertyAction(
                action = { resourcePropertiesActionsInteractor.provideWebsiteUrl() },
                doOnResult = { view?.openWebsite(it.result) }
            )
        }
    }

    override fun menuCopyUrlClick() {
        coroutineScope.launch {
            performResourcePropertyAction(
                action = { resourcePropertiesActionsInteractor.provideWebsiteUrl() },
                doOnResult = { view?.addToClipboard(it.label, it.result, it.isSecret) }
            )
        }
    }

    override fun menuCopyPasswordClick() {
        coroutineScope.launch {
            performSecretPropertyAction(
                action = { secretPropertiesActionsInteractor.providePassword() },
                doOnDecryptionFailure = { view?.showDecryptionFailure() },
                doOnFetchFailure = { view?.showFetchFailure() },
                doOnSuccess = { view?.addToClipboard(it.label, it.result.orEmpty(), it.isSecret) }
            )
        }
    }

    override fun menuCopyMetadataDescriptionClick() {
        coroutineScope.launch {
            performResourcePropertyAction(
                action = { resourcePropertiesActionsInteractor.provideDescription() },
                doOnResult = { view?.addToClipboard(it.label, it.result, it.isSecret) }
            )
        }
    }

    override fun menuCopyNoteClick() {
        coroutineScope.launch {
            performSecretPropertyAction(
                action = { secretPropertiesActionsInteractor.provideNote() },
                doOnDecryptionFailure = { view?.showDecryptionFailure() },
                doOnFetchFailure = { view?.showFetchFailure() },
                doOnSuccess = { view?.addToClipboard(it.label, it.result, it.isSecret) }
            )
        }
    }

    override fun searchAvatarClick() {
        if (refreshInProgress) {
            view?.showPleaseWaitForDataRefresh()
        } else {
            view?.navigateToSwitchAccount()
        }
    }

    override fun menuDeleteClick() {
        view?.showDeleteConfirmationDialog()
    }

    override fun deleteResourceConfirmed() {
        coroutineScope.launch {
            deleteResourceIdlingResource.setIdle(false)
            performCommonResourceAction(
                action = { resourceCommonActionsInteractor.deleteResource() },
                doOnFailure = { view?.showDeleteResourceFailure() },
                doOnSuccess = { resourceDeleted(it.resourceName) }
            )
            deleteResourceIdlingResource.setIdle(true)
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

    override fun resourceShared(isShared: Boolean) {
        if (isShared) {
            initRefresh()
            view?.showResourceSharedSnackbar()
        }
    }

    override fun newResourceCreated(resourceId: String?) {
        resourceId?.let {
            initRefresh()
            view?.apply {
                showResourceCreatedSnackbar()
                resourcePostCreateAction(resourceId)
            }
        }
    }

    private fun initRefresh() {
        refreshInProgress = true
        fullDataRefreshExecutor.performFullDataRefresh()
    }

    override fun menuEditClick() {
        view?.navigateToEdit(requireNotNull(currentMoreMenuResource))
    }

    override fun switchAccountManageAccountClick() {
        view?.navigateToManageAccounts()
    }

    override fun switchAccountClick() {
        view?.navigateToSwitchedAccountAuth()
    }

    override fun filtersClick() {
        view?.showFiltersMenu(homeView)
    }

    private fun navigateToHomeView(homeView: HomeDisplayViewModel) {
        if (!hasPreviousBackEntry) {
            view?.navigateRootHomeFromRootHome(homeView)
        } else {
            view?.navigateToRootHomeFromChildHome(homeView)
        }
    }

    override fun menuShareClick() {
        view?.navigateToShare(
            requireNotNull(currentMoreMenuResource)
        )
    }

    override fun allItemsClick() {
        navigateToHomeView(HomeDisplayViewModel.AllItems)
    }

    override fun favouritesClick() {
        navigateToHomeView(HomeDisplayViewModel.Favourites)
    }

    override fun recentlyModifiedClick() {
        navigateToHomeView(HomeDisplayViewModel.RecentlyModified)
    }

    override fun sharedWithMeClick() {
        navigateToHomeView(HomeDisplayViewModel.SharedWithMe)
    }

    override fun ownedByMeClick() {
        navigateToHomeView(HomeDisplayViewModel.OwnedByMe)
    }

    override fun expiryClick() {
        navigateToHomeView(HomeDisplayViewModel.Expiry)
    }

    override fun foldersClick() {
        navigateToHomeView(HomeDisplayViewModel.folderRoot())
    }

    override fun tagsClick() {
        navigateToHomeView(HomeDisplayViewModel.tagsRoot())
    }

    override fun groupsClick() {
        navigateToHomeView(HomeDisplayViewModel.groupsRoot())
    }

    override fun folderItemClick(folderModel: FolderWithCountAndPath) {
        view?.navigateToChild(
            HomeDisplayViewModel.Folders(
                Folder.Child(
                    folderModel.folderId
                ),
                folderModel.name,
                folderModel.isShared
            )
        )
    }

    override fun tagItemClick(tag: TagWithCount) {
        view?.navigateToChild(
            HomeDisplayViewModel.Tags(tag.id, tag.slug, tag.isShared)
        )
    }

    override fun groupItemClick(group: GroupWithCount) {
        view?.navigateToChild(
            HomeDisplayViewModel.Groups(group.groupId, group.groupName)
        )
    }

    override fun createResourceClick() {
        view?.navigateToCreateResource(
            when (val currentHomeView = homeView) {
                is HomeDisplayViewModel.Folders -> currentHomeView.activeFolder.folderId
                else -> null
            }
        )
    }

    override fun closeClick() {
        view?.finish()
    }

    override fun menuFavouriteClick(option: ResourceMoreMenuModel.FavouriteOption) {
        coroutineScope.launch {
            performCommonResourceAction(
                action = { resourceCommonActionsInteractor.toggleFavourite(option) },
                doOnFailure = { view?.showToggleFavouriteFailure() },
                doOnSuccess = { showActiveHomeView() }
            )
        }
    }

    override fun moreClick() {
        when (val currentHomeView = homeView) {
            is HomeDisplayViewModel.Folders -> view?.navigateToFolderMoreMenu(
                FolderMoreMenuModel(currentHomeView.activeFolderName)
            )
            else -> {
                // more is present on folders only for now
            }
        }
    }

    override fun seeFolderDetailsClick() {
        val currentHomeView = homeView as HomeDisplayViewModel.Folders
        require(currentHomeView.activeFolder is Folder.Child)
        view?.navigateToFolderDetails(currentHomeView.activeFolder as Folder.Child)
    }

    override fun createFolderClick() {
        val currentHomeView = homeView as? HomeDisplayViewModel.Folders
        requireNotNull(currentHomeView) {
            "Create folder accessed not from folder context (${currentHomeView?.javaClass?.name})"
        }
        view?.navigateToCreateFolder(currentHomeView.activeFolder.folderId)
    }

    override fun folderCreated(name: String) {
        initRefresh()
        view?.showFolderCreated(name)
    }

    override fun otpQrScanReturned(isTotpCreated: Boolean, isManualCreationChosen: Boolean) {
        if (isTotpCreated) {
            initRefresh()
        } else if (isManualCreationChosen) {
            view?.navigateToCreateTotpManually(
                when (val currentHomeView = homeView) {
                    is HomeDisplayViewModel.Folders -> currentHomeView.activeFolder.folderId
                    else -> null
                }
            )
        }
    }

    override fun resourceFormReturned(isResourceCreated: Boolean, isResourceEdited: Boolean, resourceName: String?) {
        if (isResourceCreated) {
            initRefresh()
            view?.showResourceCreatedSnackbar()
        }
        if (isResourceEdited) {
            initRefresh()
            view?.showResourceEditedSnackbar(resourceName.orEmpty())
        }
    }

    override fun resourceDetailsReturned(isResourceEdited: Boolean, isResourceDeleted: Boolean, resourceName: String?) {
        if (isResourceEdited) {
            initRefresh()
            view?.showResourceEditedSnackbar(resourceName.orEmpty())
        }
        if (isResourceDeleted) {
            initRefresh()
            view?.showResourceDeletedSnackbar(resourceName.orEmpty())
        }
    }

    override fun menuCopyOtpClick() {
        coroutineScope.launch {
            performSecretPropertyAction(
                action = { secretPropertiesActionsInteractor.provideOtp() },
                doOnFetchFailure = { view?.showFetchFailure() },
                doOnDecryptionFailure = { view?.showDecryptionFailure() },
                doOnSuccess = {
                    if (it.result.key.isNotBlank()) {
                        val otpParameters = totpParametersProvider.provideOtpParameters(
                            secretKey = it.result.key,
                            digits = it.result.digits,
                            period = it.result.period,
                            algorithm = it.result.algorithm
                        )
                        view?.addToClipboard(it.label, otpParameters.otpValue, isSecret = true)
                    } else {
                        Timber.e("Fetched totp key is empty")
                        view?.showGeneralError()
                    }
                }
            )
        }
    }

    override fun onCreateResourceClick() {
        view?.showCreateResourceMenu(homeView)
    }
}
