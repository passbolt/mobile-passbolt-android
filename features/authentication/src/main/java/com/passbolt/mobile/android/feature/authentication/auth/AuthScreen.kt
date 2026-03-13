package com.passbolt.mobile.android.feature.authentication.auth

import PassboltTheme
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.Start
import com.passbolt.mobile.android.core.navigation.compose.keys.AuthenticationNavigationKey.MfaDuo
import com.passbolt.mobile.android.core.navigation.compose.keys.AuthenticationNavigationKey.MfaTotp
import com.passbolt.mobile.android.core.navigation.compose.keys.AuthenticationNavigationKey.MfaUnknownProvider
import com.passbolt.mobile.android.core.navigation.compose.keys.AuthenticationNavigationKey.MfaYubikey
import com.passbolt.mobile.android.core.navigation.compose.keys.LogsNavigationKey.Logs
import com.passbolt.mobile.android.core.navigation.compose.results.ResultEffect
import com.passbolt.mobile.android.core.ui.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.circularimage.CircularProfileImage
import com.passbolt.mobile.android.core.ui.dialogs.RootWarningAlertDialog
import com.passbolt.mobile.android.core.ui.dialogs.ServerNotReachableDialog
import com.passbolt.mobile.android.core.ui.progressdialog.ProgressDialog
import com.passbolt.mobile.android.core.ui.snackbar.ColoredSnackbarVisuals
import com.passbolt.mobile.android.core.ui.text.PasswordInput
import com.passbolt.mobile.android.core.ui.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.AcceptChangedServerFingerprint
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.AccessLogs
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.AuthenticateUsingBiometry
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.BiometricKeyInvalidated
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.ConfirmSetupLeave
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.ConnectToExistingAccount
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.DismissConfirmSetupLeave
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.DismissHelpMenu
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.DismissNoAccountExplanation
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.DismissServerNotReachable
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.ForgotPassword
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.GoBack
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.OpenHelpMenu
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.PassphraseInputChanged
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.SignIn
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.AuthSuccess
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.HideKeyboard
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.LaunchBiometricPrompt
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.NavigateToAccountList
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.NavigateToLogs
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.NavigateToMfa
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.authentication.auth.AuthState.RefreshAuthReason.PASSPHRASE
import com.passbolt.mobile.android.feature.authentication.auth.AuthState.RefreshAuthReason.SESSION
import com.passbolt.mobile.android.feature.authentication.auth.accountdoesnotexist.AccountDoesNotExistDialog
import com.passbolt.mobile.android.feature.authentication.mfa.MfaDialogState
import com.passbolt.mobile.android.feature.authentication.mfa.MfaResult
import com.passbolt.mobile.android.featureflagserror.FeatureFlagsFetchErrorDialog
import com.passbolt.mobile.android.helpmenu.HelpMenuBottomSheet
import com.passbolt.mobile.android.testtags.composetags.Auth
import com.passbolt.mobile.android.ui.HelpMenuModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import java.util.concurrent.Executor
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Suppress("CyclomaticComplexMethod")
@Composable
internal fun AuthScreen(
    authConfig: ActivityIntents.AuthConfig,
    userId: String,
    appContext: AppContext,
    navigator: AppNavigator = koinInject(),
    viewModel: AuthViewModel = koinViewModel { parametersOf(authConfig, userId, appContext) },
    biometricPromptBuilder: BiometricPrompt.PromptInfo.Builder = koinInject(),
    executor: Executor = koinInject(),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    BackHandler {
        viewModel.onIntent(GoBack)
    }

    AuthScreen(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
    )

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is NavigateBack -> {
                if (authConfig is ActivityIntents.AuthConfig.Setup) {
                    navigator.startNavigationActivity(context, Start)
                } else {
                    navigator.navigateBack()
                }
            }
            is AuthSuccess -> {
                activity?.setResult(Activity.RESULT_OK)
                when (sideEffect.authConfig) {
                    is ActivityIntents.AuthConfig.Startup,
                    is ActivityIntents.AuthConfig.ManageAccount,
                    -> {
                        val destination =
                            when (sideEffect.appContext) {
                                AppContext.APP -> NavigationActivity.Home
                                AppContext.AUTOFILL -> NavigationActivity.AutofillReorderToFront
                            }
                        navigator.finishActivity(activity)
                        navigator.startNavigationActivity(context, destination)
                    }
                    else -> navigator.finishActivity(activity)
                }
            }
            is NavigateToAccountList -> navigator.navigateBack()
            is NavigateToLogs -> navigator.navigateToKey(Logs)
            is LaunchBiometricPrompt -> {
                val subtitle =
                    sideEffect.authReason?.let { reason ->
                        when (reason) {
                            SESSION ->
                                context.getString(LocalizationR.string.auth_reason_session_expired)
                            PASSPHRASE ->
                                context.getString(LocalizationR.string.auth_reason_passphrase_expired)
                        }
                    } ?: ""
                showBiometricPrompt(
                    activity = context as AppCompatActivity,
                    executor = executor,
                    biometricPromptBuilder = biometricPromptBuilder,
                    fingerprintEncryptionCipher = sideEffect.cipher,
                    title = context.getString(LocalizationR.string.auth_biometric_title),
                    subtitle = subtitle,
                    onAuthenticationSuccess = { resultCipher ->
                        viewModel.onIntent(AuthIntent.BiometricAuthenticationSuccess(resultCipher))
                    },
                    onAuthenticationError = { error ->
                        viewModel.onIntent(AuthIntent.BiometricAuthenticationError(error))
                    },
                    onAuthenticationCancelled = {},
                    onKeyPermanentlyInvalidated = {
                        viewModel.onIntent(BiometricKeyInvalidated)
                    },
                )
            }
            is HideKeyboard -> focusManager.clearFocus()
            is AuthSideEffect.FinishAffinity -> navigator.finishAffinity(activity)
            is NavigateToMfa -> {
                val key =
                    when (val mfaState = sideEffect.mfaState) {
                        is MfaDialogState.Totp -> MfaTotp(mfaState.authToken, mfaState.hasOtherProviders)
                        is MfaDialogState.Yubikey -> MfaYubikey(mfaState.authToken, mfaState.hasOtherProviders)
                        is MfaDialogState.Duo -> MfaDuo(mfaState.authToken, mfaState.hasOtherProviders)
                        is MfaDialogState.UnknownProvider -> MfaUnknownProvider
                    }
                navigator.navigateToKey(key)
            }
            is ShowErrorSnackbar -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        ColoredSnackbarVisuals(
                            message = getSnackBarMessage(context, sideEffect.kind, sideEffect.message),
                            backgroundColor = Color(context.getColor(CoreUiR.color.red)),
                        ),
                    )
                }
            }
        }
    }

    ResultEffect<MfaResult> { result ->
        when (result) {
            is MfaResult.Succeeded -> viewModel.onIntent(AuthIntent.MfaSucceeded(result.mfaHeader))
            is MfaResult.OtherProvider ->
                viewModel.onIntent(
                    AuthIntent.ChooseOtherMfaProvider(result.bearer, result.currentProvider),
                )
        }
    }
}

