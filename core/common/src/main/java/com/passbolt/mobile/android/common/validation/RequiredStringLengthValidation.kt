package com.passbolt.mobile.android.common.validation

class RequiredStringLengthValidation : (String, MinLength, MaxLength) -> Boolean {

    override fun invoke(text: String, minLength: MinLength, maxLength: MaxLength) =
        text.length in minLength..maxLength
}
