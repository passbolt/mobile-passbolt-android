package com.passbolt.mobile.android.storage.paths

class AccountDataFileName(userId: String) {

    val name = ACCOUNT_DATA_FILE_NAME_FORMAT.format(userId)

    private companion object {
        private const val ACCOUNTS_DATA_ALIAS = "accounts"
        private const val ACCOUNT_DATA_FILE_NAME_FORMAT = "${ACCOUNTS_DATA_ALIAS}_%s"
    }
}
