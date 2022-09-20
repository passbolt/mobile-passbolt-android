package com.passbolt.mobile.android.core.mvp.authentication

interface AuthenticatedUseCaseOutput {
    val authenticationState: AuthenticationState
}

sealed class AuthenticationState {

    object Authenticated : AuthenticationState()

    class Unauthenticated(val reason: Reason) : AuthenticationState() {

        sealed class Reason {

            object Passphrase : Reason()
            object Session : Reason()
            class Mfa(val providers: List<MfaProvider?>?) : Reason() {
                enum class MfaProvider {
                    YUBIKEY,
                    TOTP;

                    companion object {
                        fun parse(provider: String?) =
                            when (provider) {
                                "yubikey" -> YUBIKEY
                                "totp" -> TOTP
                                else -> null
                            }
                    }
                }
            }
        }
    }
}

operator fun AuthenticationState.plus(other: AuthenticationState): AuthenticationState {
    return when {
        this is AuthenticationState.Authenticated && other is AuthenticationState.Authenticated ->
            AuthenticationState.Authenticated
        else -> AuthenticationState.Unauthenticated(AuthenticationState.Unauthenticated.Reason.Session)
    }
}

typealias UnauthenticatedReason = AuthenticationState.Unauthenticated.Reason
