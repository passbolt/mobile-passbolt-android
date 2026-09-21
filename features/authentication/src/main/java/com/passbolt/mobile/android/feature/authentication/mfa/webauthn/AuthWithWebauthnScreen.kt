package com.passbolt.mobile.android.feature.authentication.mfa.webauthn

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.Start
import com.passbolt.mobile.android.core.ui.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.dialogs.LeaveSetupAlertDialog
import com.passbolt.mobile.android.core.ui.progressdialog.ProgressDialog
import com.passbolt.mobile.android.core.ui.switch.TextSwitch
import com.passbolt.mobile.android.feature.authentication.mfa.MfaDialogState
import com.passbolt.mobile.android.feature.authentication.mfa.MfaResult
import com.passbolt.mobile.android.feature.authentication.mfa.MfaResult.OtherProvider
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.AuthenticateWithWebauthn
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.ChooseOtherProvider
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.Close
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.ConfirmSetupLeave
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.DismissSetupLeave
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.ToggleRememberMe
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.CloseAndNavigateToStartup
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.NotifyOtherProviderClicked
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
            parametersOf(mfaState.authToken, mfaState.hasOtherProviders, isSetupFlow)
        },
) {
    val context = LocalContext.current
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    BackHandler { viewModel.onIntent(Close) }

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is NotifyOtherProviderClicked -> onMfaResult(OtherProvider(sideEffect.bearer, MfaProvider.WEBAUTHN))
            is CloseAndNavigateToStartup -> appNavigator.startNavigationActivity(context, Start)
        }
    }

    AuthWithWebauthnScreen(
        state = state,
        onIntent = viewModel::onIntent,
    )
}

@Composable
internal fun AuthWithWebauthnScreen(
    state: AuthWithWebauthnState,
    onIntent: (AuthWithWebauthnIntent) -> Unit,
) {
    Scaffold { paddingValues ->
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
        )
    }
}
