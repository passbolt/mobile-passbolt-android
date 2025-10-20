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

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
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
import com.passbolt.mobile.android.core.ui.compose.dialogs.ConfirmResourceDeleteAlertDialog
import com.passbolt.mobile.android.core.ui.compose.empty.EmptyResourceListState
import com.passbolt.mobile.android.core.ui.compose.fab.AddFloatingActionButton
import com.passbolt.mobile.android.core.ui.compose.progressdialog.ProgressDialog
import com.passbolt.mobile.android.core.ui.compose.scaffold.HomeScaffold
import com.passbolt.mobile.android.core.ui.compose.search.SearchInput
import com.passbolt.mobile.android.core.ui.compose.snackbar.ColoredSnackbarVisuals
import com.passbolt.mobile.android.createresourcemenu.CreateResourceMenuBottomSheet
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationHandler
import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuBottomSheet
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CloseCreateResourceMenu
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CloseDeleteConfirmationDialog
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CloseSwitchAccount
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.ConfirmDeleteResource
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CreateFolder
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CreateNote
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CreatePassword
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CreateTotp
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.Initialize
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.OpenCreateResourceMenu
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.OpenFiltersBottomSheet
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.Search
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.SearchEndIconAction
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
import com.passbolt.mobile.android.feature.home.screen.ShowSuggestedModel.DoNotShow
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountBottomSheet
import com.passbolt.mobile.android.ui.FiltersMenuModel
import com.passbolt.mobile.android.ui.Folder.Child
import com.passbolt.mobile.android.ui.Folder.Root
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
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
@Suppress("ktlint:compose:vm-forwarding-check", "ViewModelForwarding")
internal fun HomeScreen(
    navigation: HomeNavigation,
    showSuggestedModel: ShowSuggestedModel,
    homeView: HomeDisplayViewModel,
    modifier: Modifier = Modifier,
    clipboardAccess: ClipboardAccess = koinInject(),
    navigator: AppNavigator = koinInject(),
    viewModel: HomeViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(homeView, showSuggestedModel) {
        viewModel.onIntent(
            Initialize(
                homeView = homeView,
                showSuggestedModel = DoNotShow,
            ),
        )
    }

    HomeScreen(
        state = state.value,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        homeNavigation = navigation,
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
            is NavigateToCreateTotp -> navigation.navigateToScanOtpCodeForResult(it.folderId)
            is NavigateToCreateResourceForm -> navigation.navigateToCreateResourceForm(it.leadingContentType, it.folderId)
            is NavigateToEditResourceForm ->
                navigation.navigateToEditResourceForm(
                    it.resourceModel.resourceId,
                    it.resourceModel.metadataJsonModel.name,
                )
            InitiateDataRefresh -> DataRefreshService.start(context)
            is OpenResourceMoreMenu -> navigation.openResourceMoreMenu(it.resourceId, it.resourceName)
            is NavigateToResourceUri -> navigator.openExternalWebsite(context, it.url)
            is NavigateToShare -> navigation.navigateToShare(it.resourceModel.resourceId)
            is NavigateToCreateFolder -> navigation.navigateToCreateFolder(it.folderId)
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
    homeNavigation: HomeNavigation,
    modifier: Modifier = Modifier,
) {
    val activity = LocalActivity.current
    val context = LocalContext.current

    HomeScaffold(
        snackbarHostState = snackbarHostState,
        modifier =
            modifier
                .testTag("home_screen"),
        appBarTitle = getAppBarTitle(context, state),
        appBarIconRes = getAppBarIconResId(state),
        shouldShowMoreIcon = homeNavigation.resourceHandlingStrategy.shouldShowFolderMoreMenu() && state.showMoreMenu,
        onMoreClick = { homeNavigation.openFolderMoreMenu(state.homeView) },
        shouldShowBackIcon = state.showBackIcon,
        onBackClick = { homeNavigation.navigateBack() },
        shouldShowCloseIcon = homeNavigation.resourceHandlingStrategy.shouldShowCloseButton(),
        onCloseClick = { activity?.finish() },
        appBarSearchInput = {
            SearchInput(
                value = state.searchQuery,
                onValueChange = { onIntent(Search(it)) },
                placeholder = stringResource(LocalizationR.string.all_items_home_search_hint),
                avatarUrl = state.userAvatar,
                endIconMode = state.searchInputEndIconMode,
                leadingIcon = {
                    Image(
                        painter = painterResource(CoreUiR.drawable.ic_filter),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .clickable { onIntent(OpenFiltersBottomSheet) }
                                .testTag("home_search_filter"),
                    )
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp)
                        .testTag("home_search_input"),
                onEndIconClick = { onIntent(SearchEndIconAction) },
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
                if (state.shouldShowEmptyState) {
                    EmptyResourceListState(title = stringResource(LocalizationR.string.no_passwords))
                } else {
                    HomeResourceList(state, homeNavigation, onIntent)
                }
            }

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
                    onDismissRequest = { onIntent(HomeIntent.CloseFiltersBottomSheet) },
                    onHomeViewChange = { homeNavigation.navigateToRoot(it) },
                    filtersMenuModel = FiltersMenuModel(state.homeView),
                )
            }

            ProgressDialog(state.showProgress)
        },
    )
}

@Suppress("CyclomaticComplexMethod")
private fun getAppBarTitle(
    context: Context,
    state: HomeState,
): String =
    when (state.homeView) {
        is Folders ->
            when (state.homeView.activeFolder) {
                is Child -> state.homeView.activeFolderName.orEmpty()
                is Root -> context.getString(LocalizationR.string.filters_menu_folders)
            }
        is Groups ->
            if (state.homeView.activeGroupId == null) {
                context.getString(LocalizationR.string.filters_menu_groups)
            } else {
                state.homeView.activeGroupName.orEmpty()
            }
        is Tags ->
            if (state.homeView.activeTagId == null) {
                context.getString(LocalizationR.string.filters_menu_tags)
            } else {
                state.homeView.activeTagName.orEmpty()
            }
        AllItems -> context.getString(LocalizationR.string.filters_menu_all_items)
        Expiry -> context.getString(LocalizationR.string.filters_menu_expiry)
        Favourites -> context.getString(LocalizationR.string.filters_menu_favourites)
        OwnedByMe -> context.getString(LocalizationR.string.filters_menu_owned_by_me)
        RecentlyModified -> context.getString(LocalizationR.string.filters_menu_recently_modified)
        SharedWithMe -> context.getString(LocalizationR.string.filters_menu_shared_with_me)
        HomeDisplayViewModel.NotLoaded -> context.getString(LocalizationR.string.filters_menu_loading)
    }

@DrawableRes
private fun getAppBarIconResId(state: HomeState): Int =
    when (state.homeView) {
        AllItems -> CoreUiR.drawable.ic_list
        Expiry -> CoreUiR.drawable.ic_calendar_clock
        Favourites -> CoreUiR.drawable.ic_star
        is Folders -> if (state.homeView.isActiveFolderShared == true) CoreUiR.drawable.ic_shared_folder else CoreUiR.drawable.ic_folder
        is Groups -> CoreUiR.drawable.ic_group
        OwnedByMe -> CoreUiR.drawable.ic_person
        RecentlyModified -> CoreUiR.drawable.ic_clock
        SharedWithMe -> CoreUiR.drawable.ic_share
        is Tags -> if (state.homeView.isActiveTagShared == true) CoreUiR.drawable.ic_shared_tag else CoreUiR.drawable.ic_tag
        HomeDisplayViewModel.NotLoaded -> CoreUiR.drawable.ic_password_generate
    }
