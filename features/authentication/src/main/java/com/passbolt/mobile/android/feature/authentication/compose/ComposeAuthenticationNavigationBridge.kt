package com.passbolt.mobile.android.feature.authentication.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoDialog
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoListener
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpDialog
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpListener
import com.passbolt.mobile.android.feature.authentication.mfa.unknown.UnknownProviderDialog
import com.passbolt.mobile.android.feature.authentication.mfa.youbikey.ScanYubikeyDialog
import com.passbolt.mobile.android.feature.authentication.mfa.youbikey.ScanYubikeyListener

/**
 * Compose implementation of AuthenticationNavigation that works with FragmentManager
 * This allows displaying authentication dialogs from a Composable context
 */
class ComposeAuthenticationNavigationBridge(
    private val activity: FragmentActivity,
    private val onTotpVerificationSucceeded: (String?) -> Unit,
    private val onYubikeyVerificationSucceeded: (String?) -> Unit,
    private val onDuoAuthSucceeded: (String?) -> Unit,
    private val onTotpOtherProviderClick: (String) -> Unit,
    private val onYubikeyOtherProviderClick: (String?) -> Unit,
    private val onDuoOtherProviderClick: (String?) -> Unit,
) : AuthenticationNavigation {
    override fun showMfaAuth(
        mfaReason: MfaProvider?,
        hasMultipleProviders: Boolean,
        sessionAccessToken: String?,
    ) {
        when (mfaReason) {
            MfaProvider.YUBIKEY -> showYubikeyDialog(hasMultipleProviders, sessionAccessToken)
            MfaProvider.TOTP -> showTotpDialog(hasMultipleProviders, sessionAccessToken)
            MfaProvider.DUO -> showDuoDialog(hasMultipleProviders, sessionAccessToken)
            null -> showUnknownProvider()
        }
    }

    override fun showTotpDialog(
        hasOtherProviders: Boolean,
        sessionAccessToken: String?,
    ) {
        val dialog =
            EnterTotpDialog.newInstance(
                token = sessionAccessToken,
                hasOtherProvider = hasOtherProviders,
            )

        dialog.show(
            activity.supportFragmentManager,
            EnterTotpDialog::class.java.name,
        )

        dialog.listener =
            object : EnterTotpListener {
                override fun totpOtherProviderClick(bearer: String) {
                    onTotpOtherProviderClick(bearer)
                }

                override fun totpVerificationSucceeded(mfaHeader: String?) {
                    onTotpVerificationSucceeded(mfaHeader)
                }
            }
    }

    override fun showYubikeyDialog(
        hasOtherProviders: Boolean,
        sessionAccessToken: String?,
    ) {
        val dialog =
            ScanYubikeyDialog.newInstance(
                token = sessionAccessToken,
                hasOtherProvider = hasOtherProviders,
            )

        dialog.show(
            activity.supportFragmentManager,
            ScanYubikeyDialog::class.java.name,
        )

        dialog.listener =
            object : ScanYubikeyListener {
                override fun yubikeyOtherProviderClick(jwtToken: String?) {
                    onYubikeyOtherProviderClick(jwtToken)
                }

                override fun yubikeyVerificationSucceeded(mfaHeader: String?) {
                    onYubikeyVerificationSucceeded(mfaHeader)
                }
            }
    }

    override fun showUnknownProvider() {
        UnknownProviderDialog().show(
            activity.supportFragmentManager,
            UnknownProviderDialog::class.java.name,
        )
    }

    override fun showDuoDialog(
        hasOtherProviders: Boolean,
        sessionAccessToken: String?,
    ) {
        val dialog =
            AuthWithDuoDialog.newInstance(
                token = sessionAccessToken,
                hasOtherProvider = hasOtherProviders,
            )

        dialog.show(
            activity.supportFragmentManager,
            AuthWithDuoDialog::class.java.name,
        )

        dialog.listener =
            object : AuthWithDuoListener {
                override fun duoOtherProviderClick(jwtToken: String?) {
                    onDuoOtherProviderClick(jwtToken)
                }

                override fun duoAuthSucceeded(mfaHeader: String?) {
                    onDuoAuthSucceeded(mfaHeader)
                }
            }
    }

    companion object Companion {
        @Composable
        fun rememberAuthenticationNavigation(onAuthenticatedIntent: (AuthenticatedIntent) -> Unit): AuthenticationNavigation {
            val context = LocalContext.current
            val activity =
                context as? FragmentActivity
                    ?: error("Currently FragmentActivity is needed as parent to show MFA dialogs.")

            return ComposeAuthenticationNavigationBridge(
                activity = activity,
                onTotpVerificationSucceeded = {
                    onAuthenticatedIntent(AuthenticatedIntent.AuthenticationRefreshed)
                },
                onYubikeyVerificationSucceeded = {
                    onAuthenticatedIntent(AuthenticatedIntent.AuthenticationRefreshed)
                },
                onDuoAuthSucceeded = {
                    onAuthenticatedIntent(AuthenticatedIntent.AuthenticationRefreshed)
                },
                onTotpOtherProviderClick = {
                    onAuthenticatedIntent(AuthenticatedIntent.OtherProviderClick(MfaProvider.TOTP))
                },
                onYubikeyOtherProviderClick = {
                    onAuthenticatedIntent(AuthenticatedIntent.OtherProviderClick(MfaProvider.YUBIKEY))
                },
                onDuoOtherProviderClick = {
                    onAuthenticatedIntent(AuthenticatedIntent.OtherProviderClick(MfaProvider.DUO))
                },
            )
        }
    }
}
