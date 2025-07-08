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

package com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.ui.compose.circularimage.CircularProfileImage
import com.passbolt.mobile.android.core.ui.compose.labelledtext.LabelledText
import com.passbolt.mobile.android.core.ui.compose.labelledtext.LabelledTextEndAction
import com.passbolt.mobile.android.core.ui.compose.progressdialog.ProgressDialog
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationHandler
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationNavigation
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorIntent.GoBack
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorIntent.OpenMoreMenu
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorScreenSideEffect.AddFingerprintToClipboard
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorScreenSideEffect.AddUidToClipboard
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorScreenSideEffect.ErrorSnackbarType.FAILED_TO_FETCH_KEY
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorScreenSideEffect.NavigateToKeyInspectorMoreMenu
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorScreenSideEffect.NavigateUp
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorScreenSideEffect.ShowErrorSnackbar
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun KeyInspectorScreen(
    navigation: KeyInspectorNavigation,
    authenticationNavigation: AuthenticationNavigation,
    modifier: Modifier = Modifier,
    viewModel: KeyInspectorViewModel = koinViewModel(),
    clipboardManager: ClipboardManager? = koinInject(),
) {
    val context = LocalContext.current
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    KeyInspectorScreen(
        modifier = modifier,
        state = state.value,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
    )

    AuthenticationHandler(
        onAuthenticatedIntent = viewModel::onAuthenticationIntent,
        authenticationSideEffect = viewModel.authenticationSideEffect,
        authenticationNavigation = authenticationNavigation,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            is AddFingerprintToClipboard -> {
                addToClipboard(
                    context = context,
                    clipboardManager = clipboardManager,
                    label = context.getString(LocalizationR.string.copy_label_fingerprint),
                    value = it.fingerprint,
                )
            }
            is AddUidToClipboard -> {
                addToClipboard(
                    context = context,
                    clipboardManager = clipboardManager,
                    label = context.getString(LocalizationR.string.copy_label_uid),
                    value = it.uid,
                )
            }
            NavigateToKeyInspectorMoreMenu -> navigation.navigateToKeyInspectorMoreMenu()
            NavigateUp -> navigation.navigateUp()
            is ShowErrorSnackbar ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(getSnackbarMessage(context, it), duration = Short)
                }
        }
    }
}

private fun getSnackbarMessage(
    context: Context,
    snackbar: ShowErrorSnackbar,
): String =
    when (snackbar.type) {
        FAILED_TO_FETCH_KEY ->
            context.getString(
                LocalizationR.string.key_inspector_error_during_key_fetch,
                snackbar.errorMessage.orEmpty(),
            )
    }

private fun addToClipboard(
    context: Context,
    clipboardManager: ClipboardManager?,
    label: String,
    value: String,
) {
    clipboardManager?.apply {
        setPrimaryClip(ClipData.newPlainText(label, value))
    }
    Toast
        .makeText(
            context,
            context.getString(LocalizationR.string.copied_info, label),
            Toast.LENGTH_SHORT,
        ).show()
}

@Composable
private fun KeyInspectorScreen(
    state: KeyInspectorState,
    snackbarHostState: SnackbarHostState,
    onIntent: (KeyInspectorIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
    ) {
        Column {
            TitleAppBar(
                title = stringResource(LocalizationR.string.settings_accounts_key_inspector),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
                actions =
                    {
                        IconButton(onClick = { onIntent(OpenMoreMenu) }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = null,
                            )
                        }
                    },
            )

            CircularProfileImage(
                state.avatarUrl,
                width = 96.dp,
                height = 96.dp,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 32.dp),
            )

            Text(
                text = state.label,
                modifier =
                    Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )

            if (state.uid.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LabelledText(
                    label = stringResource(LocalizationR.string.key_inspector_uid),
                    text = state.uid,
                    endAction =
                        LabelledTextEndAction(
                            icon = CoreUiR.drawable.ic_copy,
                            action = { onIntent(KeyInspectorIntent.CopyUid) },
                        ),
                )
            }

            if (state.fingerprint.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LabelledText(
                    label = stringResource(LocalizationR.string.key_inspector_fingerprint),
                    text = state.fingerprint,
                    useMonospaceFont = true,
                    endAction =
                        LabelledTextEndAction(
                            icon = CoreUiR.drawable.ic_copy,
                            action = { onIntent(KeyInspectorIntent.CopyFingerprint) },
                        ),
                )
            }

            if (state.created.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LabelledText(
                    label = stringResource(LocalizationR.string.key_inspector_created),
                    text = state.created,
                )
            }

            if (state.expires.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LabelledText(
                    label = stringResource(LocalizationR.string.key_inspector_expires),
                    text = state.expires,
                )
            }

            if (state.keyLength > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LabelledText(
                    label = stringResource(LocalizationR.string.key_inspector_key_length),
                    text = state.keyLength.toString(),
                )
            }

            if (state.algorithm.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LabelledText(
                    label = stringResource(LocalizationR.string.key_inspector_key_algorithm),
                    text = state.algorithm,
                )
            }
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

        ProgressDialog(isVisible = state.showProgress)
    }
}

@Preview(showBackground = true)
@Composable
private fun KeyInspectorPreview() {
    KeyInspectorScreen(
        state =
            KeyInspectorState(
                label = "Grace Hopper",
                uid = "1234567890",
                fingerprint = "ABCD1234EFGH5678IJKL9012MNOP3456QRST7890",
                created = "2023-10-01",
                expires = "2024-10-01",
                keyLength = 2048,
                algorithm = "RSA",
                showProgress = false,
            ),
        snackbarHostState = SnackbarHostState(),
        onIntent = {},
        modifier = Modifier,
    )
}
