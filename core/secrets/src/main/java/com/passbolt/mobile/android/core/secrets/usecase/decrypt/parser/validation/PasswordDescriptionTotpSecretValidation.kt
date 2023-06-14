/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

package com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.validation

import androidx.annotation.VisibleForTesting
import com.passbolt.mobile.android.common.validation.OptionalStringLengthValidation
import com.passbolt.mobile.android.common.validation.RequiredIntInInclusiveRangeValidation
import com.passbolt.mobile.android.common.validation.RequiredStringInSetValidation
import com.passbolt.mobile.android.common.validation.RequiredStringLengthValidation
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.DecryptedSecret

class PasswordDescriptionTotpSecretValidation {

    fun invoke(secret: DecryptedSecret.PasswordDescriptionTotp): Boolean {
        val validationResults = listOf(
            RequiredStringLengthValidation().invoke(
                secret.password,
                PASSWORD_DESCRIPTION_TOTP_PASSWORD_MIN_LENGTH,
                PASSWORD_DESCRIPTION_TOTP_PASSWORD_MAX_LENGTH
            ),
            OptionalStringLengthValidation().invoke(
                secret.description,
                PASSWORD_DESCRIPTION_TOTP_DESCRIPTION_MIN_LENGTH,
                PASSWORD_DESCRIPTION_TOTP_DESCRIPTION_MAX_LENGTH
            ),
            RequiredStringInSetValidation().invoke(
                secret.totp.algorithm,
                PASSWORD_DESCRIPTION_TOTP_TOTP_ALGORITHM_ALLOWED_VALUES
            ),
            RequiredStringLengthValidation().invoke(
                secret.totp.key,
                PASSWORD_DESCRIPTION_TOTP_TOTP_KEY_MIN_LENGTH,
                PASSWORD_DESCRIPTION_TOTP_TOTP_KEY_MAX_LENGTH
            ),
            RequiredIntInInclusiveRangeValidation().invoke(
                secret.totp.digits,
                PASSWORD_DESCRIPTION_TOTP_TOTP_DIGITS_INCLUSIVE_MIN,
                PASSWORD_DESCRIPTION_TOTP_TOTP_DIGITS_INCLUSIVE_MAX
            )
        )

        return validationResults.all { it }
    }

    @VisibleForTesting
    companion object {
        const val PASSWORD_DESCRIPTION_TOTP_PASSWORD_MIN_LENGTH = 0
        const val PASSWORD_DESCRIPTION_TOTP_PASSWORD_MAX_LENGTH = 4096
        const val PASSWORD_DESCRIPTION_TOTP_DESCRIPTION_MIN_LENGTH = 0
        const val PASSWORD_DESCRIPTION_TOTP_DESCRIPTION_MAX_LENGTH = 10_000
        val PASSWORD_DESCRIPTION_TOTP_TOTP_ALGORITHM_ALLOWED_VALUES = hashSetOf("SHA1", "SHA256", "SHA512")
        const val PASSWORD_DESCRIPTION_TOTP_TOTP_KEY_MIN_LENGTH = 0
        const val PASSWORD_DESCRIPTION_TOTP_TOTP_KEY_MAX_LENGTH = 1024
        const val PASSWORD_DESCRIPTION_TOTP_TOTP_DIGITS_INCLUSIVE_MIN = 6
        const val PASSWORD_DESCRIPTION_TOTP_TOTP_DIGITS_INCLUSIVE_MAX = 8
    }
}
