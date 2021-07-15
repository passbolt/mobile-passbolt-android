package com.passbolt.mobile.android.core.navigation

import java.io.Serializable

sealed class AuthenticationType : Serializable {

    object Passphrase : AuthenticationType()

    object Refresh : AuthenticationType()

    data class SignIn(
        val userId: String? = null
    ) : AuthenticationType() {
        companion object {
            const val name = "SignIn"
        }
    }
}
