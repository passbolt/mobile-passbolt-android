package com.passbolt.mobile.android.storage.paths

class PrivateKeyFileName(userId: String) {

    val name = PRIVATE_KEY_FILE_NAME_FORMAT.format(userId)

    private companion object {
        private const val PRIVATE_KEY_FILE_NAME = "user_key"
        private const val PRIVATE_KEY_FILE_NAME_FORMAT = "${PRIVATE_KEY_FILE_NAME}_%s"
    }
}
