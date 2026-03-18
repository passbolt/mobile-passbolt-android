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

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.clipboard.ClipboardAccess
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.fulldatarefresh.service.DataRefreshService
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.BottomTab
import com.passbolt.mobile.android.core.navigation.compose.keys.CreateFolderNavigationKey
import com.passbolt.mobile.android.core.navigation.compose.keys.FolderDetailsNavigationKey.FolderDetails
import com.passbolt.mobile.android.core.navigation.compose.keys.HomeNavigationKey
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.ScanOtp
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.ScanOtpMode
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.Permissions
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.MainResourceForm
import com.passbolt.mobile.android.core.navigation.compose.keys.SettingsNavigationKey.Autofill
import com.passbolt.mobile.android.core.ui.dialogs.ConfirmResourceDeleteAlertDialog
import com.passbolt.mobile.android.core.ui.fab.AddFloatingActionButton
import com.passbolt.mobile.android.core.ui.progressdialog.ProgressDialog
import com.passbolt.mobile.android.core.ui.scaffold.HomeScaffold
import com.passbolt.mobile.android.core.ui.search.SearchInput
import com.passbolt.mobile.android.core.ui.snackbar.ColoredSnackbarVisuals
import com.passbolt.mobile.android.createresourcemenu.CreateResourceMenuBottomSheet
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationHandler
import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuBottomSheet
import com.passbolt.mobile.android.feature.home.foldermoremenu.FolderMoreMenuBottomSheet
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
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CreateTotp
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.DeleteResource
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.EditResource
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.Initialize
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.LaunchResourceWebsite
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.OpenCreateResourceMenu
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.OpenFiltersBottomSheet
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.OpenFolderMoreMenu
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.Search
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.SearchEndIconAction
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.ShareResource
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.ToggleResourceFavourite
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
import com.passbolt.mobile.android.feature.home.screen.snackbar.AutofillConflictSnackbarEffect
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountBottomSheet
import com.passbolt.mobile.android.resourcemoremenu.ResourceMoreMenuBottomSheet
import com.passbolt.mobile.android.testtags.composetags.Home
import com.passbolt.mobile.android.ui.FiltersMenuModel
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Folders
import com.passbolt.mobile.android.ui.PermissionsItem
import com.passbolt.mobile.android.ui.PermissionsMode
import com.passbolt.mobile.android.ui.ResourceFormMode
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
@Suppress("ktlint:compose:vm-forwarding-check", "ViewModelForwarding")
internal fun HomeScreen(
    resourceHandlingStrategy: ResourceHandlingStrategy,
    showSuggestedModel: ShowSuggestedModel,
    homeView: HomeDisplayViewModel,
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    clipboardAccess: ClipboardAccess = koinInject(),
    viewModel: HomeViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    AutofillConflictSnackbarEffect(
        snackbarHostState = snackbarHostState,
        isAutofillConflictDetected = state.isAutofillConflictDetected,
        onActionClick = {
            with(navigator) {
                setPendingNavigation(Autofill)
                requestTabSwitch(BottomTab.SETTINGS)
            }
        },
    )

    LaunchedEffect(homeView, showSuggestedModel) {
        viewModel.onIntent(
            Initialize(
                homeView = homeView,
                showSuggestedModel = showSuggestedModel,
            ),
        )
    }

    HomeScreen(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        navigator = navigator,
        resourceHandlingStrategy = resourceHandlingStrategy,
        modifier = modifier,
    )

    AuthenticationHandler(
        onAuthenticatedIntent = viewModel::onAuthenticationIntent,
        authenticationSideEffect = viewModel.authenticationSideEffect,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            is CopyToClipboard ->
                clipboardAccess.setPrimaryClip(
                    context = context,
                    label = it.label,
                    value = it.value,
                    isSensitive = it.isSensitive,
                )
            is ShowErrorSnackbar ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        ColoredSnackbarVisuals(
                            message = getErrorMessage(context, it.type, it.message),
                            backgroundColor = Color(context.getColor(CoreUiR.color.red)),
                        ),
                    )
                }
            is ShowSuccessSnackbar ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        ColoredSnackbarVisuals(
                            message = getSuccessMessage(context, it.type, it.message),
                            backgroundColor = Color(context.getColor(CoreUiR.color.green)),
                        ),
                    )
                }
            is NavigateToCreateTotp ->
                navigator.navigateToKey(
                    ScanOtp(ScanOtpMode.SCAN_WITH_SUCCESS_SCREEN, it.folderId),
                )
            is NavigateToCreateResourceForm ->
                navigator.navigateToKey(
                    MainResourceForm(ResourceFormMode.Create(it.leadingContentType, it.folderId)),
                )
            is NavigateToEditResourceForm ->
                navigator.navigateToKey(
                    MainResourceForm(
                        ResourceFormMode.Edit(
                            resourceId = it.resourceModel.resourceId,
                            resourceName = it.resourceModel.metadataJsonModel.name,
                        ),
                    ),
                )
            InitiateDataRefresh -> DataRefreshService.start(context)
            is NavigateToResourceUri -> navigator.openExternalWebsite(context, it.url)
            is NavigateToShare ->
                navigator.navigateToKey(
                    Permissions(it.resourceModel.resourceId, PermissionsMode.EDIT, PermissionsItem.RESOURCE),
                )
            is NavigateToCreateFolder ->
                navigator.navigateToKey(
                    CreateFolderNavigationKey.CreateFolder(it.folderId),
                )
            is NavigateToFolderDetails ->
                navigator.navigateToKey(
                    FolderDetails(it.folderId),
                )
            is ShowToast -> Toast.makeText(context, getToastMessage(context, it.type), Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    navigator: AppNavigator,
    resourceHandlingStrategy: ResourceHandlingStrategy,
    modifier: Modifier = Modifier,
) {
    val activity = LocalActivity.current
    val context = LocalContext.current

    HomeScaffold(
        snackbarHostState = snackbarHostState,
        modifier =
            modifier
                .testTag(Home.SCREEN),
        appBarTitle = getAppBarTitle(context, state),
        appBarIconRes = getAppBarIconResId(state),
        shouldShowMoreIcon = resourceHandlingStrategy.shouldShowFolderMoreMenu() && state.showMoreMenu,
        onMoreClick = { onIntent(OpenFolderMoreMenu) },
        shouldShowBackIcon = state.showBackIcon,
        onBackClick = { navigator.navigateBack() },
        shouldShowCloseIcon = resourceHandlingStrategy.shouldShowCloseButton(),
        onCloseClick = { activity?.finish() },
        appBarSearchInput = {
            SearchInput(
                onValueChange = { onIntent(Search(it)) },
                placeholder = stringResource(LocalizationR.string.all_items_home_search_hint),
                endIconMode = state.searchInputEndIconMode,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp),
                avatarUrl = state.userAvatar,
                initialValue = state.searchQuery,
                onEndIconClick = { onIntent(SearchEndIconAction) },
                leadingIcon = {
                    Image(
                        painter = painterResource(CoreUiR.drawable.ic_filter),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .clickable { onIntent(OpenFiltersBottomSheet) }
                                .testTag(Home.SEARCH_FILTER),
                    )
                },
            )
        },
        floatingActionButton = {
            if (state.canCreateResource) {
                AddFloatingActionButton(onClick = { onIntent(OpenCreateResourceMenu) })
            }
        },
        content = { paddingValues ->
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { DataRefreshService.start(context) },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
            ) {
                HomeResourceList(state, navigator, resourceHandlingStrategy, onIntent)
            }
        },
    )

    if (state.showCreateResourceBottomSheet) {
        CreateResourceMenuBottomSheet(
            homeDisplayViewModel = state.homeView,
            onCreatePassword = { onIntent(CreatePassword) },
            onCreateTotp = { onIntent(CreateTotp) },
            onCreateNote = { onIntent(CreateNote) },
            onCreateFolder = { onIntent(CreateFolder) },
            onDismissRequest = { onIntent(CloseCreateResourceMenu) },
        )
    }

    ConfirmResourceDeleteAlertDialog(
        isVisible = state.showDeleteResourceConfirmationDialog,
        onConfirm = { onIntent(ConfirmDeleteResource) },
        onDismiss = { onIntent(CloseDeleteConfirmationDialog) },
    )

    if (state.showAccountSwitchBottomSheet) {
        SwitchAccountBottomSheet(
            appContext = AppContext.APP,
            onDismissRequest = { onIntent(CloseSwitchAccount) },
        )
    }

    if (state.showFiltersBottomSheet) {
        FiltersMenuBottomSheet(
            onDismissRequest = { onIntent(CloseFiltersBottomSheet) },
            onHomeViewChange = { navigator.replaceAll(HomeNavigationKey.Home(it)) },
            filtersMenuModel = FiltersMenuModel(state.homeView),
        )
    }

    if (state.showResourceMoreBottomSheet && state.moreMenuResource != null) {
        ResourceMoreMenuBottomSheet(
            resourceId = state.moreMenuResource.resourceId,
            resourceName = state.moreMenuResource.metadataJsonModel.name,
            onDismissRequest = { onIntent(CloseResourceMoreMenu) },
            onCopyPassword = { onIntent(CopyPassword) },
            onCopyMetadataDescription = { onIntent(CopyResourceMetadataDescription) },
            onCopyNote = { onIntent(CopyNote) },
            onCopyUrl = { onIntent(CopyResourceUri) },
            onCopyUsername = { onIntent(CopyResourceUsername) },
            onLaunchWebsite = { onIntent(LaunchResourceWebsite) },
            onDelete = { onIntent(DeleteResource) },
            onEdit = { onIntent(EditResource) },
            onShare = { onIntent(ShareResource) },
            onToggleFavourite = { onIntent(ToggleResourceFavourite(it)) },
        )
    }

    if (state.showFolderMoreMenuBottomSheet) {
        FolderMoreMenuBottomSheet(
            folderName = (state.homeView as? Folders)?.activeFolderName,
            onDismissRequest = { onIntent(CloseFolderMoreMenu) },
            onSeeDetails = { onIntent(ViewFolderDetails) },
        )
    }

    ProgressDialog(state.showProgress)
}
