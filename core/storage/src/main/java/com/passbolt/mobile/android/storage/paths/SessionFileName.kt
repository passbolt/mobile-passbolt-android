package com.passbolt.mobile.android.storage.paths

class SessionFileName(userId: String) {

    val name = SESSION_FILE_NAME_FORMAT.format(userId)

    private companion object {
        private const val SESSION_TOKENS_ALIAS = "sessions"
        private const val SESSION_FILE_NAME_FORMAT = "${SESSION_TOKENS_ALIAS}_%s"
    }
}
