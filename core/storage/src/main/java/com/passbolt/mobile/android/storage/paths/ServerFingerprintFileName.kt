package com.passbolt.mobile.android.storage.paths

class ServerFingerprintFileName(userId: String) {

    val name = SERVER_FINGERPRINT_FILE_NAME_FORMAT.format(userId)

    private companion object {
        private const val SERVER_FINGERPRINT_FILE_NAME = "server_fingerprint"
        private const val SERVER_FINGERPRINT_FILE_NAME_FORMAT = "${SERVER_FINGERPRINT_FILE_NAME}_%s"
    }
}
