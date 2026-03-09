package com.passbolt.mobile.android.feature.authentication.compose

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticatedIntent.AuthenticationRefreshed
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticatedIntent.Disposed
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticatedIntent.Launched
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationSideEffect.ShowAuth
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationSideEffect.ShowMfaAuth
import com.passbolt.mobile.android.feature.authentication.mfa.MfaDialogState
import com.passbolt.mobile.android.feature.authentication.mfa.MfaResult
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoScreen
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpScreen
import com.passbolt.mobile.android.feature.authentication.mfa.unknown.UnknownProviderScreen
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeyScreen
import kotlinx.coroutines.flow.Flow

/**
 * Composable that handles authentication side effects from AuthenticatedViewModel.
 * This should be used in screens that need to handle authentication flows.
 */
@Composable
fun AuthenticationHandler(
    onAuthenticatedIntent: (AuthenticatedIntent) -> Unit,
    authenticationSideEffect: Flow<AuthenticationSideEffect>,
) {
    val context = LocalContext.current

    val currentOnAuthenticatedIntent by rememberUpdatedState(onAuthenticatedIntent)

    var mfaDialogState by remember { mutableStateOf<MfaDialogState?>(null) }

    // Needed as long as all navigation child screens are not migrated to compose
    // Then authentication can be handled once in MainActivity and these can be removed
    DisposableEffect(Unit) {
        currentOnAuthenticatedIntent(Launched)
        onDispose {
            currentOnAuthenticatedIntent(Disposed)
        }
    }

    val authenticationResult =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onAuthenticatedIntent(AuthenticationRefreshed)
            }
        }

    LaunchedEffect(Unit) {
        authenticationSideEffect.collect { sideEffect ->
            when (sideEffect) {
                is ShowAuth -> {
                    authenticationResult.launch(
                        ActivityIntents.authentication(context, sideEffect.type),
                    )
                }
                is ShowMfaAuth -> {
                    mfaDialogState = sideEffect.toMfaDialogState()
                }
            }
        }
    }

    mfaDialogState?.let { state ->
        MfaDialog(
            state = state,
            onMfaResult = { result ->
                mfaDialogState = null
                when (result) {
                    is MfaResult.Succeeded -> onAuthenticatedIntent(AuthenticationRefreshed)
                    is MfaResult.OtherProvider ->
                        onAuthenticatedIntent(AuthenticatedIntent.OtherProviderClick(result.currentProvider))
                }
            },
        )
    }
}

@Composable
private fun MfaDialog(
    state: MfaDialogState,
    onMfaResult: (MfaResult) -> Unit,
) {
    when (state) {
        is MfaDialogState.Totp -> EnterTotpScreen(mfaState = state, onMfaResult = onMfaResult)
        is MfaDialogState.Yubikey -> ScanYubikeyScreen(mfaState = state, onMfaResult = onMfaResult)
        is MfaDialogState.Duo -> AuthWithDuoScreen(mfaState = state, onMfaResult = onMfaResult)
        is MfaDialogState.UnknownProvider -> UnknownProviderScreen()
    }
}

private fun ShowMfaAuth.toMfaDialogState(): MfaDialogState =
    when (mfaReason) {
        MfaProvider.TOTP -> MfaDialogState.Totp(authToken = sessionAccessToken, hasOtherProviders = hasMultipleProviders)
        MfaProvider.YUBIKEY -> MfaDialogState.Yubikey(authToken = sessionAccessToken, hasOtherProviders = hasMultipleProviders)
        MfaProvider.DUO -> MfaDialogState.Duo(authToken = sessionAccessToken, hasOtherProviders = hasMultipleProviders)
        null -> MfaDialogState.UnknownProvider
    }
