package com.passbolt.mobile.android.storage.paths

class PassphraseFileName(userId: String) {

    val name = PASSPHRASE_FILE_NAME_FORMAT.format(userId)

    private companion object {
        private const val PASSPHRASE_FILE_NAME = "passphrase"
        private const val PASSPHRASE_FILE_NAME_FORMAT = "${PASSPHRASE_FILE_NAME}_%s"
    }
}
