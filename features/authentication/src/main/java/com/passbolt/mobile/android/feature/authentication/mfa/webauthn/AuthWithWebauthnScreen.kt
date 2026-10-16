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

package com.passbolt.mobile.android.feature.authentication.mfa.webauthn

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.PassboltTheme
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.AuthenticationSignIn
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.Start
import com.passbolt.mobile.android.core.ui.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.dialogs.LeaveSetupAlertDialog
import com.passbolt.mobile.android.core.ui.progressdialog.ProgressDialog
import com.passbolt.mobile.android.core.ui.snackbar.ColoredSnackbarVisuals
import com.passbolt.mobile.android.core.ui.switch.TextSwitch
import com.passbolt.mobile.android.feature.authentication.mfa.MfaDialogState
import com.passbolt.mobile.android.feature.authentication.mfa.MfaResult
import com.passbolt.mobile.android.feature.authentication.mfa.MfaResult.OtherProvider
import com.passbolt.mobile.android.feature.authentication.mfa.MfaResult.Succeeded
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.AuthenticateWithWebauthn
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.ChooseOtherProvider
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.Close
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.ConfirmSetupLeave
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.DismissSetupLeave
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.ToggleRememberMe
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.WebauthnCeremonyFinished
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.CloseAndNavigateToStartup
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.LaunchWebauthnCeremony
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.NavigateToLogin
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.NotifyOtherProviderClicked
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.NotifyVerificationSucceeded
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.ShowErrorSnackbar
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun AuthWithWebauthnScreen(
    mfaState: MfaDialogState.Webauthn,
    onMfaResult: (MfaResult) -> Unit,
    isSetupFlow: Boolean = false,
    appNavigator: AppNavigator = koinInject(),
    viewModel: AuthWithWebauthnViewModel =
        koinViewModel {
            parametersOf(mfaState.userId, mfaState.authToken, mfaState.hasOtherProviders, isSetupFlow)
        },
) {
    val context = LocalContext.current
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val ceremonyLauncher =
        rememberLauncherForActivityResult(WebauthnCeremonyResultContract()) { result ->
            viewModel.onIntent(WebauthnCeremonyFinished(result))
        }

    BackHandler { viewModel.onIntent(Close) }

    val errorSnackbarColor = colorResource(CoreUiR.color.red)
    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is LaunchWebauthnCeremony -> ceremonyLauncher.launch(sideEffect.ceremonyUrl)
            is NotifyVerificationSucceeded -> onMfaResult(Succeeded(sideEffect.mfaHeader))
            is NotifyOtherProviderClicked -> onMfaResult(OtherProvider(sideEffect.bearer, MfaProvider.WEBAUTHN))
            is CloseAndNavigateToStartup -> appNavigator.startNavigationActivity(context, Start)
            is NavigateToLogin -> appNavigator.startNavigationActivity(context, AuthenticationSignIn)
            is ShowErrorSnackbar -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        ColoredSnackbarVisuals(
                            message = getSnackbarMessage(context, sideEffect.kind),
                            backgroundColor = errorSnackbarColor,
                        ),
                    )
                }
            }
        }
    }

    AuthWithWebauthnScreen(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
internal fun AuthWithWebauthnScreen(
    state: AuthWithWebauthnState,
    onIntent: (AuthWithWebauthnIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    val customVisuals = data.visuals as? ColoredSnackbarVisuals
                    if (customVisuals != null) {
                        Snackbar(
                            snackbarData = data,
                            containerColor = customVisuals.backgroundColor,
                            contentColor = customVisuals.contentColor,
                        )
                    } else {
                        Snackbar(snackbarData = data)
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconButton(
                onClick = { onIntent(Close) },
                modifier =
                    Modifier
                        .align(Alignment.End)
                        .padding(top = 16.dp, end = 16.dp),
            ) {
                Image(
                    painter = painterResource(CoreUiR.drawable.ic_close),
                    contentDescription = null,
                )
            }

            Text(
                text = stringResource(LocalizationR.string.dialog_mfa_mfa),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Image(
                painter = painterResource(CoreUiR.drawable.ic_passkey_mfa),
                contentDescription = null,
                modifier = Modifier.size(116.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(LocalizationR.string.dialog_mfa_webauthn),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            TextSwitch(
                text = stringResource(LocalizationR.string.dialog_mfa_remember_me),
                isChecked = state.rememberMe,
                onCheckedChange = { onIntent(ToggleRememberMe(it)) },
            )

            Spacer(modifier = Modifier.height(8.dp))

            PrimaryButton(
                text = stringResource(LocalizationR.string.dialog_mfa_auth_with_webauthn),
                onClick = { onIntent(AuthenticateWithWebauthn) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            if (state.hasOtherProvider) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { onIntent(ChooseOtherProvider) },
                    modifier = Modifier.padding(bottom = 24.dp),
                ) {
                    Text(
                        text = stringResource(LocalizationR.string.dialog_mfa_other_provider),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }

    LeaveSetupAlertDialog(
        isVisible = state.showSetupLeaveConfirmationDialog,
        onLeaveConfirm = { onIntent(ConfirmSetupLeave) },
        onDismiss = { onIntent(DismissSetupLeave) },
    )

    ProgressDialog(isVisible = state.showProgress)
}

@Preview(showBackground = true)
@Composable
private fun AuthWithWebauthnScreenPreview() {
    PassboltTheme {
        AuthWithWebauthnScreen(
            state = AuthWithWebauthnState(hasOtherProvider = true),
            onIntent = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
