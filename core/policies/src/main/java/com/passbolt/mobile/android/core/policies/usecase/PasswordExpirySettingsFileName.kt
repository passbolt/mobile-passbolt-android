package com.passbolt.mobile.android.core.policies.usecase

class PasswordExpirySettingsFileName(
    userId: String,
) {
    val name = PASSWORD_EXPIRY_SETTINGS_FILE_NAME_FORMAT.format(userId)

    private companion object {
        private const val PASSWORD_EXPIRY_SETTINGS_FILE_NAME = "password_expiry_settings"
        private const val PASSWORD_EXPIRY_SETTINGS_FILE_NAME_FORMAT = "${PASSWORD_EXPIRY_SETTINGS_FILE_NAME}_%s"
    }
}
