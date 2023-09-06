package com.passbolt.mobile.android.serializers.gson.validation

import androidx.annotation.VisibleForTesting
import com.passbolt.mobile.android.common.validation.OptionalStringLengthValidation
import com.passbolt.mobile.android.common.validation.RequiredStringLengthValidation
import com.passbolt.mobile.android.dto.response.ResourceResponseDto

class PasswordStringResourceValidation : ResourceValidation {

    override fun invoke(resource: ResourceResponseDto): Boolean {
        val validationResults = listOf(
            RequiredStringLengthValidation().invoke(
                resource.name,
                PASSWORD_STRING_NAME_MIN_LENGTH,
                PASSWORD_STRING_NAME_MAX_LENGTH
            ),
            OptionalStringLengthValidation().invoke(
                resource.username,
                PASSWORD_STRING_USERNAME_MIN_LENGTH,
                PASSWORD_STRING_USERNAME_MAX_LENGTH
            ),
            OptionalStringLengthValidation().invoke(
                resource.uri,
                PASSWORD_STRING_URI_MIN_LENGTH,
                PASSWORD_STRING_URI_MAX_LENGTH
            ),
            OptionalStringLengthValidation().invoke(
                resource.description,
                PASSWORD_STRING_DESCRIPTION_MIN_LENGTH,
                PASSWORD_STRING_DESCRIPTION_MAX_LENGTH
            )
        )

        return validationResults.all { it }
    }

    @VisibleForTesting
    companion object {
        const val PASSWORD_STRING_NAME_MIN_LENGTH = 0
        const val PASSWORD_STRING_NAME_MAX_LENGTH = 255
        const val PASSWORD_STRING_USERNAME_MIN_LENGTH = 0
        const val PASSWORD_STRING_USERNAME_MAX_LENGTH = 255
        const val PASSWORD_STRING_URI_MIN_LENGTH = 0
        const val PASSWORD_STRING_URI_MAX_LENGTH = 1024
        const val PASSWORD_STRING_DESCRIPTION_MIN_LENGTH = 0
        const val PASSWORD_STRING_DESCRIPTION_MAX_LENGTH = 10_000
    }
}