@Composable
private fun AuthScreen(
    state: AuthState,
    onIntent: (AuthIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    Scaffold(
        topBar = {
            TitleAppBar(
                title = getTitleText(LocalContext.current, state.authReason),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
                actions = {
                    IconButton(onClick = { onIntent(OpenHelpMenu) }) {
                        Image(
                            painter = painterResource(CoreUiR.drawable.ic_help),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
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
            Spacer(modifier = Modifier.height(88.dp))

            CircularProfileImage(
                imageUrl = state.accountData.avatarUrl,
                width = 56.dp,
                height = 56.dp,
                placeholderRes = CoreUiR.drawable.ic_avatar_placeholder,
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.accountData.label,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
            )

            state.accountData.email?.let { email ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.accountData.domain,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
            )

            Spacer(modifier = Modifier.height(40.dp))

            PasswordInput(
                title = stringResource(LocalizationR.string.auth_password_title),
                hint = stringResource(LocalizationR.string.auth_enter_passphrase),
                text = String(state.passphrase, Charsets.UTF_8),
                onTextChange = { onIntent(PassphraseInputChanged(it.toByteArray(Charsets.UTF_8))) },
                testTag = Auth.PASSPHRASE_INPUT,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
            )

            if (state.showBiometricButton) {
                Spacer(modifier = Modifier.height(8.dp))
                IconButton(
                    onClick = { onIntent(AuthenticateUsingBiometry) },
                    modifier =
                        Modifier
                            .size(56.dp)
                            .border(
                                width = 1.dp,
                                color = colorResource(CoreUiR.color.divider),
                                shape = CircleShape,
                            ),
                ) {
                    Icon(
                        painter = painterResource(CoreUiR.drawable.ic_fingerprint_primary),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            state.authReason?.let { reason ->
                val reasonText =
                    when (reason) {
                        SESSION -> stringResource(LocalizationR.string.auth_reason_session_expired)
                        PASSPHRASE -> stringResource(LocalizationR.string.auth_reason_passphrase_expired)
                    }
                Text(
                    text = reasonText,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            PrimaryButton(
                text = stringResource(LocalizationR.string.auth_sign_in),
                onClick = { onIntent(SignIn) },
                isEnabled = state.isAuthButtonEnabled,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            TextButton(
                onClick = { onIntent(ForgotPassword) },
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(LocalizationR.string.auth_forgot_password_button),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }

    if (state.showForgotPasswordDialog) {
        AlertDialog(
            title = { Text(stringResource(LocalizationR.string.auth_forgot_password_title)) },
            text = { Text(stringResource(LocalizationR.string.auth_forgot_password_message)) },
            confirmButton = {
                TextButton(onClick = { onIntent(DismissNoAccountExplanation) }) {
                    Text(stringResource(LocalizationR.string.got_it))
                }
            },
            onDismissRequest = { onIntent(DismissNoAccountExplanation) },
        )
    }

    if (state.canShowLeaveConfirmation && state.showLeaveConfirmationDialog) {
        AlertDialog(
            title = { Text(stringResource(LocalizationR.string.are_you_sure)) },
            text = { Text(stringResource(LocalizationR.string.auth_exit_dialog_message)) },
            confirmButton = {
                TextButton(onClick = { onIntent(DismissConfirmSetupLeave) }) {
                    Text(stringResource(LocalizationR.string.continue_setup))
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(ConfirmSetupLeave) }) {
                    Text(stringResource(LocalizationR.string.cancel_setup))
                }
            },
            onDismissRequest = { onIntent(DismissConfirmSetupLeave) },
        )
    }

    ServerNotReachableDialog(
        isVisible = state.showServerNotReachable,
        domain = state.serverNotReachableDomain,
        onDismiss = { onIntent(DismissServerNotReachable) },
    )

    RootWarningAlertDialog(
        isVisible = state.showDeviceRooted,
        onAcknowledge = { onIntent(AuthIntent.RootedDeviceAcknowledged) },
    )

    if (state.showHelpMenu) {
        HelpMenuBottomSheet(
            helpMenuModel =
                HelpMenuModel(
                    shouldShowShowQrCodesHelp = false,
                    shouldShowImportProfile = false,
                    shouldShowImportAccountKit = false,
                ),
            onDismissRequest = { onIntent(DismissHelpMenu) },
            onAccessLogs = { onIntent(AccessLogs) },
        )
    }

    FeatureFlagsFetchErrorDialog(
        isVisible = state.showFetchFeatureFlagsError,
        onRetry = { onIntent(AuthIntent.Retry) },
        onSignOut = { onIntent(AuthIntent.SignOut) },
    )

    if (state.showAccountDoesNotExist) {
        AccountDoesNotExistDialog(
            label = state.accountDoesNotExistLabel,
            email = state.accountDoesNotExistEmail,
            url = state.accountDoesNotExistUrl,
            onConnectToExistingAccount = { onIntent(ConnectToExistingAccount) },
        )
    }

    if (state.showServerFingerprintChanged) {
        ServerFingerprintChangedDialog(
            fingerprint = state.serverFingerprintChangedFingerprint,
            onAcceptNewKey = { onIntent(AcceptChangedServerFingerprint(it)) },
            onBack = { onIntent(AuthIntent.RejectChangedServerFingerprint) },
        )
    }

    ProgressDialog(isVisible = state.showProgress)
}

@Preview(showBackground = true)
@Composable
private fun AuthScreenPreview() {
    PassboltTheme {
        AuthScreen(
            state =
                AuthState(
                    accountData =
                        AuthState.AccountData(
                            label = "John Doe",
                            email = "john@passbolt.com",
                            domain = "https://passbolt.local",
                        ),
                    showBiometricButton = true,
                ),
            onIntent = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
