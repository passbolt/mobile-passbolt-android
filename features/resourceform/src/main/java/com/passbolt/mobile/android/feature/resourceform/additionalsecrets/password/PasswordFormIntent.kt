package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password

internal sealed interface PasswordFormIntent {
    data class PasswordTextChanged(
        val password: String,
    ) : PasswordFormIntent

    data class MainUriTextChanged(
        val mainUri: String,
    ) : PasswordFormIntent

    data class UsernameTextChanged(
        val username: String,
    ) : PasswordFormIntent

    data object GeneratePassword : PasswordFormIntent

    data object ApplyChanges : PasswordFormIntent

    data object GoBack : PasswordFormIntent
}
