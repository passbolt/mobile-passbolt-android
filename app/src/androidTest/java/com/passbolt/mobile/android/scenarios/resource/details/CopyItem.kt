package com.passbolt.mobile.android.scenarios.resource.details

import androidx.annotation.StringRes
import com.passbolt.mobile.android.core.localization.R

enum class CopyItem(
    @param:StringRes val stringResId: Int,
) {
    COPY_URI(R.string.more_copy_uri),
    COPY_PASSWORD(R.string.more_copy_password),
    COPY_METADATA_DESCRIPTION(R.string.more_copy_metadata_desc),
    COPY_NOTE(R.string.more_copy_note),
    COPY_USERNAME(R.string.more_copy_username),
}
