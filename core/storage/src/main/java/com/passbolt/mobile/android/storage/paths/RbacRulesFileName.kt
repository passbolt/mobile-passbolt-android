package com.passbolt.mobile.android.storage.paths

class RbacRulesFileName(userId: String) {

    val name = RBAC_FLAGS_FILE_NAME_FORMAT.format(userId)

    private companion object {
        private const val RBAC_FLAGS_FILE_NAME = "rbac"
        private const val RBAC_FLAGS_FILE_NAME_FORMAT = "${RBAC_FLAGS_FILE_NAME}_%s"
    }
}
