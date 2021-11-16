package com.passbolt.mobile.android.feature.secrets.usecase.decrypt.parser

sealed class DecryptedSecret(val password: String) {

    class SimplePassword(
        password: String
    ) : DecryptedSecret(password)

    class PasswordWithDescription(
        val description: String,
        password: String
    ) : DecryptedSecret(password)
}
