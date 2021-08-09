package com.passbolt.mobile.android.core.mvp.session

interface AuthenticatedUseCaseOutput {
    val authenticationState: AuthenticationState
}

sealed class AuthenticationState {

    object Authenticated : AuthenticationState()

    class Unauthenticated(val reason: Reason) : AuthenticationState() {

        enum class Reason {
            PASSPHRASE, SESSION
        }
    }
}

typealias UnauthenticatedReason = AuthenticationState.Unauthenticated.Reason
