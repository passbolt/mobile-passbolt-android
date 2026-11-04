package com.passbolt.mobile.android.feature.authentication.mfa.totp

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
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
import com.passbolt.mobile.android.core.clipboard.ClipboardAccess
import com.passbolt.mobile.android.core.compose.PassboltTheme
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.AuthenticationSignIn
import com.passbolt.mobile.android.core.ui.dialogs.LeaveSetupAlertDialog
import com.passbolt.mobile.android.core.ui.progressdialog.ProgressDialog
import com.passbolt.mobile.android.core.ui.snackbar.ColoredSnackbarVisuals
import com.passbolt.mobile.android.feature.authentication.mfa.MfaDialogState
import com.passbolt.mobile.android.feature.authentication.mfa.MfaResult
import com.passbolt.mobile.android.feature.authentication.mfa.MfaResult.Closed
import com.passbolt.mobile.android.feature.authentication.mfa.MfaResult.OtherProvider
import com.passbolt.mobile.android.feature.authentication.mfa.MfaResult.Succeeded
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpIntent.ChooseOtherProvider
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpIntent.Close
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpIntent.ConfirmSetupLeave
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpIntent.DismissSetupLeave
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpIntent.PasteFromClipboard
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpIntent.ToggleRememberMe
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpIntent.ValidateOtp
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.ClearOtp
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.CloseAndNavigateToStartup
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.NavigateToLogin
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.NotifyChooseOtherProvider
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.NotifyLoginSucceeded
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.NotifyVerificationSucceeded
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.PasteOtp
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpState.OtpTextColor.DEFAULT
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpState.OtpTextColor.ERROR
import com.passbolt.mobile.android.feature.authentication.mfa.totp.compose.DigitsOnlySanitizer
import com.passbolt.mobile.android.feature.authentication.mfa.totp.compose.PinInput
import com.passbolt.mobile.android.feature.authentication.mfa.totp.compose.PinInputState
import com.passbolt.mobile.android.feature.authentication.mfa.totp.compose.rememberPinInputState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

private const val OTP_LENGTH = 6

@Composable
internal fun EnterTotpScreen(
    mfaState: MfaDialogState.Totp,
    onMfaResult: (MfaResult) -> Unit,
    isSetupFlow: Boolean = false,
    viewModel: EnterTotpViewModel =
        koinViewModel {
            parametersOf(mfaState.authToken, mfaState.hasOtherProviders, isSetupFlow)
        },
    clipboardAccess: ClipboardAccess = koinInject(),
    appNavigator: AppNavigator = koinInject(),
) {
    val context = LocalContext.current
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    BackHandler { viewModel.onIntent(Close) }

    val pinInputState =
        rememberPinInputState(
            onPinComplete = { otp -> viewModel.onIntent(ValidateOtp(otp)) },
        )

    val errorSnackbarColor = colorResource(CoreUiR.color.red)
    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is NotifyVerificationSucceeded ->
                onMfaResult(Succeeded(sideEffect.mfaHeader))
            is NotifyLoginSucceeded ->
                onMfaResult(Succeeded(null))
            is NotifyChooseOtherProvider ->
                onMfaResult(OtherProvider(sideEffect.bearer, MfaProvider.TOTP))
            is CloseAndNavigateToStartup -> onMfaResult(Closed)
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
            is PasteOtp -> {
                val pasteData = clipboardAccess.getPrimaryClipTextOrNull()
                val cleaned = pasteData?.replace("\\s".toRegex(), "")
                if (!cleaned.isNullOrBlank() && cleaned.length == OTP_LENGTH && cleaned.all { it.isDigit() }) {
                    pinInputState.setText(cleaned)
                } else {
                    Timber.e("Incorrect TOTP code")
                }
            }
            is ClearOtp -> pinInputState.setText("")
        }
    }

    EnterTotpScreen(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        pinInputState = pinInputState,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EnterTotpScreen(
    state: EnterTotpState,
    onIntent: (EnterTotpIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    pinInputState: PinInputState,
    isImeVisible: Boolean = WindowInsets.isImeVisible,
) {
    val textColor =
        when (state.otpTextColor) {
            DEFAULT -> colorResource(CoreUiR.color.text_primary)
            ERROR -> colorResource(CoreUiR.color.red)
        }

    Scaffold(
        modifier = Modifier.imePadding(),
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

            AnimatedVisibility(
                visible = !isImeVisible,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(LocalizationR.string.dialog_mfa_mfa),
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Image(
                        painter = painterResource(CoreUiR.drawable.totp_logo),
                        contentDescription = null,
                        modifier = Modifier.size(116.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(LocalizationR.string.dialog_mfa_totp),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(LocalizationR.string.dialog_mfa_enter_otp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            PinInput(
                state = pinInputState,
                textColor = textColor,
                modifier = Modifier.padding(horizontal = 32.dp),
                onLongPress = { onIntent(PasteFromClipboard) },
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { onIntent(PasteFromClipboard) },
            ) {
                Text(
                    text = stringResource(LocalizationR.string.dialog_mfa_paste_code),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Checkbox(
                    checked = state.rememberMe,
                    onCheckedChange = { onIntent(ToggleRememberMe(it)) },
                )
                Text(
                    text = stringResource(LocalizationR.string.dialog_mfa_remember_me),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (state.hasOtherProvider) {
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
private fun EnterTotpScreenPreview() {
    PassboltTheme {
        EnterTotpScreen(
            state = EnterTotpState(hasOtherProvider = true),
            onIntent = {},
            snackbarHostState = SnackbarHostState(),
            pinInputState =
                PinInputState(
                    initialValue = "12",
                    sanitizer = DigitsOnlySanitizer(maxLength = OTP_LENGTH),
                ),
            isImeVisible = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EnterTotpScreenImeVisiblePreview() {
    PassboltTheme {
        EnterTotpScreen(
            state = EnterTotpState(hasOtherProvider = true),
            onIntent = {},
            snackbarHostState = SnackbarHostState(),
            pinInputState =
                PinInputState(
                    initialValue = "12",
                    sanitizer = DigitsOnlySanitizer(maxLength = OTP_LENGTH),
                ),
            isImeVisible = true,
        )
    }
}
