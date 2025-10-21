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

package com.passbolt.mobile.android.feature.home.screen

/**
 * Presenter responsible for managing the home resource list. The general flow is to fetch resources and resource types
 * from the backend on start and update the database. Then when applying different views (all, favourite,
 * shared with me, etc.) the reload is done from the database only. To refresh from backend again users can do the
 * swipe to refresh gesture.
 */

import com.passbolt.mobile.android.common.extension.areListsEmpty
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderDetailsUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.base.DataRefreshViewReactivePresenter
import com.passbolt.mobile.android.core.idlingresource.DeleteResourceIdlingResource
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider.OtpParametersResult.InvalidTotpInput
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider.OtpParametersResult.OtpParameters
import com.passbolt.mobile.android.core.preferences.usecase.GetHomeDisplayViewPrefsUseCase
import com.passbolt.mobile.android.core.resources.actions.ResourceCommonActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourcePropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.performCommonResourceAction
import com.passbolt.mobile.android.core.resources.actions.performResourcePropertyAction
import com.passbolt.mobile.android.core.resources.actions.performSecretPropertyAction
import com.passbolt.mobile.android.core.ui.compose.search.SearchInputEndIconMode
import com.passbolt.mobile.android.feature.home.screen.data.HomeDataProvider
import com.passbolt.mobile.android.mappers.HomeDisplayViewMapper
import com.passbolt.mobile.android.metadata.usecase.CanCreateResourceUseCase
import com.passbolt.mobile.android.metadata.usecase.CanShareResourceUseCase
import com.passbolt.mobile.android.ui.Folder.Child
import com.passbolt.mobile.android.ui.Folder.Root
import com.passbolt.mobile.android.ui.FolderMoreMenuModel
import com.passbolt.mobile.android.ui.FolderWithCountAndPath
import com.passbolt.mobile.android.ui.GroupWithCount
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.AllItems
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Expiry
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Favourites
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Folders
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Groups
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.OwnedByMe
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.RecentlyModified
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.SharedWithMe
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Tags
import com.passbolt.mobile.android.ui.LeadingContentType.PASSWORD
import com.passbolt.mobile.android.ui.LeadingContentType.STANDALONE_NOTE
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
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import timber.log.Timber

