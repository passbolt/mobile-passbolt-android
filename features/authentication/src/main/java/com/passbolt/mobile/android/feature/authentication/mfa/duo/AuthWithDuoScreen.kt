package com.passbolt.mobile.android.feature.authentication.mfa.duo

import PassboltTheme
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
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoIntent.AuthenticateWithDuo
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoIntent.ChooseOtherProvider
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoIntent.Close
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoIntent.DismissDuoAuth
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoIntent.DuoAuthFinished
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoSideEffect.CloseAndNavigateToStartup
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoSideEffect.NavigateToLogin
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoSideEffect.NotifyLoginSucceeded
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoSideEffect.NotifyOtherProviderClicked
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoSideEffect.NotifyVerificationSucceeded
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.authentication.mfa.duo.duowebviewsheet.DuoWebViewSheet
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun AuthWithDuoScreen(
    mfaState: MfaDialogState.Duo,
    onMfaResult: (MfaResult) -> Unit,
    appNavigator: AppNavigator = koinInject(),
    viewModel: AuthWithDuoViewModel =
        koinViewModel {
            parametersOf(mfaState.authToken, mfaState.hasOtherProviders)
        },
) {
    val context = LocalContext.current
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    BackHandler { viewModel.onIntent(Close) }

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is NotifyVerificationSucceeded -> onMfaResult(Succeeded(sideEffect.mfaHeader))
            is NotifyLoginSucceeded -> onMfaResult(Succeeded(null))
            is NotifyOtherProviderClicked -> onMfaResult(OtherProvider(sideEffect.bearer, MfaProvider.DUO))
            is CloseAndNavigateToStartup -> appNavigator.startNavigationActivity(context, Start)
            is NavigateToLogin -> appNavigator.startNavigationActivity(context, AuthenticationSignIn)
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

    AuthWithDuoScreen(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun AuthWithDuoScreen(
    state: AuthWithDuoState,
    onIntent: (AuthWithDuoIntent) -> Unit,
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
                painter = painterResource(CoreUiR.drawable.duo_logo),
                contentDescription = null,
                modifier = Modifier.size(116.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(LocalizationR.string.dialog_mfa_duo),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            PrimaryButton(
                text = stringResource(LocalizationR.string.dialog_mfa_auth_with_duo),
                onClick = { onIntent(AuthenticateWithDuo) },
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

    if (state.showDuoWebViewSheet) {
        DuoWebViewSheet(
            duoPromptUrl = state.duoPromptUrl,
            onDuoAuthFinish = { duoState -> onIntent(DuoAuthFinished(duoState)) },
            onDismiss = { onIntent(DismissDuoAuth) },
        )
    }

    ProgressDialog(isVisible = state.showProgress)
}

@Preview(showBackground = true)
@Composable
private fun AuthWithDuoScreenPreview() {
    PassboltTheme {
        AuthWithDuoScreen(
            state = AuthWithDuoState(hasOtherProvider = true),
            onIntent = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
