package com.passbolt.mobile.android.serializers.gson.validation.raw

import com.passbolt.mobile.android.serializers.gson.validation.MaxLength
import com.passbolt.mobile.android.serializers.gson.validation.MinLength

class OptionalStringFieldLengthValidation : (String?, MinLength, MaxLength) -> Boolean {

    override fun invoke(field: String?, minLength: MinLength, maxLength: MaxLength) =
        if (field != null) {
            field.length in minLength..maxLength
        } else {
            // when optional fields is missing it passes validation
            true
        }
}
