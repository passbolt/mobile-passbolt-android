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

package com.passbolt.mobile.android.feature.setup.fingerprint

import PassboltTheme
import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.ActivityIntents.AuthConfig.Setup
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.Home
import com.passbolt.mobile.android.core.navigation.compose.keys.SettingsNavigationKey.DismissBehavior.FINISH_TO_HOME
import com.passbolt.mobile.android.core.navigation.compose.keys.SettingsNavigationKey.EncourageNativeAutofill
import com.passbolt.mobile.android.core.ui.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.dialogs.KeyChangesDetectedAlertDialog
import com.passbolt.mobile.android.feature.authentication.auth.showBiometricPrompt
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.AuthenticationSuccess
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.BiometricAuthenticationCancel
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.BiometricAuthenticationError
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.BiometricAuthenticationSuccess
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.ConfirmKeyPermanentlyInvalidated
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.DismissKeyPermanentlyInvalidated
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.KeyPermanentlyInvalidated
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.MaybeLater
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.ResumeView
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.UseFingerprint
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupSideEffect.NavigateToAppSystemSettings
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupSideEffect.NavigateToEncourageAutofill
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupSideEffect.NavigateToHome
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupSideEffect.ShowBiometricPrompt
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupSideEffect.StartAuthActivity
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.util.concurrent.Executor
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
fun FingerprintSetupScreen(
    modifier: Modifier = Modifier,
    viewModel: FingerprintSetupViewModel = koinViewModel(),
    navigator: AppNavigator = koinInject(),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val environment = rememberFingerprintSetupEnvironment(viewModel::onIntent)

    LaunchedEffect(Unit) {
        viewModel.onIntent(ResumeView)
    }

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is ShowBiometricPrompt ->
                showBiometricPrompt(
                    activity = environment.context as AppCompatActivity,
                    executor = environment.executor,
                    biometricPromptBuilder = environment.biometricPromptBuilder,
                    fingerprintEncryptionCipher = sideEffect.cipher,
                    onAuthenticationSuccess = { cipher ->
                        viewModel.onIntent(BiometricAuthenticationSuccess(cipher))
                    },
                    onAuthenticationCancelled = {
                        viewModel.onIntent(BiometricAuthenticationCancel)
                    },
                    onAuthenticationError = { error ->
                        viewModel.onIntent(BiometricAuthenticationError(error))
                    },
                    onKeyPermanentlyInvalidated = { exception ->
                        viewModel.onIntent(KeyPermanentlyInvalidated(exception))
                    },
                )
            NavigateToAppSystemSettings -> navigator.openAppOsSettings(context)
            StartAuthActivity ->
                environment.authenticationLauncher.launch(
                    ActivityIntents.authentication(
                        environment.context,
                        Setup,
                    ),
                )
            NavigateToEncourageAutofill ->
                navigator.navigateToKey(EncourageNativeAutofill(dismissBehavior = FINISH_TO_HOME))
            NavigateToHome -> {
                navigator.startNavigationActivity(context, Home)
                activity?.finish()
            }
            is ShowErrorSnackbar ->
                environment.coroutineScope.launch {
                    environment.snackbarHostState.showSnackbar(
                        getSnackbarMessage(sideEffect.errorType, environment),
                    )
                }
        }
    }

    FingerprintSetupScreen(
        modifier = modifier,
        state = state.value,
        snackbarHostState = environment.snackbarHostState,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun rememberFingerprintSetupEnvironment(
    onIntent: (FingerprintSetupIntent) -> Unit,
    executor: Executor = koinInject(),
    biometricPromptBuilder: BiometricPrompt.PromptInfo.Builder = koinInject(),
): FingerprintSetupEnvironment {
    val context = LocalContext.current
    val authenticationLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onIntent(AuthenticationSuccess)
            }
        }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    return remember {
        FingerprintSetupEnvironment(
            context = context,
            authenticationLauncher = authenticationLauncher,
            biometricPromptBuilder = biometricPromptBuilder,
            executor = executor,
            snackbarHostState = snackbarHostState,
            coroutineScope = coroutineScope,
        )
    }
}

@Composable
private fun FingerprintSetupScreen(
    state: FingerprintSetupState,
    snackbarHostState: SnackbarHostState,
    onIntent: (FingerprintSetupIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            Image(
                painter =
                    painterResource(
                        if (state.hasBiometricSetup) {
                            CoreUiR.drawable.ic_use_fingerprint
                        } else {
                            CoreUiR.drawable.ic_configure_fingerprint
                        },
                    ),
                contentDescription = null,
                modifier = Modifier.height(200.dp),
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text =
                    stringResource(
                        if (state.hasBiometricSetup) {
                            LocalizationR.string.fingerprint_setup_use_title
                        } else {
                            LocalizationR.string.fingerprint_setup_configure_title
                        },
                    ),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text =
                    stringResource(
                        if (state.hasBiometricSetup) {
                            LocalizationR.string.fingerprint_setup_use_description
                        } else {
                            LocalizationR.string.fingerprint_setup_configure_description
                        },
                    ),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.weight(1f))

            PrimaryButton(
                onClick = { onIntent(UseFingerprint) },
                text = stringResource(LocalizationR.string.fingerprint_setup_use_fingerprint_button),
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { onIntent(MaybeLater) },
            ) {
                Text(text = stringResource(LocalizationR.string.common_maybe_later), color = MaterialTheme.colorScheme.onBackground)
            }

            Spacer(modifier = Modifier.height(24.dp))
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

        KeyChangesDetectedAlertDialog(
            isVisible = state.showKeyChangesDetected,
            onConfirm = { onIntent(ConfirmKeyPermanentlyInvalidated) },
            onDismiss = { onIntent(DismissKeyPermanentlyInvalidated) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FingerprintSetupScreenWithBiometricPreview() {
    PassboltTheme {
        FingerprintSetupScreen(
            state =
                FingerprintSetupState(
                    hasBiometricSetup = true,
                    showKeyChangesDetected = false,
                ),
            snackbarHostState = SnackbarHostState(),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FingerprintSetupScreenWithoutBiometricPreview() {
    PassboltTheme {
        FingerprintSetupScreen(
            state =
                FingerprintSetupState(
                    hasBiometricSetup = false,
                    showKeyChangesDetected = false,
                ),
            snackbarHostState = SnackbarHostState(),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FingerprintSetupScreenWithKeyChangesDialogPreview() {
    PassboltTheme {
        FingerprintSetupScreen(
            state =
                FingerprintSetupState(
                    hasBiometricSetup = true,
                    showKeyChangesDetected = true,
                ),
            snackbarHostState = SnackbarHostState(),
            onIntent = {},
        )
    }
}
