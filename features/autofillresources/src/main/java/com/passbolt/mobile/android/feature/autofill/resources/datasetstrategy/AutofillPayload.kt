package com.passbolt.mobile.android.feature.autofill.resources.datasetstrategy

data class AutofillPayload(
    val username: String?,
    val password: String?,
    val totpCode: String?,
    val uri: String?,
    /**
     * TOTP code to place in the clipboard right before the dataset is returned
     * (the "copy TOTP automatically after autofill" setting). Null when the
     * setting is off, the resource has no TOTP, or the form already had a TOTP
     * field that got filled directly.
     */
    val totpCodeToCopy: String? = null,
)
