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

package com.passbolt.mobile.android.feature.otp.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.clipboard.ClipboardAccess
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.fulldatarefresh.service.DataRefreshService
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.resources.resourceicon.ResourceIconProvider
import com.passbolt.mobile.android.core.ui.compose.dialogs.ConfirmTotpDeleteAlertDialog
import com.passbolt.mobile.android.core.ui.compose.empty.EmptyResourceListState
import com.passbolt.mobile.android.core.ui.compose.fab.AddFloatingActionButton
import com.passbolt.mobile.android.core.ui.compose.progressdialog.ProgressDialog
import com.passbolt.mobile.android.core.ui.compose.scaffold.HomeScaffold
import com.passbolt.mobile.android.core.ui.compose.search.SearchInput
import com.passbolt.mobile.android.core.ui.compose.snackbar.ColoredSnackbarVisuals
import com.passbolt.mobile.android.createresourcemenu.compose.CreateResourceMenuBottomSheet
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationHandler
import com.passbolt.mobile.android.feature.home.switchaccount.compose.SwitchAccountBottomSheet
import com.passbolt.mobile.android.feature.metadatakeytrust.ui.compose.NewMetadataKeyTrustDialog
import com.passbolt.mobile.android.feature.metadatakeytrust.ui.compose.TrustedMetadataKeyDeletedDialog
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CloseCreateResourceMenu
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CloseDeleteConfirmationDialog
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CloseTrustedKeyDeletedDialog
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.ConfirmDeleteTotp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CopyOtp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CreateNote
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CreatePassword
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CreateTotp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.DeleteOtp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.EditOtp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.OpenCreateResourceMenu
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.OpenOtpMoreMenu
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.RevealOtp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.Search
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.SearchEndIconAction
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.TrustMetadataKeyDeletion
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.CopyToClipboard
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.InitiateDataRefresh
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.NavigateToCreateResourceForm
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.NavigateToCreateTotp
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.NavigateToEditResourceForm
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.ShowSuccessSnackbar
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuBottomSheet
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun OtpScreen(
    navigation: OtpNavigation,
    modifier: Modifier = Modifier,
    viewModel: OtpViewModel = koinViewModel(),
    resourceIconProvider: ResourceIconProvider = koinInject(),
    clipboardAccess: ClipboardAccess = koinInject(),
) {
    val context = LocalContext.current
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    OtpScreen(
        state = state.value,
        onIntent = viewModel::onIntent,
        resourceIconProvider = resourceIconProvider,
        snackbarHostState = snackbarHostState,
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
            NavigateToCreateTotp -> navigation.navigateToScanOtpCodeForResult()
            is NavigateToCreateResourceForm -> navigation.navigateToCreateResourceForm(it.leadingContentType)
            is NavigateToEditResourceForm -> navigation.navigateToEditResourceForm(it.resourceId, it.resourceName)
            InitiateDataRefresh -> DataRefreshService.start(context)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpScreen(
    state: OtpState,
    onIntent: (OtpIntent) -> Unit,
    resourceIconProvider: ResourceIconProvider,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    HomeScaffold(
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        appBarTitleRes = LocalizationR.string.main_menu_otp,
        appBarIconRes = CoreUiR.drawable.ic_time_lock,
        appBarSearchInput = {
            SearchInput(
                value = state.searchQuery,
                onValueChange = { onIntent(Search(it)) },
                placeholder = stringResource(LocalizationR.string.otp_search),
                avatarUrl = state.userAvatar,
                endIconMode = state.searchInputEndIconMode,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp),
            ) { onIntent(SearchEndIconAction) }
        },
        floatingActionButton = {
            if (!state.isRefreshing) {
                AddFloatingActionButton(onClick = { onIntent(OpenCreateResourceMenu) })
            }
        },
        { paddingValues ->
            val context = LocalContext.current
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { DataRefreshService.start(context) },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
            ) {
                if (state.shouldShowEmptyState) {
                    EmptyResourceListState(title = stringResource(LocalizationR.string.no_otps))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp),
                    ) {
                        items(state.uiOtps) { otpItem ->
                            OtpItem(
                                otpItem = otpItem,
                                resourceIconProvider = resourceIconProvider,
                                onItemClick = { onIntent(RevealOtp(otpItem)) },
                                onMoreClick = { onIntent(OpenOtpMoreMenu(otpItem)) },
                            )
                        }
                    }
                }

                if (state.showCreateResourceBottomSheet) {
                    CreateResourceMenuBottomSheet(
                        onCreatePassword = { onIntent(CreatePassword) },
                        onCreateTotp = { onIntent(CreateTotp) },
                        onCreateNote = { onIntent(CreateNote) },
                        onDismissRequest = { onIntent(CloseCreateResourceMenu) },
                    )
                }

                if (state.showOtpMoreBottomSheet) {
                    val moreMenuResource = requireNotNull(state.moreMenuResource)
                    OtpMoreMenuBottomSheet(
                        resourceId = moreMenuResource.resource.resourceId,
                        resourceName = moreMenuResource.resource.metadataJsonModel.name,
                        onDismissRequest = { onIntent(OtpIntent.CloseOtpMoreMenu) },
                        onShowOtp = { onIntent(RevealOtp(moreMenuResource)) },
                        onCopyOtp = { onIntent(CopyOtp(moreMenuResource)) },
                        onEditOtp = { onIntent(EditOtp(moreMenuResource)) },
                        onDeleteOtp = { onIntent(DeleteOtp(moreMenuResource)) },
                    )
                }

                ConfirmTotpDeleteAlertDialog(
                    isVisible = state.showDeleteTotpConfirmationDialog,
                    onConfirm = { onIntent(ConfirmDeleteTotp) },
                    onDismiss = { onIntent(CloseDeleteConfirmationDialog) },
                )

                if (state.showMetadataTrustedKeyDeletedDialog && state.metadataDeletedKeyModel != null) {
                    TrustedMetadataKeyDeletedDialog(
                        trustedKeyDeletedModel = state.metadataDeletedKeyModel,
                        onDismiss = { onIntent(CloseTrustedKeyDeletedDialog) },
                        onTrustClick = { onIntent(TrustMetadataKeyDeletion) },
                    )
                }

                if (state.showNewMetadataTrustDialog && state.newMetadataKeyTrustModel != null) {
                    NewMetadataKeyTrustDialog(
                        newKeyToTrustModel = state.newMetadataKeyTrustModel,
                        onTrustClick = { onIntent(OtpIntent.TrustNewMetadataKey(state.newMetadataKeyTrustModel)) },
                        onDismiss = { onIntent(OtpIntent.CloseTrustNewKeyDialog) },
                    )
                }

                if (state.showAccountSwitchBottomSheet) {
                    SwitchAccountBottomSheet(
                        onDismissRequest = { onIntent(OtpIntent.CloseSwitchAccount) },
                        appContext = AppContext.APP,
                    )
                }

                ProgressDialog(state.showProgress)
            }
        },
    )
}
