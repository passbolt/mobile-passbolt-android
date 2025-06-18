package com.passbolt.mobile.android.metadata.usecase

class TrustedMetadataKeyFileName(userId: String) {

    val name = TRUSTED_METADATA_KEY_FILE_NAME_FORMAT.format(userId)

    private companion object {
        private const val TRUSTED_METADATA_KEY_FILE_NAME = "trusted_metadata_key"
        private const val TRUSTED_METADATA_KEY_FILE_NAME_FORMAT = "${TRUSTED_METADATA_KEY_FILE_NAME}_%s"
    }
}
