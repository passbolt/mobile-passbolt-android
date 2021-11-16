package com.passbolt.mobile.android.storage.paths

class BiometricKeyIvFileName(userId: String) {

    val name = BIOMETRIC_KEY_IV_FILE_NAME_FORMAT.format(userId)

    private companion object {
        private const val BIOMETRIC_KEY_IV = "BIOMETRIC_KEY_IV"
        private const val BIOMETRIC_KEY_IV_FILE_NAME_FORMAT = "${BIOMETRIC_KEY_IV}_%s"
    }
}
