package com.passbolt.mobile.android.core.mvp.authentication

interface AuthenticatedUseCaseOutput {
    val authenticationState: AuthenticationState
}

sealed class AuthenticationState {
    data object Authenticated : AuthenticationState()

    class Unauthenticated(
        val reason: Reason,
    ) : AuthenticationState() {
        sealed class Reason {
            data object Passphrase : Reason()

            data object Session : Reason()

            data class Mfa(
                val providers: List<MfaProvider?>?,
            ) : Reason() {
                enum class MfaProvider(
                    val providerName: String,
                ) {
                    YUBIKEY("yubikey"),
                    TOTP("totp"),
                    DUO("duo"),
                    ;

                    companion object {
                        fun parse(provider: String?) =
                            when (provider) {
                                "yubikey" -> YUBIKEY
                                "totp" -> TOTP
                                "duo" -> DUO
                                else -> null
                            }
                    }
                }
            }
        }
    }
}

operator fun AuthenticationState.plus(other: AuthenticationState): AuthenticationState =
    when {
        this is AuthenticationState.Authenticated && other is AuthenticationState.Authenticated ->
            AuthenticationState.Authenticated
        else -> {
            val isAnyAPassphraseReason =
                (
                    this is AuthenticationState.Unauthenticated &&
                        this.reason is AuthenticationState.Unauthenticated.Reason.Passphrase
                ) ||
                    (
                        other is AuthenticationState.Unauthenticated &&
                            other.reason is AuthenticationState.Unauthenticated.Reason.Passphrase
                    )

            // resurface passphrase reason if present when multiple reasons
            AuthenticationState.Unauthenticated(
                if (isAnyAPassphraseReason) {
                    AuthenticationState.Unauthenticated.Reason.Passphrase
                } else {
                    AuthenticationState.Unauthenticated.Reason.Session
                },
            )
        }
    }

typealias UnauthenticatedReason = AuthenticationState.Unauthenticated.Reason
