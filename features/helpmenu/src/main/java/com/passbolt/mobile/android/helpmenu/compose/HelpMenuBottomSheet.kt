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

package com.passbolt.mobile.android.helpmenu.compose

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.compose.bottomsheet.BottomSheetHeader
import com.passbolt.mobile.android.core.ui.compose.dialogs.QrCodesInformationDialog
import com.passbolt.mobile.android.core.ui.compose.menu.OpenableSettingsItem
import com.passbolt.mobile.android.core.ui.compose.menu.SwitchableSettingsItem
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetIntent.AccessLogs
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetIntent.AccountKitRead
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetIntent.Close
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetIntent.DismissQrCodesDialog
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetIntent.ImportAccountKit
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetIntent.ImportProfileManually
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetIntent.Initialize
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetIntent.SeeWhyQrCodesExplanation
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetIntent.ToggleEnableLogs
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetIntent.VisitHelpWebsite
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetSideEffect.Dismiss
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetSideEffect.NavigateToAccessLogs
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetSideEffect.NavigateToImportAccountKit
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetSideEffect.NavigateToImportProfileManually
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetSideEffect.NotifyAccountKitRead
import com.passbolt.mobile.android.helpmenu.compose.HelpMenuBottomSheetSideEffect.OpenHelpWebsite
import com.passbolt.mobile.android.ui.HelpMenuModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
fun HelpMenuBottomSheet(
    helpMenuModel: HelpMenuModel,
    onDismissRequest: () -> Unit,
    onImportProfileManually: () -> Unit = {},
    onAccountKitSelect: (String) -> Unit = {},
    onAccessLogs: () -> Unit = {},
    navigator: AppNavigator = koinInject(),
    viewModel: HelpMenuBottomSheetViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.viewState.collectAsState()

    // TODO consider passing URI + additional reader class that provides content resolver
    // TODO to move reader of the UI
    val accountKitFileChosenResult =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            uri?.let {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val accountKit = inputStream.reader().readText()
                    viewModel.onIntent(AccountKitRead(accountKit))
                }
            }
        }

    LaunchedEffect(helpMenuModel) {
        viewModel.onIntent(Initialize(helpMenuModel))
    }

    HelpMenuBottomSheet(
        state = state,
        onIntent = viewModel::onIntent,
    )

    QrCodesInformationDialog(
        isVisible = state.showWhyQrCodesExplanationDialog,
        onDismiss = { viewModel.onIntent(DismissQrCodesDialog) },
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            Dismiss -> onDismissRequest()
            OpenHelpWebsite ->
                navigator.openExternalWebsite(
                    context = context,
                    url = context.getString(LocalizationR.string.help_website),
                )
            NavigateToImportProfileManually -> {
                onImportProfileManually()
            }
            NavigateToImportAccountKit -> {
                accountKitFileChosenResult.launch(arrayOf("*/*"))
            }
            NavigateToAccessLogs -> {
                onAccessLogs()
            }
            is NotifyAccountKitRead -> {
                onAccountKitSelect(it.accountKit)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HelpMenuBottomSheet(
    state: HelpMenuBottomSheetState,
    onIntent: (HelpMenuBottomSheetIntent) -> Unit,
) {
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )

    ModalBottomSheet(
        onDismissRequest = { onIntent(Close) },
        containerColor = colorResource(R.color.elevated_background),
        sheetState = sheetState,
    ) {
        Column {
            BottomSheetHeader(
                title = stringResource(LocalizationR.string.help_menu_help),
                onClose = { onIntent(Close) },
            )

            if (state.showScanQrCodesHelp) {
                OpenableSettingsItem(
                    title = stringResource(LocalizationR.string.help_menu_why_scan_codes),
                    iconPainter = painterResource(CoreUiR.drawable.ic_camera),
                    onClick = { onIntent(SeeWhyQrCodesExplanation) },
                    opensInternally = true,
                )
            }

            SwitchableSettingsItem(
                title = stringResource(LocalizationR.string.help_menu_enable_debug_logs),
                iconPainter = painterResource(CoreUiR.drawable.ic_bug),
                isChecked = state.enableLogsSwitch,
                onCheckedChange = { onIntent(ToggleEnableLogs(it)) },
            )

            OpenableSettingsItem(
                title = stringResource(LocalizationR.string.help_menu_access_logs),
                iconPainter = painterResource(CoreUiR.drawable.ic_bug),
                onClick = { onIntent(AccessLogs) },
                opensInternally = true,
                isEnabled = state.accessLogsEnabled,
            )

            if (state.showImportProfileHelp) {
                OpenableSettingsItem(
                    title = stringResource(LocalizationR.string.help_menu_import_profile_manually),
                    iconPainter = painterResource(CoreUiR.drawable.ic_import_profile),
                    onClick = { onIntent(ImportProfileManually) },
                    opensInternally = true,
                )
            }

            if (state.showImportAccountKitHelp) {
                OpenableSettingsItem(
                    title = stringResource(LocalizationR.string.help_menu_import_account_kit),
                    iconPainter = painterResource(CoreUiR.drawable.ic_import_file),
                    onClick = { onIntent(ImportAccountKit) },
                    opensInternally = true,
                )
            }

            OpenableSettingsItem(
                title = stringResource(LocalizationR.string.help_menu_visit_help_website),
                iconPainter = painterResource(CoreUiR.drawable.ic_link),
                onClick = { onIntent(VisitHelpWebsite) },
                opensInternally = false,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HelpMenuBottomSheetPreview() {
    HelpMenuBottomSheet(
        state =
            HelpMenuBottomSheetState(
                showScanQrCodesHelp = true,
                showImportProfileHelp = true,
                showImportAccountKitHelp = true,
                enableLogsSwitch = true,
                accessLogsEnabled = true,
            ),
    ) {}
}
