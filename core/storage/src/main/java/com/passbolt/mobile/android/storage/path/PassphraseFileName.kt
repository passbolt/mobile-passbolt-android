package com.passbolt.mobile.android.storage.path

import com.passbolt.mobile.android.storage.usecase.PASSPHRASE_FILE_NAME

class PassphraseFileName(userId: String) {

    val path = PASSPHRASE_FILE_NAME_FORMAT.format(userId)

    private companion object {
        private const val PASSPHRASE_FILE_NAME_FORMAT = "${PASSPHRASE_FILE_NAME}_%s"
    }
}
