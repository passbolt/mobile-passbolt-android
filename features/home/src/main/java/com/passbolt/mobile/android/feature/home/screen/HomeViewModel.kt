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
import com.passbolt.mobile.android.common.autofill.DetectAutofillConflict
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithFailure
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithSuccess
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.NotCompleted
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.InProgress
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.ui.search.SearchInputEndIconMode.AVATAR
import com.passbolt.mobile.android.core.ui.search.SearchInputEndIconMode.CLEAR
import com.passbolt.mobile.android.core.ui.search.SearchInputEndIconMode.NONE
import com.passbolt.mobile.android.domain.accounts.AccountSwitchFlow
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.folders.usecase.GetLocalFolderDetailsUseCase
import com.passbolt.mobile.android.domain.metadata.interactor.ResourceAccessInteractor
import com.passbolt.mobile.android.domain.preferences.mapper.toHomeDisplayViewModel
import com.passbolt.mobile.android.domain.preferences.usecase.GetHomeDisplayViewPreferencesUseCase
import com.passbolt.mobile.android.domain.resources.actions.ResourceCommonActionsInteractor
import com.passbolt.mobile.android.domain.resources.actions.ResourcePropertiesActionsInteractor
import com.passbolt.mobile.android.domain.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.domain.resources.actions.performCommonResourceAction
import com.passbolt.mobile.android.domain.resources.actions.performResourcePropertyAction
import com.passbolt.mobile.android.domain.resources.actions.performSecretPropertyAction
import com.passbolt.mobile.android.domain.secrets.offline.OfflineSessionState
import com.passbolt.mobile.android.domain.secrets.usecase.offline.GetOfflineCacheStatusUseCase
import com.passbolt.mobile.android.domain.secrets.usecase.offline.MarkResourceOfflineUseCase
import com.passbolt.mobile.android.domain.secrets.usecase.offline.UnmarkResourceOfflineUseCase
import com.passbolt.mobile.android.domain.users.profile.UserProfileInteractor
import com.passbolt.mobile.android.domain.users.profile.UserProfileInteractor.Output.Failure
import com.passbolt.mobile.android.domain.users.profile.UserProfileInteractor.Output.Success
import com.passbolt.mobile.android.domain.users.profile.UserProfileRefreshTrackingFlow
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CloseCreateResourceMenu
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CloseDeleteConfirmationDialog
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CloseFiltersBottomSheet
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CloseFolderMoreMenu
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
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CreatePinCode
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CreateTotp
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.DeleteResource
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.EditResource
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.FolderCreateReturned
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.Initialize
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.LaunchResourceWebsite
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.OnResume
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.OpenCreateResourceMenu
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.OpenFiltersBottomSheet
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.OpenFolderMoreMenu
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
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.ToggleResourceOfflineAvailability
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.ViewFolderDetails
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.CopyToClipboard
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.InitiateDataRefresh
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.NavigateToCreateFolder
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.NavigateToCreateResourceForm
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.NavigateToCreateTotp
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.NavigateToEditResourceForm
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.NavigateToFolderDetails
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.NavigateToResourceUri
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.NavigateToShare
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.ShowSuccessSnackbar
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.ShowToast
import com.passbolt.mobile.android.feature.home.screen.SnackbarErrorType.DECRYPTION_FAILURE
import com.passbolt.mobile.android.feature.home.screen.SnackbarErrorType.FAILED_TO_DELETE_RESOURCE
import com.passbolt.mobile.android.feature.home.screen.SnackbarErrorType.FAILED_TO_REFRESH_DATA
import com.passbolt.mobile.android.feature.home.screen.SnackbarErrorType.FETCH_FAILURE
import com.passbolt.mobile.android.feature.home.screen.SnackbarErrorType.NO_SHARED_KEY_ACCESS
import com.passbolt.mobile.android.feature.home.screen.SnackbarErrorType.PROFILE_FETCH_FAILURE
import com.passbolt.mobile.android.feature.home.screen.SnackbarErrorType.TOGGLE_FAVOURITE_FAILURE
import com.passbolt.mobile.android.feature.home.screen.SnackbarErrorType.TOGGLE_OFFLINE_AVAILABILITY_FAILURE
import com.passbolt.mobile.android.feature.home.screen.SnackbarSuccessType.RESOURCE_AVAILABLE_OFFLINE
import com.passbolt.mobile.android.feature.home.screen.SnackbarSuccessType.RESOURCE_CREATED
import com.passbolt.mobile.android.feature.home.screen.SnackbarSuccessType.RESOURCE_DELETED
import com.passbolt.mobile.android.feature.home.screen.SnackbarSuccessType.RESOURCE_EDITED
import com.passbolt.mobile.android.feature.home.screen.SnackbarSuccessType.RESOURCE_MARKED_OFFLINE_NOT_CACHED
import com.passbolt.mobile.android.feature.home.screen.SnackbarSuccessType.RESOURCE_OFFLINE_AVAILABILITY_REMOVED
import com.passbolt.mobile.android.feature.home.screen.SnackbarSuccessType.RESOURCE_SHARED
import com.passbolt.mobile.android.feature.home.screen.ToastType.WAIT_FOR_DATA_REFRESH_FINISH
import com.passbolt.mobile.android.feature.home.screen.data.HomeDataProvider
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.autofillSlugs
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.homeSlugs
import com.passbolt.mobile.android.ui.Folder.Child
import com.passbolt.mobile.android.ui.Folder.Root
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Folders
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Groups
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Tags
import com.passbolt.mobile.android.ui.LeadingContentType.PASSWORD
import com.passbolt.mobile.android.ui.LeadingContentType.PIN_CODE
import com.passbolt.mobile.android.ui.LeadingContentType.STANDALONE_NOTE
import com.passbolt.mobile.android.ui.LeadingContentType.TOTP
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.FavouriteOption
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.OfflineOption
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import timber.log.Timber

