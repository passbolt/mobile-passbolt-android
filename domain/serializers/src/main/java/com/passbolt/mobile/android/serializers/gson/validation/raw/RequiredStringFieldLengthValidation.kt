package com.passbolt.mobile.android.serializers.gson.validation.raw

import com.passbolt.mobile.android.serializers.gson.validation.MaxLength
import com.passbolt.mobile.android.serializers.gson.validation.MinLength

class RequiredStringFieldLengthValidation : (String, MinLength, MaxLength) -> Boolean {

    override fun invoke(field: String, minLength: MinLength, maxLength: MaxLength) =
        field.length in minLength..maxLength
}
