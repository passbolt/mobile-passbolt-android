package com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser

import com.google.gson.annotations.SerializedName

sealed class DecryptedSecret(val password: String) {

    class SimplePassword(
        password: String
    ) : DecryptedSecret(password)

    class PasswordWithDescription(
        val description: String,
        password: String
    ) : DecryptedSecret(password)

    class StandaloneTotp(
        val totp: Totp
    ) {
        data class Totp(
            val algorithm: String,
            @SerializedName("secret_key")
            val key: String,
            val digits: Int,
            val period: Long
        )
    }
}
