package com.passbolt.mobile.android.featureflags.usecase

class FeatureFlagsFileName(userId: String) {

    val name = FEATURE_FLAGS_FILE_NAME_FORMAT.format(userId)

    private companion object {
        private const val FEATURE_FLAGS_FILE_NAME = "feature_flags"
        private const val FEATURE_FLAGS_FILE_NAME_FORMAT = "${FEATURE_FLAGS_FILE_NAME}_%s"
    }
}
