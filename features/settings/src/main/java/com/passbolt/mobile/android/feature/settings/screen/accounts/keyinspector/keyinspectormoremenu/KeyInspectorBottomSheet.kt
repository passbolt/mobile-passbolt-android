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

package com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration.Short
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.ActivityIntents.AuthConfig.RefreshPassphrase
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.ui.compose.menu.OpenableSettingsItem
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetIntent.Close
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetIntent.ExportPrivateKey
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetIntent.ExportPublicKey
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetIntent.RefreshedPassphrase
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetSideEffect.ConfirmPassphrase
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetSideEffect.Dismiss
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetSideEffect.ErrorSnackbarType.FAILED_TO_GENERATE_PUBLIC_KEY
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetSideEffect.ShowTextShareSheet
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun KeyInspectorBottomSheet(
    onDismissRequest: () -> Unit,
    navigator: AppNavigator = koinInject(),
    viewModel: KeyInspectorBottomSheetViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val authenticationLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.onIntent(RefreshedPassphrase)
            }
        }

    KeyInspectorBottomSheet(
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
        onDismissRequest = onDismissRequest,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            is ShowErrorSnackbar ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(getSnackbarMessage(context, it), duration = Short)
                }
            ConfirmPassphrase ->
                authenticationLauncher.launch(
                    ActivityIntents.authentication(
                        context,
                        RefreshPassphrase,
                    ),
                )
            Dismiss -> onDismissRequest()
            is ShowTextShareSheet ->
                navigator.startTextShareSheet(
                    context = context,
                    text = it.text,
                    shareSheetTitle = context.getString(LocalizationR.string.key_inspector_menu_export_key),
                )
        }
    }
}

private fun getSnackbarMessage(
    context: Context,
    snackbar: ShowErrorSnackbar,
): String =
    when (snackbar.type) {
        FAILED_TO_GENERATE_PUBLIC_KEY ->
            context.getString(
                LocalizationR.string.key_inspector_menu_failed_to_generate_public_key,
                snackbar.errorMessage.orEmpty(),
            )
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeyInspectorBottomSheet(
    snackbarHostState: SnackbarHostState,
    onIntent: (KeyInspectorBottomSheetIntent) -> Unit,
    onDismissRequest: () -> Unit = {},
) {
    Box {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = stringResource(LocalizationR.string.key_inspector_menu_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier =
                        Modifier
                            .weight(1f)
                            .align(Alignment.CenterVertically),
                    color = colorResource(id = CoreUiR.color.text_primary),
                )

                Icon(
                    painter = painterResource(id = CoreUiR.drawable.ic_close),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(24.dp)
                            .clickable { onIntent(Close) },
                    tint = colorResource(id = CoreUiR.color.icon_tint),
                )
            }

            HorizontalDivider(
                color = colorResource(id = CoreUiR.color.divider),
                modifier =
                    Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp),
                thickness = 1.dp,
            )

            OpenableSettingsItem(
                title = stringResource(LocalizationR.string.key_inspector_menu_export_private_key),
                iconPainter = painterResource(CoreUiR.drawable.ic_export),
                onClick = { onIntent(ExportPrivateKey) },
                opensInternally = false,
            )
            OpenableSettingsItem(
                title = stringResource(LocalizationR.string.key_inspector_menu_export_public_key),
                iconPainter = painterResource(CoreUiR.drawable.ic_export),
                onClick = { onIntent(ExportPublicKey) },
                opensInternally = false,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 16.dp),
            snackbar = { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = colorResource(CoreUiR.color.red),
                    contentColor = colorResource(CoreUiR.color.white),
                )
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun KeyInspectorPreview() {
    KeyInspectorBottomSheet(
        snackbarHostState = SnackbarHostState(),
        onIntent = {},
    )
}
