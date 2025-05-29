package com.passbolt.mobile.android.common.validation

class OptionalStringLengthValidation : (String?, MinLength, MaxLength) -> Boolean {
    override fun invoke(
        text: String?,
        minLength: MinLength,
        maxLength: MaxLength,
    ) = if (text != null) {
        text.length in minLength..maxLength
    } else {
        // when optional fields is missing it passes validation
        true
    }
}