@Suppress("TooManyFunctions", "LargeClass") // TODO MOB-321
class HomePresenter(
    private val coroutineLaunchContext: CoroutineLaunchContext,
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val getHomeDisplayViewPrefsUseCase: GetHomeDisplayViewPrefsUseCase,
    private val homeModelMapper: HomeDisplayViewMapper,
    private val getLocalFolderUseCase: GetLocalFolderDetailsUseCase,
    private val deleteResourceIdlingResource: DeleteResourceIdlingResource,
    private val totpParametersProvider: TotpParametersProvider,
    private val canCreateResourceUse: CanCreateResourceUseCase,
    private val canShareResourceUse: CanShareResourceUseCase,
) : DataRefreshViewReactivePresenter<HomeContract.View>(coroutineLaunchContext),
    HomeContract.Presenter,
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

    private var currentMoreMenuResource: ResourceModel? = null

    private var userAvatarUrl: String? = null
    private val searchInputEndIconMode
        get() = if (currentSearchText.value.isBlank()) SearchInputEndIconMode.AVATAR else SearchInputEndIconMode.CLEAR

    private val resourcePropertiesActionsInteractor: ResourcePropertiesActionsInteractor
        get() = get { parametersOf(requireNotNull(currentMoreMenuResource)) }
    private val secretPropertiesActionsInteractor: SecretPropertiesActionsInteractor
        get() = get { parametersOf(requireNotNull(currentMoreMenuResource)) }
    private val resourceCommonActionsInteractor: ResourceCommonActionsInteractor
        get() = get { parametersOf(requireNotNull(currentMoreMenuResource)) }

    private val homeDataProvider: HomeDataProvider by inject()

    override fun argsRetrieved(
        showSuggestedModel: ShowSuggestedModel,
        homeDisplayView: HomeDisplayViewModel?,
        hasPreviousEntry: Boolean,
        shouldShowCloseButton: Boolean,
        shouldShowResourceMoreMenu: Boolean,
    ) {
        val filterPreferences = getHomeDisplayViewPrefsUseCase.execute(Unit)
        this.homeView = homeDisplayView ?: homeModelMapper.map(
            filterPreferences.userSetHomeView,
            filterPreferences.lastUsedHomeView,
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
            is AllItems -> view.showAllItemsSearchHint()
            else -> view.showDefaultSearchHint()
        }
    }

    private fun processScreenTitle(view: HomeContract.View) {
        when (val currentHomeView = homeView) {
            is Folders -> processFoldersTitle(currentHomeView, view)
            is Tags -> processTagsTitle(currentHomeView, view)
            is Groups -> processGroupsTitle(currentHomeView, view)
            else -> view.showHomeScreenTitle(currentHomeView)
        }
    }

    private fun processGroupsTitle(
        currentHomeView: Groups,
        view: HomeContract.View,
    ) {
        if (currentHomeView.activeGroupId != null) {
            view.showGroupTitle(requireNotNull(currentHomeView.activeGroupName))
        } else {
            view.showHomeScreenTitle(currentHomeView)
        }
    }

    private fun processTagsTitle(
        currentHomeView: Tags,
        view: HomeContract.View,
    ) {
        if (currentHomeView.activeTagId != null && currentHomeView.isActiveTagShared != null) {
            view.showTagTitle(
                requireNotNull(currentHomeView.activeTagName),
                requireNotNull(currentHomeView.isActiveTagShared),
            )
        } else {
            view.showHomeScreenTitle(currentHomeView)
        }
    }

    private fun processFoldersTitle(
        currentHomeView: Folders,
        view: HomeContract.View,
    ) {
        when (currentHomeView.activeFolder) {
            is Child ->
                view.showChildFolderTitle(
                    requireNotNull(currentHomeView.activeFolderName),
                    requireNotNull(currentHomeView.isActiveFolderShared),
                )
            is Root -> view.showHomeScreenTitle(currentHomeView)
        }
    }

    override fun refreshInProgressAction() {
        view?.hideCreateButton()
    }

    override fun refreshSuccessAction() {
        coroutineScope.launch {
            showActiveHomeView()
        }
    }

    override fun refreshFailureAction() {
        view?.showDataRefreshError()
        view?.hideCreateButton()
    }

    private fun collectFilteringRefreshes() {
        filteringScope.launch {
            currentSearchText
                .drop(1) // initial empty value
                .collectLatest {
                    Timber.d("New search text received")
                    processSearchIconChange()
                    showActiveHomeView()
                }
        }
    }

    private suspend fun shouldShowCreateButton(): Boolean {
        homeView.let {
            // currently do not show add button on tags and groups
            if (it is Tags || it is Groups) {
                return false
            }
            // show only in folder with update permission
            if (it is Folders) {
                return when (val currentFolder = it.activeFolder) {
                    is Child ->
                        runWithHandlingMissingItem({
                            getLocalFolderUseCase
                                .execute(GetLocalFolderDetailsUseCase.Input(currentFolder.folderId))
                                .folder.permission in setOf(ResourcePermission.OWNER, ResourcePermission.UPDATE)
                        }, resultIfActionFails = false)

                    is Root -> true
                }
            }
        }
        return true
    }

    private suspend fun <T> runWithHandlingMissingItem(
        action: suspend () -> T,
        resultIfActionFails: T,
    ): T =
        try {
            action()
        } catch (exception: Exception) {
            // the current filtering item (tag, folder, group)
            // was deleted from other application instance and full refresh was done while being on that item
            // in that case navigate to selected filter root and show info message
            navigateToHomeView(
                when (homeView) {
                    is AllItems -> AllItems
                    is Favourites -> Favourites
                    is Folders -> HomeDisplayViewModel.folderRoot()
                    is Groups -> HomeDisplayViewModel.groupsRoot()
                    is OwnedByMe -> OwnedByMe
                    is RecentlyModified -> RecentlyModified
                    is SharedWithMe -> SharedWithMe
                    is Tags -> HomeDisplayViewModel.tagsRoot()
                    is Expiry -> Expiry
                },
            )
            view?.showContentNotAvailable()
            resultIfActionFails
        }

    // show in child folders only
    private fun shouldShowFolderMoreMenu(shouldShowResourceMoreMenu: Boolean) =
        shouldShowResourceMoreMenu &&
            homeView.let {
                it is Folders && it.activeFolder is Child
            }

    private fun handleBackArrowVisibility() {
        if (hasPreviousBackEntry) {
            view?.showBackArrow()
        } else {
            view?.hideBackArrow()
        }
    }

    private fun loadUserAvatar() {
        userAvatarUrl =
            getSelectedAccountDataUseCase
                .execute(Unit)
                .avatarUrl
                .also { view?.displaySearchAvatar(it) }
    }

    private fun showActiveHomeView() {
        view?.let { processScreenTitle(it) }
        coroutineScope.launch {
            if (shouldShowCreateButton()) {
                view?.showCreateButton()
            } else {
                view?.hideCreateButton()
            }

            runWithHandlingMissingItem(action = {
                val homeData =
                    withContext(coroutineLaunchContext.io) {
                        homeDataProvider.provideData(
                            currentSearchText.value,
                            homeView,
                            showSuggestedModel,
                        )
                    }

                if (areListsEmpty(
                        homeData.data.resourceList,
                        homeData.data.foldersList,
                        homeData.data.tagsList,
                        homeData.data.groupsList,
                        homeData.data.suggestedResourceList,
                    )
                ) {
                    if (currentSearchText.value.isNotBlank()) {
                        view?.showSearchEmptyList()
                    } else {
                        view?.showEmptyList()
                    }
                } else {
                    view?.showItems(
                        homeData.data.suggestedResourceList,
                        homeData.data.resourceList,
                        homeData.data.foldersList,
                        homeData.data.tagsList,
                        homeData.data.groupsList,
                        homeData.data.filteredSubFolders,
                        homeData.data.filteredSubFolderResources,
                        homeData.headerSectionConfiguration,
                    )
                }
            }, resultIfActionFails = Unit)
        }
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
                doOnResult = { view?.addToClipboard(it.label, it.result, it.isSecret) },
            )
        }
    }

    override fun menuLaunchWebsiteClick() {
        coroutineScope.launch {
            performResourcePropertyAction(
                action = { resourcePropertiesActionsInteractor.provideMainUri() },
                doOnResult = { view?.openWebsite(it.result) },
            )
        }
    }

    override fun menuCopyUrlClick() {
        coroutineScope.launch {
            performResourcePropertyAction(
                action = { resourcePropertiesActionsInteractor.provideMainUri() },
                doOnResult = { view?.addToClipboard(it.label, it.result, it.isSecret) },
            )
        }
    }

    override fun menuCopyPasswordClick() {
        coroutineScope.launch {
            performSecretPropertyAction(
                action = { secretPropertiesActionsInteractor.providePassword() },
                doOnDecryptionFailure = { view?.showDecryptionFailure() },
                doOnFetchFailure = { view?.showFetchFailure() },
                doOnSuccess = { view?.addToClipboard(it.label, it.result.orEmpty(), it.isSecret) },
            )
        }
    }

    override fun menuCopyMetadataDescriptionClick() {
        coroutineScope.launch {
            performResourcePropertyAction(
                action = { resourcePropertiesActionsInteractor.provideDescription() },
                doOnResult = { view?.addToClipboard(it.label, it.result, it.isSecret) },
            )
        }
    }

    override fun menuCopyNoteClick() {
        coroutineScope.launch {
            performSecretPropertyAction(
                action = { secretPropertiesActionsInteractor.provideNote() },
                doOnDecryptionFailure = { view?.showDecryptionFailure() },
                doOnFetchFailure = { view?.showFetchFailure() },
                doOnSuccess = { view?.addToClipboard(it.label, it.result, it.isSecret) },
            )
        }
    }

    override fun searchAvatarClick() {
        if (dataRefreshTrackingFlow.isInProgress()) {
            view?.showPleaseWaitForDataRefresh()
        } else {
            view?.navigateToSwitchAccount()
        }
    }

    override fun menuDeleteClick() {
        view?.showDeleteConfirmationDialog()
    }

    override fun deleteResourceConfirmed() {
        runWhileShowingProgress {
            deleteResourceIdlingResource.setIdle(false)
            performCommonResourceAction(
                action = { resourceCommonActionsInteractor.deleteResource() },
                doOnFailure = { view?.showDeleteResourceFailure() },
                doOnSuccess = { resourceDeleted(it.resourceName) },
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
        view?.performFullDataRefresh()
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
        onCanShareResource {
            view?.navigateToShare(
                requireNotNull(currentMoreMenuResource),
            )
        }
    }

    override fun allItemsClick() {
        navigateToHomeView(AllItems)
    }

    override fun favouritesClick() {
        navigateToHomeView(Favourites)
    }

    override fun recentlyModifiedClick() {
        navigateToHomeView(RecentlyModified)
    }

    override fun sharedWithMeClick() {
        navigateToHomeView(SharedWithMe)
    }

    override fun ownedByMeClick() {
        navigateToHomeView(OwnedByMe)
    }

    override fun expiryClick() {
        navigateToHomeView(Expiry)
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
            Folders(
                Child(
                    folderModel.folderId,
                ),
                folderModel.name,
                folderModel.isShared,
            ),
        )
    }

    override fun tagItemClick(tag: TagWithCount) {
        view?.navigateToChild(
            Tags(tag.id, tag.slug, tag.isShared),
        )
    }

    override fun groupItemClick(group: GroupWithCount) {
        view?.navigateToChild(
            Groups(group.groupId, group.groupName),
        )
    }

    override fun createPasswordClick() {
        onCanCreateResource {
            view?.navigateToCreateResource(
                when (val currentHomeView = homeView) {
                    is Folders -> currentHomeView.activeFolder.folderId
                    else -> null
                },
                PASSWORD,
            )
        }
    }

    override fun createTotpClick() {
        onCanCreateResource {
            val parentFolderId = (homeView as? Folders)?.activeFolder?.folderId
            view?.navigateToScanTotp(parentFolderId)
        }
    }

    private fun onCanCreateResource(function: () -> Unit) {
        coroutineScope.launch {
            val folderId = (homeView as? Folders)?.activeFolder?.folderId
            if (canCreateResourceUse.execute(CanCreateResourceUseCase.Input(folderId)).canCreateResource) {
                function()
            } else {
                view?.showCannotPerformThisActionMessage()
            }
        }
    }

    private fun onCanShareResource(function: () -> Unit) {
        coroutineScope.launch {
            if (canShareResourceUse.execute(Unit).canShareResource) {
                function()
            } else {
                view?.showCannotPerformThisActionMessage()
            }
        }
    }

    override fun createNoteClick() {
        onCanCreateResource {
            view?.navigateToCreateResource(
                when (val currentHomeView = homeView) {
                    is Folders -> currentHomeView.activeFolder.folderId
                    else -> null
                },
                STANDALONE_NOTE,
            )
        }
    }

    override fun closeClick() {
        view?.finish()
    }

    override fun menuFavouriteClick(option: ResourceMoreMenuModel.FavouriteOption) {
        coroutineScope.launch {
            performCommonResourceAction(
                action = { resourceCommonActionsInteractor.toggleFavourite(option) },
                doOnFailure = { view?.showToggleFavouriteFailure() },
                doOnSuccess = { showActiveHomeView() },
            )
        }
    }

    override fun moreClick() {
        when (val currentHomeView = homeView) {
            is Folders ->
                view?.navigateToFolderMoreMenu(
                    FolderMoreMenuModel(currentHomeView.activeFolderName),
                )
            else -> {
                // more is present on folders only for now
            }
        }
    }

    override fun seeFolderDetailsClick() {
        val currentHomeView = homeView as Folders
        require(currentHomeView.activeFolder is Child)
        view?.navigateToFolderDetails(currentHomeView.activeFolder as Child)
    }

    override fun createFolderClick() {
        val currentHomeView = homeView as? Folders
        requireNotNull(currentHomeView) {
            "Create folder accessed not from folder context (${currentHomeView?.javaClass?.name})"
        }
        view?.navigateToCreateFolder(currentHomeView.activeFolder.folderId)
    }

    override fun folderCreated(name: String) {
        initRefresh()
        view?.showFolderCreated(name)
    }

    override fun otpQrScanReturned(
        isTotpCreated: Boolean,
        isManualCreationChosen: Boolean,
    ) {
        if (isTotpCreated) {
            initRefresh()
        } else if (isManualCreationChosen) {
            view?.navigateToCreateTotpManually(
                when (val currentHomeView = homeView) {
                    is Folders -> currentHomeView.activeFolder.folderId
                    else -> null
                },
            )
        }
    }

    override fun resourceFormReturned(
        isResourceCreated: Boolean,
        isResourceEdited: Boolean,
        resourceName: String?,
    ) {
        if (isResourceCreated) {
            initRefresh()
            view?.showResourceCreatedSnackbar()
        }
        if (isResourceEdited) {
            initRefresh()
            view?.showResourceEditedSnackbar(resourceName.orEmpty())
        }
    }

    override fun resourceDetailsReturned(
        isResourceEdited: Boolean,
        isResourceDeleted: Boolean,
        resourceName: String?,
    ) {
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
                        val otpParametersResult =
                            totpParametersProvider.provideOtpParameters(
                                secretKey = it.result.key,
                                digits = it.result.digits,
                                period = it.result.period,
                                algorithm = it.result.algorithm,
                            )
                        when (otpParametersResult) {
                            is OtpParameters -> view?.addToClipboard(it.label, otpParametersResult.otpValue, isSecret = true)
                            InvalidTotpInput -> view?.showGeneralError()
                        }
                    } else {
                        Timber.e("Fetched totp key is empty")
                        view?.showGeneralError()
                    }
                },
            )
        }
    }

    override fun onCreateResourceClick() {
        view?.showCreateResourceMenu(homeView)
    }

    private fun runWhileShowingProgress(action: suspend () -> Unit) {
        coroutineScope.launch {
            view?.showProgress()
            action()
            view?.hideProgress()
        }
    }
}
