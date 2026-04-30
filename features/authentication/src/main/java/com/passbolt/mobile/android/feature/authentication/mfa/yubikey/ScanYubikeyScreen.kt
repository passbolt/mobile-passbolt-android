package com.passbolt.mobile.android.feature.authentication.mfa.yubikey

import PassboltTheme
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.AuthenticationSignIn
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.Start
import com.passbolt.mobile.android.core.ui.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.progressdialog.ProgressDialog
import com.passbolt.mobile.android.core.ui.snackbar.ColoredSnackbarVisuals
import com.passbolt.mobile.android.feature.authentication.mfa.MfaDialogState
import com.passbolt.mobile.android.feature.authentication.mfa.MfaResult
import com.passbolt.mobile.android.feature.authentication.mfa.MfaResult.OtherProvider
import com.passbolt.mobile.android.feature.authentication.mfa.MfaResult.Succeeded
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeyIntent.CancelYubikeyScan
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeyIntent.ChooseOtherProvider
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeyIntent.Close
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeyIntent.DismissNotFromCurrentUserDialog
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeyIntent.DismissScanCancelledDialog
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeyIntent.ScanYubikey
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeyIntent.ToggleRememberMe
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeyIntent.ValidateYubikeyOtp
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.CloseAndNavigateToStartup
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.LaunchYubikeyScan
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.NavigateToLogin
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.NotifyChooseOtherProvider
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.NotifyLoginSucceeded
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.NotifyVerificationSucceeded
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.ShowErrorSnackbar
import com.yubico.yubikit.android.ui.OtpActivity
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun ScanYubikeyScreen(
    mfaState: MfaDialogState.Yubikey,
    onMfaResult: (MfaResult) -> Unit,
    appNavigator: AppNavigator = koinInject(),
    viewModel: ScanYubikeyViewModel =
        koinViewModel {
            parametersOf(mfaState.authToken, mfaState.hasOtherProviders)
        },
) {
    val context = LocalContext.current
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    BackHandler { viewModel.onIntent(Close) }

    val scanYubikeyLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val otp = result.data?.getStringExtra(OtpActivity.EXTRA_OTP)
                viewModel.onIntent(ValidateYubikeyOtp(otp))
            } else {
                viewModel.onIntent(CancelYubikeyScan)
            }
        }

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is NotifyVerificationSucceeded -> onMfaResult(Succeeded(sideEffect.mfaHeader))
            is NotifyLoginSucceeded -> onMfaResult(Succeeded(null))
            is NotifyChooseOtherProvider -> onMfaResult(OtherProvider(sideEffect.bearer, MfaProvider.YUBIKEY))
            is CloseAndNavigateToStartup -> appNavigator.startNavigationActivity(context, Start)
            is NavigateToLogin -> appNavigator.startNavigationActivity(context, AuthenticationSignIn)
            is LaunchYubikeyScan -> scanYubikeyLauncher.launch(Intent(context, OtpActivity::class.java))
            is ShowErrorSnackbar -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        ColoredSnackbarVisuals(
                            message = getSnackbarMessage(context, sideEffect.kind),
                            backgroundColor = Color(context.getColor(CoreUiR.color.red)),
                        ),
                    )
                }
            }
        }
    }

    ScanYubikeyScreen(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun ScanYubikeyScreen(
    state: ScanYubikeyState,
    onIntent: (ScanYubikeyIntent) -> Unit,
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
                painter = painterResource(CoreUiR.drawable.yubikey_logo),
                contentDescription = null,
                modifier = Modifier.size(116.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(LocalizationR.string.dialog_mfa_yubikey_otp),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

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

            Spacer(modifier = Modifier.height(8.dp))

            PrimaryButton(
                text = stringResource(LocalizationR.string.dialog_mfa_scan_youbikey),
                onClick = { onIntent(ScanYubikey) },
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

    if (state.showScanCancelledDialog) {
        AlertDialog(
            title = { Text(stringResource(LocalizationR.string.dialog_mfa_scan_youbikey_failed_title)) },
            text = { Text(stringResource(LocalizationR.string.dialog_mfa_scan_youbikey_failed_message)) },
            confirmButton = {
                TextButton(onClick = { onIntent(DismissScanCancelledDialog) }) {
                    Text(stringResource(LocalizationR.string.got_it))
                }
            },
            onDismissRequest = { onIntent(DismissScanCancelledDialog) },
        )
    }

    if (state.showNotFromCurrentUserDialog) {
        AlertDialog(
            title = { Text(stringResource(LocalizationR.string.dialog_mfa_scan_youbikey_not_from_current_user_title)) },
            text = { Text(stringResource(LocalizationR.string.dialog_mfa_scan_youbikey_not_from_current_user_message)) },
            confirmButton = {
                TextButton(onClick = { onIntent(DismissNotFromCurrentUserDialog) }) {
                    Text(stringResource(LocalizationR.string.got_it))
                }
            },
            onDismissRequest = { onIntent(DismissNotFromCurrentUserDialog) },
        )
    }

    ProgressDialog(isVisible = state.showProgress)
}

@Preview(showBackground = true)
@Composable
private fun ScanYubikeyScreenPreview() {
    PassboltTheme {
        ScanYubikeyScreen(
            state = ScanYubikeyState(hasOtherProvider = true),
            onIntent = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
