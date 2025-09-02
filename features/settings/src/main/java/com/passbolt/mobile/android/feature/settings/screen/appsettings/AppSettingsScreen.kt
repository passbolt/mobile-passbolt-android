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

package com.passbolt.mobile.android.feature.settings.screen.appsettings

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.ActivityIntents.AuthConfig.RefreshPassphrase
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.keys.SettingsNavigationKey.Autofill
import com.passbolt.mobile.android.core.navigation.compose.keys.SettingsNavigationKey.DefaultFilter
import com.passbolt.mobile.android.core.navigation.compose.keys.SettingsNavigationKey.ExpertSettings
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.compose.dialogs.ConfigureFingerprintAlertDialog
import com.passbolt.mobile.android.core.ui.compose.dialogs.DisableFingerprintAlertDialog
import com.passbolt.mobile.android.core.ui.compose.dialogs.KeyChangesDetectedAlertDialog
import com.passbolt.mobile.android.core.ui.compose.menu.OpenableSettingsItem
import com.passbolt.mobile.android.core.ui.compose.menu.SwitchableSettingsItem
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.authentication.auth.showBiometricPrompt
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.CancelConfigureFingerprint
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.CancelConfirmKeyChange
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.CancelDisableFingerprint
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.CanceledBiometricAuth
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.ConfigureFingerprint
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.ConfirmDisableFingerprint
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.ConfirmKeyChangeClick
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.ErroredBiometricAuth
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.FinalizedBiometricAuth
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.GoBack
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.GoToAutofill
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.GoToDefaultFilter
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.GoToExpertSettings
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.InvalidateBiometricKeyPermanently
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.RefreshedPassphrase
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.ShowBiometryError
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.ToggleFingerprint
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateToAutofill
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateToDefaultFilter
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateToExpertSettings
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateToGetPassphrase
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateToSystemSettings
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateUp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.compose.rememberKoinInject
import java.util.concurrent.Executor
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun AppSettingsScreen(
    modifier: Modifier = Modifier,
    navigator: AppNavigator = koinInject(),
    viewModel: AppSettingsViewModel = koinViewModel(),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val environment = rememberAppSettingsEnvironment(viewModel::onIntent)

    AppSettingsSideEffectsHandler(
        sideEffectFlow = viewModel.sideEffect,
        environment = environment,
        onIntent = viewModel::onIntent,
        navigator = navigator,
    )

    AppSettingsScreen(
        modifier = modifier,
        state = state.value,
        snackbarHostState = environment.snackbarHostState,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun rememberAppSettingsEnvironment(onIntent: (AppSettingsIntent) -> Unit): AppSettingsEnvironment {
    val context = LocalContext.current
    val authenticationLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onIntent(RefreshedPassphrase)
            }
        }
    val biometricPromptBuilder = rememberKoinInject<BiometricPrompt.PromptInfo.Builder>()
    val executor = rememberKoinInject<Executor>()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    return remember {
        AppSettingsEnvironment(
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
private fun AppSettingsSideEffectsHandler(
    sideEffectFlow: Flow<AppSettingsSideEffect>,
    environment: AppSettingsEnvironment,
    onIntent: (AppSettingsIntent) -> Unit,
    navigator: AppNavigator,
) {
    SideEffectDispatcher(sideEffectFlow) {
        when (it) {
            NavigateToAutofill -> navigator.navigateToKey(Autofill)
            NavigateToDefaultFilter -> navigator.navigateToKey(DefaultFilter)
            NavigateToExpertSettings -> navigator.navigateToKey(ExpertSettings)
            NavigateUp -> navigator.navigateBack()
            NavigateToGetPassphrase ->
                environment.authenticationLauncher.launch(
                    ActivityIntents.authentication(
                        environment.context,
                        RefreshPassphrase,
                    ),
                )
            NavigateToSystemSettings -> environment.context.startActivity(Intent(Settings.ACTION_SETTINGS))
            is AppSettingsSideEffect.LaunchBiometricPrompt ->
                try {
                    showBiometricPrompt(
                        activity = environment.context as AppCompatActivity,
                        executor = environment.executor,
                        biometricPromptBuilder = environment.biometricPromptBuilder,
                        fingerprintEncryptionCipher = it.cipher,
                        onAuthenticationSuccess = { onIntent(FinalizedBiometricAuth(it)) },
                        onAuthenticationCancelled = { onIntent(CanceledBiometricAuth) },
                        onAuthenticationError = { onIntent(ErroredBiometricAuth(it)) },
                    )
                } catch (exception: KeyPermanentlyInvalidatedException) {
                    onIntent(InvalidateBiometricKeyPermanently(exception))
                } catch (exception: Exception) {
                    onIntent(ShowBiometryError(exception))
                }
            is AppSettingsSideEffect.ShowErrorSnackbar ->
                environment.coroutineScope.launch {
                    environment.snackbarHostState.showSnackbar(getSnackbarMessage(it, environment), duration = SnackbarDuration.Short)
                }
        }
    }
}

private fun getSnackbarMessage(
    snackbar: AppSettingsSideEffect.ShowErrorSnackbar,
    environment: AppSettingsEnvironment,
): String =
    when (snackbar.snackbarKind) {
        AppSettingsSideEffect.SnackbarKind.AUTHENTICAION_ERROR ->
            environment.context.getString(
                LocalizationR.string.settings_app_settings_biometry_authentication_error,
                snackbar.additionalMessage.orEmpty(),
            )
        AppSettingsSideEffect.SnackbarKind.BIOMETRY_ERROR ->
            environment.context.getString(
                LocalizationR.string.settings_app_settings_biometry_error,
                snackbar.additionalMessage.orEmpty(),
            )
    }

@Composable
private fun AppSettingsScreen(
    state: AppSettingsState,
    snackbarHostState: SnackbarHostState,
    onIntent: (AppSettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
    ) {
        Column {
            TitleAppBar(
                title = stringResource(LocalizationR.string.settings_app_settings),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
            )
            SwitchableSettingsItem(
                iconPainter = painterResource(R.drawable.ic_fingerprint),
                title = stringResource(LocalizationR.string.settings_app_settings_fingerprint),
                isChecked = state.isFingerprintEnabled,
                onCheckedChange = { onIntent(ToggleFingerprint) },
            )

            OpenableSettingsItem(
                iconPainter = painterResource(R.drawable.ic_key),
                title = stringResource(LocalizationR.string.settings_app_settings_autofill),
                onClick = { onIntent(GoToAutofill) },
            )

            OpenableSettingsItem(
                iconPainter = painterResource(R.drawable.ic_filter),
                title = stringResource(LocalizationR.string.settings_app_settings_default_filter),
                onClick = { onIntent(GoToDefaultFilter) },
            )

            OpenableSettingsItem(
                iconPainter = painterResource(R.drawable.ic_cog),
                title = stringResource(LocalizationR.string.settings_app_settings_expert_settings),
                onClick = { onIntent(GoToExpertSettings) },
            )
        }

        DisableFingerprintAlertDialog(
            isVisible = state.isDisableFingerprintDialogVisible,
            onDisableConfirm = { onIntent(ConfirmDisableFingerprint) },
            onDismiss = { onIntent(CancelDisableFingerprint) },
        )

        ConfigureFingerprintAlertDialog(
            isVisible = state.isConfigureFingerprintDialogVisible,
            onConfigureFingerprint = { onIntent(ConfigureFingerprint) },
            onDismiss = { onIntent(CancelConfigureFingerprint) },
        )

        KeyChangesDetectedAlertDialog(
            isVisible = state.isKeyChangesDialogDetectedVisible,
            onConfirm = { onIntent(ConfirmKeyChangeClick) },
            onDismiss = { onIntent(CancelConfirmKeyChange) },
        )

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
private fun AppSettingsPreview() {
    AppSettingsScreen(
        state = AppSettingsState(isFingerprintEnabled = true),
        snackbarHostState = remember { SnackbarHostState() },
        onIntent = {},
        modifier = Modifier,
    )
}
