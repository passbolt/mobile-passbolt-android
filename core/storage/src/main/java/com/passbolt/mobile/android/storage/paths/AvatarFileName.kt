package com.passbolt.mobile.android.storage.paths

class AvatarFileName(userId: String) {

    val name = AVATAR_FILE_NAME_FORMAT.format(userId)

    private companion object {
        private const val USER_AVATAR_FILE_NAME = "user_avatar"
        private const val AVATAR_FILE_NAME_FORMAT = "${USER_AVATAR_FILE_NAME}_%s"
    }
}