@Suppress("TooManyFunctions")
internal class HomeViewModel(
    private val coroutineLaunchContext: CoroutineLaunchContext,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val getHomeDisplayViewPreferencesUseCase: GetHomeDisplayViewPreferencesUseCase,
    private val homeDataProvider: HomeDataProvider,
    private val getLocalFolderUseCase: GetLocalFolderDetailsUseCase,
    private val resourceAccessInteractor: ResourceAccessInteractor,
    private val detectAutofillConflict: DetectAutofillConflict,
    private val accountSwitchFlow: AccountSwitchFlow,
    private val userProfileInteractor: UserProfileInteractor,
    private val userProfileRefreshTrackingFlow: UserProfileRefreshTrackingFlow,
    private val markResourceOfflineUseCase: MarkResourceOfflineUseCase,
    private val unmarkResourceOfflineUseCase: UnmarkResourceOfflineUseCase,
    private val getOfflineCacheStatusUseCase: GetOfflineCacheStatusUseCase,
    private val offlineSessionState: OfflineSessionState,
) : SideEffectViewModel<HomeState, HomeSideEffect>(HomeState()),
    KoinComponent {
    private val resourcePropertiesActionsInteractor: ResourcePropertiesActionsInteractor
        get() = get { parametersOf(requireNotNull(viewState.value.moreMenuResource)) }
    private val secretPropertiesActionsInteractor: SecretPropertiesActionsInteractor
        get() = get { parametersOf(requireNotNull(viewState.value.moreMenuResource)) }
    private val resourceCommonActionsInteractor: ResourceCommonActionsInteractor
        get() = get { parametersOf(requireNotNull(viewState.value.moreMenuResource)) }

    private var dataRefreshJob: Job? = null
    private var accountSwitchJob: Job? = null
    private var offlineSessionJob: Job? = null
    private var lastInitializeIntent: Initialize? = null
    private var loadedAccountId: String? = null

    init {
        loadUserAvatar()
        refreshUserProfile()
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

    private fun refreshUserProfile() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            userProfileRefreshTrackingFlow.setRefreshing(true)
            try {
                when (
                    val result =
                        runAuthenticatedOperation {
                            userProfileInteractor.fetchAndUpdateUserProfile()
                        }
                ) {
                    is Success -> loadUserAvatar()
                    is Failure -> emitSideEffect(ShowErrorSnackbar(PROFILE_FETCH_FAILURE, result.message))
                }
            } finally {
                userProfileRefreshTrackingFlow.setRefreshing(false)
            }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    fun onIntent(intent: HomeIntent) {
        when (intent) {
            CloseCreateResourceMenu -> updateViewState { copy(showCreateResourceBottomSheet = false) }
            CloseDeleteConfirmationDialog -> updateViewState { copy(showDeleteResourceConfirmationDialog = false) }
            CloseSwitchAccount -> updateViewState { copy(showAccountSwitchBottomSheet = false) }
            OpenCreateResourceMenu -> updateViewState { copy(showCreateResourceBottomSheet = true) }
            DeleteResource -> updateViewState { copy(showDeleteResourceConfirmationDialog = true) }
            OpenFiltersBottomSheet -> updateViewState { copy(showFiltersBottomSheet = true) }
            CloseFiltersBottomSheet -> updateViewState { copy(showFiltersBottomSheet = false) }
            CloseResourceMoreMenu -> updateViewState { copy(showResourceMoreBottomSheet = false) }
            OpenFolderMoreMenu -> updateViewState { copy(showFolderMoreMenuBottomSheet = true) }
            CloseFolderMoreMenu -> updateViewState { copy(showFolderMoreMenuBottomSheet = false) }
            ViewFolderDetails -> viewFolderDetails()
            ConfirmDeleteResource -> deleteResource()
            CreateNote -> createNote()
            CreatePassword -> createPassword()
            CreateTotp -> createTotp()
            CreateFolder -> createFolder()
            CreatePinCode -> createPinCode()
            is Initialize -> initialize(intent)
            OnResume -> refreshForChangedAccount()
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
            ShareResource ->
                withResourceAccess({ resourceAccessInteractor.canShareResource() }) {
                    emitSideEffect(NavigateToShare(viewState.value.requireMoreMenuResource))
                }
            is ToggleResourceFavourite -> toggleFavourite(intent.option)
            is ToggleResourceOfflineAvailability -> toggleOfflineAvailability(intent.option)
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

    private fun viewFolderDetails() {
        updateViewState { copy(showFolderMoreMenuBottomSheet = false) }
        viewState.value.currentFolderId?.let { folderId ->
            emitSideEffect(NavigateToFolderDetails(folderId))
        }
    }

    private fun openResourceMoreMenu(intent: OpenResourceMenu) {
        updateViewState {
            copy(
                moreMenuResource = intent.resourceModel,
                showResourceMoreBottomSheet = true,
            )
        }
    }

    private fun createFolder() {
        updateViewState { copy(showCreateResourceBottomSheet = false) }
        emitSideEffect(NavigateToCreateFolder(folderId = viewState.value.currentFolderId))
    }

    private fun createTotp() {
        updateViewState { copy(showCreateResourceBottomSheet = false) }
        withResourceAccess({ resourceAccessInteractor.canCreateResource(viewState.value.currentFolderId) }) {
            emitSideEffect(NavigateToCreateTotp(folderId = viewState.value.currentFolderId))
        }
    }

    private fun createPassword() {
        updateViewState { copy(showCreateResourceBottomSheet = false) }
        withResourceAccess({ resourceAccessInteractor.canCreateResource(viewState.value.currentFolderId) }) {
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
        withResourceAccess({ resourceAccessInteractor.canCreateResource(viewState.value.currentFolderId) }) {
            emitSideEffect(
                NavigateToCreateResourceForm(
                    leadingContentType = STANDALONE_NOTE,
                    folderId = viewState.value.currentFolderId,
                ),
            )
        }
    }

    private fun createPinCode() {
        updateViewState { copy(showCreateResourceBottomSheet = false) }
        withResourceAccess({ resourceAccessInteractor.canCreateResource(viewState.value.currentFolderId) }) {
            emitSideEffect(
                NavigateToCreateResourceForm(
                    leadingContentType = PIN_CODE,
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

    private fun toggleOfflineAvailability(option: OfflineOption) {
        val resource = viewState.value.requireMoreMenuResource
        viewModelScope.launch(coroutineLaunchContext.io) {
            try {
                when (option) {
                    OfflineOption.MAKE_AVAILABLE_OFFLINE -> {
                        val output =
                            markResourceOfflineUseCase.execute(
                                MarkResourceOfflineUseCase.Input(resource.resourceId, resource.modified),
                            )
                        emitSideEffect(
                            ShowSuccessSnackbar(
                                when (output) {
                                    is MarkResourceOfflineUseCase.Output.Success -> RESOURCE_AVAILABLE_OFFLINE
                                    is MarkResourceOfflineUseCase.Output.MarkedNotCached -> RESOURCE_MARKED_OFFLINE_NOT_CACHED
                                },
                            ),
                        )
                    }
                    OfflineOption.REMOVE_OFFLINE_AVAILABILITY -> {
                        unmarkResourceOfflineUseCase.execute(UnmarkResourceOfflineUseCase.Input(resource.resourceId))
                        emitSideEffect(ShowSuccessSnackbar(RESOURCE_OFFLINE_AVAILABILITY_REMOVED))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Could not change offline availability")
                emitSideEffect(ShowErrorSnackbar(TOGGLE_OFFLINE_AVAILABILITY_FAILURE))
            }
        }
    }

    private suspend fun refreshOfflineState() {
        val status = getOfflineCacheStatusUseCase.execute(Unit)
        updateViewState {
            copy(
                isOfflineSession = status.isOfflineSession,
                offlineLastSyncEpochMillis = status.lastSyncEpochMillis,
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
                action = { resourcePropertiesActionsInteractor.provideMetadataDescription() },
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
            NONE -> {
                // no-op
            }
        }
    }

    private fun searchQueryChanged(searchQuery: String) {
        val searchEndIcon = if (searchQuery.isNotBlank()) CLEAR else AVATAR
        viewModelScope.launch {
            val homeData = getHomeData(viewState.value.homeView, searchQuery, viewState.value.showSuggestedModel)
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
            val homeData = getHomeData(homeDisplay, viewState.value.searchQuery, viewState.value.showSuggestedModel)

            updateViewState {
                copy(
                    homeView = homeDisplay,
                    homeData = homeData,
                )
            }
        }
    }

    private fun initialize(intent: Initialize) {
        if (intent == lastInitializeIntent) {
            return
        }
        lastInitializeIntent = intent
        loadedAccountId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        val filterPreferences = getHomeDisplayViewPreferencesUseCase.execute(Unit)

        viewModelScope.launch {
            updateViewState { copy(appContext = intent.appContext) }
            val homeView =
                intent.homeView
                    ?: filterPreferences.userSetHomeView.toHomeDisplayViewModel(filterPreferences.lastUsedHomeView)
            val homeData = getHomeData(homeView, viewState.value.searchQuery, intent.showSuggestedModel)
            val isAutofillConflictDetected = detectAutofillConflict()
            refreshOfflineState()

            updateViewState {
                copy(
                    showSuggestedModel = intent.showSuggestedModel,
                    homeView = homeView,
                    homeData = homeData,
                    isAutofillConflictDetected = isAutofillConflictDetected,
                )
            }
            dataRefreshJob?.cancel()
            dataRefreshJob =
                viewModelScope.launch(coroutineLaunchContext.io) {
                    synchronizeWithDataRefresh()
                }
            offlineSessionJob?.cancel()
            offlineSessionJob =
                viewModelScope.launch(coroutineLaunchContext.io) {
                    // the app can enter (server vanished under a live session) or leave
                    // (server back) the offline session at any time - the banner and the
                    // create button follow immediately, not only at the next refresh
                    offlineSessionState.isOfflineSessionFlow
                        .drop(1)
                        .collect { isOffline ->
                            refreshOfflineState()
                            val showCreateResourceButton =
                                !isOffline && !viewState.value.isRefreshing && shouldShowCreateButton()
                            updateViewState { copy(canCreateResource = showCreateResourceButton) }
                        }
                }
            accountSwitchJob?.cancel()
            accountSwitchJob =
                viewModelScope.launch(coroutineLaunchContext.io) {
                    accountSwitchFlow.selectedAccountFlow
                        .drop(1)
                        .collect { switchedAccountId ->
                            loadedAccountId = switchedAccountId
                            loadUserAvatar()
                            val homeData =
                                getHomeData(
                                    viewState.value.homeView,
                                    viewState.value.searchQuery,
                                    intent.showSuggestedModel,
                                )
                            updateViewState { copy(homeData = homeData) }
                        }
                }
        }
    }

    /*
    Unlike the app's Home, the autofill flow does NOT recreate this activity when
    switching accounts: finishAffinity would destroy the pending autofill request
    this activity holds, so it is only reordered to front and
    its ViewModels survive with stale, previous-account state.
     */
    private fun refreshForChangedAccount() {
        val loadedAccount = loadedAccountId ?: return
        val selectedAccountId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        if (selectedAccountId != loadedAccount) {
            loadedAccountId = selectedAccountId
            viewModelScope.launch {
                loadUserAvatar()
                val homeData =
                    getHomeData(
                        viewState.value.homeView,
                        viewState.value.searchQuery,
                        viewState.value.showSuggestedModel,
                    )
                updateViewState { copy(homeData = homeData) }
            }
        }
    }

    private suspend fun getHomeData(
        homeView: HomeDisplayViewModel,
        searchQuery: String? = null,
        showSuggestedModel: ShowSuggestedModel,
    ) = homeDataProvider.provideData(
        searchQuery,
        homeView,
        showSuggestedModel,
        slugsForCurrentContext(),
    )

    private fun slugsForCurrentContext(): Set<String> =
        when (viewState.value.appContext) {
            AppContext.AUTOFILL -> autofillSlugs
            AppContext.APP -> homeSlugs
        }

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
                is InProgress ->
                    updateViewState {
                        copy(isRefreshing = true, refreshProgress = it.progress, canCreateResource = false)
                    }
                FinishedWithFailure -> {
                    refreshOfflineState()
                    if (!viewState.value.isOfflineSession) {
                        emitSideEffect(ShowErrorSnackbar(FAILED_TO_REFRESH_DATA))
                    }
                    updateViewState { copy(isRefreshing = false, canCreateResource = false) }
                }
                FinishedWithSuccess -> {
                    refreshOfflineState()
                    val showCreateResourceButton = shouldShowCreateButton() && !viewState.value.isOfflineSession
                    updateViewState {
                        copy(
                            isRefreshing = false,
                            canCreateResource = showCreateResourceButton,
                        )
                    }
                }
                NotCompleted -> {
                    // autofill does not perform automatic data refresh - evaluate from local data
                    if (viewState.value.appContext == AppContext.AUTOFILL) {
                        refreshOfflineState()
                        val showCreateResourceButton = shouldShowCreateButton() && !viewState.value.isOfflineSession
                        updateViewState { copy(canCreateResource = showCreateResourceButton) }
                    }
                }
            }
        }
    }

    private fun withResourceAccess(
        hasAccess: suspend () -> Boolean,
        onAllowed: () -> Unit,
    ) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            if (hasAccess()) {
                onAllowed()
            } else {
                emitSideEffect(ShowErrorSnackbar(NO_SHARED_KEY_ACCESS))
            }
        }
    }
}
