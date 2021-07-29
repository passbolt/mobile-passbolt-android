package com.passbolt.mobile.android.core.navigation

import java.io.Serializable

sealed class AuthenticationType : Serializable {

    // only get passphrase into memory cache
    object Passphrase : AuthenticationType()

    // refresh networking session
    object Refresh : AuthenticationType()

    // sign into the account and launch home - used at app start
    data class SignIn(
        val userId: String? = null
    ) : AuthenticationType() {
        companion object {
            const val name = "SignIn"
        }
    }

    // sign into the account and close sign in
    object SignInForResult : AuthenticationType()
}
