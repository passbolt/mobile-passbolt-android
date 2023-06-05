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
import com.passbolt.mobile.android.common.validation.RequiredIntInInclusiveRangeValidation
import com.passbolt.mobile.android.common.validation.RequiredStringInSetValidation
import com.passbolt.mobile.android.common.validation.RequiredStringLengthValidation
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.DecryptedSecret

class TotpSecretValidation {

    fun invoke(secret: DecryptedSecret.StandaloneTotp): Boolean {
        val validationResults = listOf(
            RequiredStringInSetValidation().invoke(
                secret.totp.algorithm,
                TOTP_ALGORITHM_ALLOWED_VALUES
            ),
            RequiredStringLengthValidation().invoke(
                secret.totp.key,
                TOTP_KEY_MIN_LENGTH,
                TOTP_KEY_MAX_LENGTH
            ),
            RequiredIntInInclusiveRangeValidation().invoke(
                secret.totp.digits,
                TOTP_DIGITS_INCLUSIVE_MIN,
                TOTP_DIGITS_INCLUSIVE_MAX
            )
        )

        return validationResults.all { it }
    }

    @VisibleForTesting
    companion object {
        val TOTP_ALGORITHM_ALLOWED_VALUES = hashSetOf("SHA1", "SHA256", "SHA512")
        const val TOTP_KEY_MIN_LENGTH = 0
        const val TOTP_KEY_MAX_LENGTH = 1024
        const val TOTP_DIGITS_INCLUSIVE_MIN = 6
        const val TOTP_DIGITS_INCLUSIVE_MAX = 8
    }
}
