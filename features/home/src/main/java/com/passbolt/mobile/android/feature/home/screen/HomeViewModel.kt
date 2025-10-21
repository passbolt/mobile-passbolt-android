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

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithFailure
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithSuccess
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.NotCompleted
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.InProgress
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderDetailsUseCase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.preferences.usecase.GetHomeDisplayViewPrefsUseCase
import com.passbolt.mobile.android.core.resources.actions.ResourceCommonActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourcePropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.performCommonResourceAction
import com.passbolt.mobile.android.core.resources.actions.performResourcePropertyAction
import com.passbolt.mobile.android.core.resources.actions.performSecretPropertyAction
import com.passbolt.mobile.android.core.ui.compose.search.SearchInputEndIconMode.AVATAR
import com.passbolt.mobile.android.core.ui.compose.search.SearchInputEndIconMode.CLEAR
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticatedViewModel
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CloseCreateResourceMenu
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CloseDeleteConfirmationDialog
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CloseResourceMoreMenu
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CloseSwitchAccount
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.ConfirmDeleteResource
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CopyNote
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CopyPassword
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CopyResourceMetadataDescription
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CopyResourceUri
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CopyResourceUsername
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CreateFolder
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CreateNote
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CreatePassword
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CreateTotp
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.DeleteResource
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.EditResource
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.FolderCreateReturned
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.Initialize
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.LaunchResourceWebsite
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.OpenCreateResourceMenu
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.OpenResourceMenu
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.OtpQRScanReturned
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.ResourceDetailsReturned
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.ResourceFormReturned
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.ResourceShareReturned
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.Search
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.SearchEndIconAction
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.ShareResource
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.ShowHomeView
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.ToggleResourceFavourite
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.CopyToClipboard
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.InitiateDataRefresh
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.NavigateToCreateFolder
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.NavigateToCreateResourceForm
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.NavigateToCreateTotp
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.NavigateToEditResourceForm
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.NavigateToResourceUri
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.NavigateToShare
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.OpenResourceMoreMenu
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.ShowSuccessSnackbar
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.ShowToast
import com.passbolt.mobile.android.feature.home.screen.SnackbarErrorType.DECRYPTION_FAILURE
import com.passbolt.mobile.android.feature.home.screen.SnackbarErrorType.FAILED_TO_DELETE_RESOURCE
import com.passbolt.mobile.android.feature.home.screen.SnackbarErrorType.FAILED_TO_REFRESH_DATA
import com.passbolt.mobile.android.feature.home.screen.SnackbarErrorType.FETCH_FAILURE
import com.passbolt.mobile.android.feature.home.screen.SnackbarErrorType.NO_SHARED_KEY_ACCESS
import com.passbolt.mobile.android.feature.home.screen.SnackbarErrorType.TOGGLE_FAVOURITE_FAILURE
import com.passbolt.mobile.android.feature.home.screen.SnackbarSuccessType.RESOURCE_CREATED
import com.passbolt.mobile.android.feature.home.screen.SnackbarSuccessType.RESOURCE_DELETED
import com.passbolt.mobile.android.feature.home.screen.SnackbarSuccessType.RESOURCE_EDITED
import com.passbolt.mobile.android.feature.home.screen.SnackbarSuccessType.RESOURCE_SHARED
import com.passbolt.mobile.android.feature.home.screen.ToastType.WAIT_FOR_DATA_REFRESH_FINISH
import com.passbolt.mobile.android.feature.home.screen.data.HomeDataProvider
import com.passbolt.mobile.android.mappers.HomeDisplayViewMapper
import com.passbolt.mobile.android.metadata.usecase.CanCreateResourceUseCase
import com.passbolt.mobile.android.metadata.usecase.CanShareResourceUseCase
import com.passbolt.mobile.android.ui.Folder.Child
import com.passbolt.mobile.android.ui.Folder.Root
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Folders
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Groups
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Tags
import com.passbolt.mobile.android.ui.LeadingContentType.PASSWORD
import com.passbolt.mobile.android.ui.LeadingContentType.STANDALONE_NOTE
import com.passbolt.mobile.android.ui.LeadingContentType.TOTP
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.FavouriteOption
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import timber.log.Timber

