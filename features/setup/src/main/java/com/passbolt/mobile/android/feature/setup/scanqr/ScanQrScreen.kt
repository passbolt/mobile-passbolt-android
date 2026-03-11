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

package com.passbolt.mobile.android.feature.setup.scanqr

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.keys.LogsNavigationKey.Logs
import com.passbolt.mobile.android.core.navigation.compose.keys.SetupNavigationKey.ImportProfile
import com.passbolt.mobile.android.core.navigation.compose.keys.SetupNavigationKey.Summary
import com.passbolt.mobile.android.core.qrscan.SCAN_MANAGER_SCOPE
import com.passbolt.mobile.android.core.qrscan.manager.ScanManager
import com.passbolt.mobile.android.core.security.flagsecure.FlagSecureEffect
import com.passbolt.mobile.android.core.ui.compose.dialogs.ServerNotReachableDialog
import com.passbolt.mobile.android.core.ui.compose.dialogs.SetupExitConfirmationDialog
import com.passbolt.mobile.android.core.ui.compose.progressdialog.ProgressDialog
import com.passbolt.mobile.android.core.ui.compose.progresstoolbar.ProgressToolbar
import com.passbolt.mobile.android.feature.setup.AccountSetupDataHolder
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.AccessLogs
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.ConfirmSetupLeave
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.DismissHelpMenu
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.DismissServerNotReachable
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.DismissSetupLeave
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.GoBack
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.ImportProfileManually
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.Initialize
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.OpenHelpMenu
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.SelectedAccountKit
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrSideEffect.NavigateToImportProfile
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrSideEffect.NavigateToLogs
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrSideEffect.NavigateToSummary
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrSideEffect.ShowToast
import com.passbolt.mobile.android.helpmenu.HelpMenuBottomSheet
import com.passbolt.mobile.android.ui.HelpMenuModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.compose.scope.KoinScope
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.qualifier.named
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@OptIn(KoinExperimentalAPI::class)
@Composable
internal fun ScanQrScreen(
    modifier: Modifier = Modifier,
    viewModel: ScanQrViewModel = koinViewModel(),
    navigation: AppNavigator = koinInject(),
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current

    FlagSecureEffect()

    KoinScope(
        scopeID = SCAN_MANAGER_SCOPE,
        scopeQualifier = named(SCAN_MANAGER_SCOPE),
    ) {
        val scanManager: ScanManager = koinInject()

        LaunchedEffect(scanManager) {
            viewModel.onIntent(
                Initialize(
                    scanManager.barcodeScanPublisher,
                    accountSetupDataModel = (activity as? AccountSetupDataHolder)?.bundledAccountSetupData,
                ),
            )
        }

        DisposableEffect(scanManager) {
            onDispose {
                scanManager.detach()
            }
        }

        ScanQrScreen(
            modifier = modifier,
            state = state,
            onIntent = viewModel::onIntent,
            scanManager = scanManager,
            lifecycleOwner = lifecycleOwner,
        )
    }

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            NavigateBack -> navigation.navigateBack()
            is NavigateToSummary -> navigation.navigateToKey(Summary(sideEffect.status))
            NavigateToImportProfile -> navigation.navigateToKey(ImportProfile)
            NavigateToLogs -> navigation.navigateToKey(Logs)
            is ShowToast -> Toast.makeText(context, getToastMessage(context, sideEffect.type), Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
private fun ScanQrScreen(
    state: ScanQrState,
    onIntent: (ScanQrIntent) -> Unit,
    scanManager: ScanManager,
    lifecycleOwner: LifecycleOwner,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        onIntent(GoBack)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ProgressToolbar(
                progress = state.scanProgress,
                onBackClick = { onIntent(GoBack) },
                endIcon = CoreUiR.drawable.ic_help,
                onEndIconClick = { onIntent(OpenHelpMenu) },
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            // TODO: migrate to the new CameraX preview composable as next step
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        try {
                            scanManager.attach(lifecycleOwner, this)
                        } catch (exception: Exception) {
                            onIntent(ScanQrIntent.StartCameraError(exception))
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            Text(
                text = getTooltipMessage(state.tooltipMessage, state.scanErrorMessage),
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 64.dp)
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(4.dp),
                        ).padding(8.dp),
                color = Color.Black,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    SetupExitConfirmationDialog(
        isVisible = state.showSetupLeaveConfirmationDialog,
        onStopSetup = { onIntent(ConfirmSetupLeave) },
        onDismiss = { onIntent(DismissSetupLeave) },
    )

    ServerNotReachableDialog(
        isVisible = state.showServerNotReachableDialog,
        domain = state.serverDomain,
        onDismiss = { onIntent(DismissServerNotReachable) },
    )

    if (state.showHelpMenu) {
        HelpMenuBottomSheet(
            HelpMenuModel(
                shouldShowShowQrCodesHelp = true,
                shouldShowImportProfile = true,
                shouldShowImportAccountKit = true,
            ),
            onDismissRequest = { onIntent(DismissHelpMenu) },
            onImportProfileManually = { onIntent(ImportProfileManually) },
            onAccessLogs = { onIntent(AccessLogs) },
            onAccountKitSelect = { onIntent(SelectedAccountKit(it)) },
        )
    }

    ProgressDialog(isVisible = state.showProgress)
}
