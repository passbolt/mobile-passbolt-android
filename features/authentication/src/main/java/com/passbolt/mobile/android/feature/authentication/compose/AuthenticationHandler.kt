package com.passbolt.mobile.android.feature.authentication.compose

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticatedIntent.AuthenticationRefreshed
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationSideEffect.ShowAuth
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationSideEffect.ShowDuoDialog
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationSideEffect.ShowMfaAuth
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationSideEffect.ShowTotpDialog
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationSideEffect.ShowUnknownProvider
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationSideEffect.ShowYubikeyDialog
import kotlinx.coroutines.flow.Flow

/**
 * Composable that handles authentication side effects from AuthenticatedViewModel.
 * This should be used in screens that need to handle authentication flows.
 */
@Composable
fun AuthenticationHandler(
    onAuthenticatedIntent: (AuthenticatedIntent) -> Unit,
    authenticationNavigation: AuthenticationNavigation,
    authenticationSideEffect: Flow<AuthenticationSideEffect>,
) {
    val context = LocalContext.current

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
                is ShowTotpDialog -> {
                    authenticationNavigation.showTotpDialog(
                        hasOtherProviders = sideEffect.hasOtherProviders,
                        sessionAccessToken = sideEffect.sessionAccessToken,
                    )
                }
                is ShowYubikeyDialog -> {
                    authenticationNavigation.showYubikeyDialog(
                        hasOtherProviders = sideEffect.hasOtherProviders,
                        sessionAccessToken = sideEffect.sessionAccessToken,
                    )
                }
                is ShowDuoDialog -> {
                    authenticationNavigation.showDuoDialog(
                        hasOtherProviders = sideEffect.hasOtherProviders,
                        sessionAccessToken = sideEffect.sessionAccessToken,
                    )
                }
                is ShowMfaAuth -> {
                    authenticationNavigation.showMfaAuth(
                        hasMultipleProviders = sideEffect.hasMultipleProviders,
                        sessionAccessToken = sideEffect.sessionAccessToken,
                        mfaReason = sideEffect.mfaReason,
                    )
                }
                is ShowUnknownProvider -> {
                    authenticationNavigation.showUnknownProvider()
                }
            }
        }
    }
}