internal class HomeViewModel(
    private val coroutineLaunchContext: CoroutineLaunchContext,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val getHomeDisplayViewPrefsUseCase: GetHomeDisplayViewPrefsUseCase,
    private val homeModelMapper: HomeDisplayViewMapper,
    private val homeDataProvider: HomeDataProvider,
    private val getLocalFolderUseCase: GetLocalFolderDetailsUseCase,
    private val canCreateResourceUse: CanCreateResourceUseCase,
    private val canShareResourceUse: CanShareResourceUseCase,
) : AuthenticatedViewModel<HomeState, HomeSideEffect>(HomeState()),
    KoinComponent {
    private val resourcePropertiesActionsInteractor: ResourcePropertiesActionsInteractor
        get() = get { parametersOf(requireNotNull(viewState.value.moreMenuResource)) }
    private val secretPropertiesActionsInteractor: SecretPropertiesActionsInteractor
        get() = get { parametersOf(requireNotNull(viewState.value.moreMenuResource)) }
    private val resourceCommonActionsInteractor: ResourceCommonActionsInteractor
        get() = get { parametersOf(requireNotNull(viewState.value.moreMenuResource)) }

    init {
        loadUserAvatar()
    }

    private fun loadUserAvatar() {
        updateViewState {
            copy(
                userAvatar =
                    getSelectedAccountDataUseCase
                        .execute(Unit)
                        .avatarUrl,
            )
        }
    }

    @Suppress("CyclomaticComplexMethod")
    fun onIntent(intent: HomeIntent) {
        when (intent) {
            CloseCreateResourceMenu -> updateViewState { copy(showCreateResourceBottomSheet = false) }
            CloseDeleteConfirmationDialog -> updateViewState { copy(showDeleteResourceConfirmationDialog = false) }
            CloseResourceMoreMenu -> updateViewState { copy(showResourceMoreBottomSheet = false, moreMenuResource = null) }
            CloseSwitchAccount -> updateViewState { copy(showAccountSwitchBottomSheet = false) }
            OpenCreateResourceMenu -> updateViewState { copy(showCreateResourceBottomSheet = true) }
            DeleteResource -> updateViewState { copy(showDeleteResourceConfirmationDialog = true) }
            ConfirmDeleteResource -> deleteResource()
            CreateNote -> createNote()
            CreatePassword -> createPassword()
            CreateTotp -> createTotp()
            CreateFolder -> createFolder()
            is Initialize -> initialize(intent)
            is OpenResourceMenu -> openResourceMoreMenu(intent)
            is Search -> searchQueryChanged(intent.searchQuery)
            SearchEndIconAction -> searchEndIconAction()
            is ShowHomeView -> showHomeView(intent.homeView)
            CopyPassword -> copyPassword()
            CopyResourceMetadataDescription -> copyMetadataDescription()
            CopyNote -> copyResourceNote()
            CopyResourceUri -> copyResourceUri()
            CopyResourceUsername -> copyResourceUsername()
            EditResource -> emitSideEffect(NavigateToEditResourceForm(viewState.value.requireMoreMenuResource))
            LaunchResourceWebsite -> launchResourceWebsite()
            ShareResource -> onCanShareResource { emitSideEffect(NavigateToShare(viewState.value.requireMoreMenuResource)) }
            is ToggleResourceFavourite -> toggleFavourite(intent.option)
            is FolderCreateReturned -> folderCreationReturned(intent)
            is OtpQRScanReturned -> processOtpScanResult(intent)
            is ResourceFormReturned -> processResourceFormResult(intent)
            is ResourceDetailsReturned -> resourceDetailsReturned(intent)
            is ResourceShareReturned -> resourceShareReturned(intent)
        }
    }

    private fun folderCreationReturned(intent: FolderCreateReturned) {
        emitSideEffect(ShowSuccessSnackbar(SnackbarSuccessType.FOLDER_CREATED, intent.folderName))
        emitSideEffect(InitiateDataRefresh)
    }

    private fun openResourceMoreMenu(intent: OpenResourceMenu) {
        updateViewState { copy(moreMenuResource = intent.resourceModel) }
        emitSideEffect(
            OpenResourceMoreMenu(
                intent.resourceModel.resourceId,
                intent.resourceModel.metadataJsonModel.name,
            ),
        )
    }

    private fun createFolder() {
        updateViewState { copy(showCreateResourceBottomSheet = false) }
        emitSideEffect(NavigateToCreateFolder(folderId = viewState.value.currentFolderId))
    }

    private fun createTotp() {
        updateViewState { copy(showCreateResourceBottomSheet = false) }
        onCanCreateResource { emitSideEffect(NavigateToCreateTotp(folderId = viewState.value.currentFolderId)) }
    }

    private fun createPassword() {
        updateViewState { copy(showCreateResourceBottomSheet = false) }
        onCanCreateResource {
            emitSideEffect(
                NavigateToCreateResourceForm(
                    leadingContentType = PASSWORD,
                    folderId = viewState.value.currentFolderId,
                ),
            )
        }
    }

    private fun createNote() {
        updateViewState { copy(showCreateResourceBottomSheet = false) }
        onCanCreateResource {
            emitSideEffect(
                NavigateToCreateResourceForm(
                    leadingContentType = STANDALONE_NOTE,
                    folderId = viewState.value.currentFolderId,
                ),
            )
        }
    }

    private fun resourceShareReturned(intent: ResourceShareReturned) {
        if (intent.resourceShared) {
            emitSideEffect(ShowSuccessSnackbar(RESOURCE_SHARED))
            emitSideEffect(InitiateDataRefresh)
        }
    }

    private fun resourceDetailsReturned(intent: ResourceDetailsReturned) {
        if (intent.resourceEdited) {
            emitSideEffect(ShowSuccessSnackbar(RESOURCE_EDITED, intent.resourceName))
            emitSideEffect(InitiateDataRefresh)
        }
        if (intent.resourceDeleted) {
            emitSideEffect(ShowSuccessSnackbar(RESOURCE_DELETED, intent.resourceName))
            emitSideEffect(InitiateDataRefresh)
        }
    }

    private fun toggleFavourite(option: FavouriteOption) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            performCommonResourceAction(
                action = { resourceCommonActionsInteractor.toggleFavourite(option) },
                doOnFailure = { emitSideEffect(ShowErrorSnackbar(TOGGLE_FAVOURITE_FAILURE)) },
                doOnSuccess = { showHomeView(viewState.value.homeView) },
            )
        }
    }

    private fun launchResourceWebsite() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            performResourcePropertyAction(
                action = { resourcePropertiesActionsInteractor.provideMainUri() },
                doOnResult = { emitSideEffect(NavigateToResourceUri(it.result)) },
            )
        }
    }

    private fun copyPassword() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            performSecretPropertyAction(
                action = { secretPropertiesActionsInteractor.providePassword() },
                doOnDecryptionFailure = { emitSideEffect(ShowErrorSnackbar(DECRYPTION_FAILURE)) },
                doOnFetchFailure = { emitSideEffect(ShowErrorSnackbar(FETCH_FAILURE)) },
                doOnSuccess = { emitSideEffect(CopyToClipboard(it.label, it.result.orEmpty(), it.isSecret)) },
            )
        }
    }

    private fun copyResourceUsername() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            performResourcePropertyAction(
                action = { resourcePropertiesActionsInteractor.provideUsername() },
                doOnResult = { emitSideEffect(CopyToClipboard(it.label, it.result, it.isSecret)) },
            )
        }
    }

    private fun copyMetadataDescription() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            performResourcePropertyAction(
                action = { resourcePropertiesActionsInteractor.provideDescription() },
                doOnResult = { emitSideEffect(CopyToClipboard(it.label, it.result, it.isSecret)) },
            )
        }
    }

    private fun copyResourceNote() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            performSecretPropertyAction(
                action = { secretPropertiesActionsInteractor.provideNote() },
                doOnDecryptionFailure = { emitSideEffect(ShowErrorSnackbar(DECRYPTION_FAILURE)) },
                doOnFetchFailure = { emitSideEffect(ShowErrorSnackbar(FETCH_FAILURE)) },
                doOnSuccess = { emitSideEffect(CopyToClipboard(it.label, it.result, it.isSecret)) },
            )
        }
    }

    fun copyResourceUri() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            performResourcePropertyAction(
                action = { resourcePropertiesActionsInteractor.provideMainUri() },
                doOnResult = { emitSideEffect(CopyToClipboard(it.label, it.result, it.isSecret)) },
            )
        }
    }

    private fun deleteResource() {
        updateViewState { copy(showDeleteResourceConfirmationDialog = false, showProgress = true) }
        viewModelScope.launch(coroutineLaunchContext.io) {
            performCommonResourceAction(
                action = { resourceCommonActionsInteractor.deleteResource() },
                doOnFailure = { emitSideEffect(ShowErrorSnackbar(FAILED_TO_DELETE_RESOURCE)) },
                doOnSuccess = {
                    emitSideEffect(InitiateDataRefresh)
                    emitSideEffect(ShowSuccessSnackbar(RESOURCE_DELETED, it.resourceName))
                },
            )
            updateViewState { copy(showProgress = false) }
        }
    }

    private fun processResourceFormResult(intent: ResourceFormReturned) {
        if (intent.resourceCreated) {
            emitSideEffect(InitiateDataRefresh)
            emitSideEffect(ShowSuccessSnackbar(RESOURCE_CREATED, intent.resourceName))
        }
        if (intent.resourceEdited) {
            emitSideEffect(InitiateDataRefresh)
            emitSideEffect(ShowSuccessSnackbar(RESOURCE_EDITED, intent.resourceName))
        }
    }

    private fun processOtpScanResult(intent: OtpQRScanReturned) {
        if (intent.otpCreated) {
            emitSideEffect(InitiateDataRefresh)
        } else {
            if (intent.otpManualCreationChosen) {
                emitSideEffect(
                    NavigateToCreateResourceForm(
                        leadingContentType = TOTP,
                        folderId = (viewState.value.homeView as? Folders)?.activeFolder?.folderId,
                    ),
                )
            }
        }
    }

    private fun searchEndIconAction() {
        when (viewState.value.searchInputEndIconMode) {
            AVATAR -> {
                viewModelScope.launch(coroutineLaunchContext.io) {
                    if (dataRefreshTrackingFlow.isInProgress()) {
                        emitSideEffect(ShowToast(WAIT_FOR_DATA_REFRESH_FINISH))
                        dataRefreshTrackingFlow.awaitIdle()
                    }
                    updateViewState { copy(showAccountSwitchBottomSheet = true) }
                }
            }
            CLEAR -> {
                searchQueryChanged("")
                updateViewState {
                    copy(searchInputEndIconMode = AVATAR)
                }
            }
        }
    }

    private fun searchQueryChanged(searchQuery: String) {
        val searchEndIcon = if (searchQuery.isNotBlank()) CLEAR else AVATAR
        viewModelScope.launch {
            val homeData = getHomeData(viewState.value.homeView, searchQuery)
            updateViewState {
                copy(
                    searchInputEndIconMode = searchEndIcon,
                    searchQuery = searchQuery,
                    homeData = homeData,
                )
            }
        }
    }

    private fun showHomeView(homeDisplay: HomeDisplayViewModel) {
        viewModelScope.launch {
            val homeData = getHomeData(homeDisplay, viewState.value.searchQuery)
            updateViewState {
                copy(
                    homeView = homeDisplay,
                    homeData = homeData,
                )
            }
        }
    }

    private fun initialize(intent: Initialize) {
        val filterPreferences = getHomeDisplayViewPrefsUseCase.execute(Unit)

        viewModelScope.launch {
            val homeView =
                intent.homeView ?: homeModelMapper.map(
                    filterPreferences.userSetHomeView,
                    filterPreferences.lastUsedHomeView,
                )
            val homeData = getHomeData(homeView, viewState.value.searchQuery)
            updateViewState {
                copy(
                    showSuggestedModel = intent.showSuggestedModel,
                    homeView = homeView,
                    homeData = homeData,
                )
            }
            viewModelScope.launch(coroutineLaunchContext.io) {
                synchronizeWithDataRefresh()
            }
        }
    }

    private suspend fun getHomeData(
        homeView: HomeDisplayViewModel,
        searchQuery: String? = null,
    ) = homeDataProvider.provideData(
        searchQuery,
        homeView,
        viewState.value.showSuggestedModel,
    )

    private suspend fun shouldShowCreateButton(): Boolean {
        viewState.value.homeView.let {
            // currently do not show add button on tags and groups
            if (it is Tags || it is Groups) {
                return false
            }
            // show only in folder with update permission
            if (it is Folders) {
                return when (val currentFolder = it.activeFolder) {
                    is Child ->
                        runWithHandlingItemDeleted({
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

    private suspend fun <T> runWithHandlingItemDeleted(
        action: suspend () -> T,
        resultIfActionFails: T,
    ): T =
        try {
            action()
        } catch (_: Exception) {
            Timber.d("Active item has been deleted, navigating to root")
            showHomeView(
                when (viewState.value.homeView) {
                    is Folders -> HomeDisplayViewModel.folderRoot()
                    is Groups -> HomeDisplayViewModel.groupsRoot()
                    is Tags -> HomeDisplayViewModel.tagsRoot()
                    else -> viewState.value.homeView
                },
            )
            resultIfActionFails
        }

    private suspend fun synchronizeWithDataRefresh() {
        dataRefreshTrackingFlow.dataRefreshStatusFlow.collect {
            when (it) {
                InProgress -> updateViewState { copy(isRefreshing = true, canCreateResource = false) }
                FinishedWithFailure -> {
                    emitSideEffect(ShowErrorSnackbar(FAILED_TO_REFRESH_DATA))
                    updateViewState { copy(isRefreshing = false, canCreateResource = false) }
                }
                FinishedWithSuccess -> {
                    val showCreateResourceButton = shouldShowCreateButton()
                    val homeData = getHomeData(viewState.value.homeView, viewState.value.searchQuery)
                    updateViewState {
                        copy(
                            homeData = homeData,
                            isRefreshing = false,
                            canCreateResource = showCreateResourceButton,
                        )
                    }
                }
                NotCompleted -> {
                    // do nothing
                }
            }
        }
    }

    private fun onCanShareResource(function: () -> Unit) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            if (canShareResourceUse.execute(Unit).canShareResource) {
                function()
            } else {
                emitSideEffect(ShowErrorSnackbar(NO_SHARED_KEY_ACCESS))
            }
        }
    }

    private fun onCanCreateResource(function: () -> Unit) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            if (canCreateResourceUse.execute(CanCreateResourceUseCase.Input(folderId = null)).canCreateResource) {
                function()
            } else {
                emitSideEffect(ShowErrorSnackbar(NO_SHARED_KEY_ACCESS))
            }
        }
    }
}
