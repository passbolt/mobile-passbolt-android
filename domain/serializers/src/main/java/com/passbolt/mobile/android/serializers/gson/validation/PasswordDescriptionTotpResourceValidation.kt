package com.passbolt.mobile.android.serializers.gson.validation

import androidx.annotation.VisibleForTesting
import com.passbolt.mobile.android.common.validation.OptionalStringLengthValidation
import com.passbolt.mobile.android.common.validation.RequiredStringLengthValidation
import com.passbolt.mobile.android.dto.response.ResourceResponseDto

class PasswordDescriptionTotpResourceValidation : ResourceValidation {

    override fun invoke(resource: ResourceResponseDto): Boolean {
        val validationResults = listOf(
            RequiredStringLengthValidation().invoke(
                resource.name,
                PASSWORD_DESCRIPTION_TOTP_NAME_MIN_LENGTH,
                PASSWORD_DESCRIPTION_TOTP_NAME_MAX_LENGTH
            ),
            OptionalStringLengthValidation().invoke(
                resource.username,
                PASSWORD_DESCRIPTION_TOTP_USERNAME_MIN_LENGTH,
                PASSWORD_DESCRIPTION_TOTP_USERNAME_MAX_LENGTH
            ),
            OptionalStringLengthValidation().invoke(
                resource.uri,
                PASSWORD_DESCRIPTION_TOTP_URI_MIN_LENGTH,
                PASSWORD_DESCRIPTION_TOTP_URI_MAX_LENGTH
            )
        )

        return validationResults.all { it }
    }

    @VisibleForTesting
    companion object {
        const val PASSWORD_DESCRIPTION_TOTP_NAME_MIN_LENGTH = 0
        const val PASSWORD_DESCRIPTION_TOTP_NAME_MAX_LENGTH = 255
        const val PASSWORD_DESCRIPTION_TOTP_USERNAME_MIN_LENGTH = 0
        const val PASSWORD_DESCRIPTION_TOTP_USERNAME_MAX_LENGTH = 255
        const val PASSWORD_DESCRIPTION_TOTP_URI_MIN_LENGTH = 0
        const val PASSWORD_DESCRIPTION_TOTP_URI_MAX_LENGTH = 1024
    }
}
