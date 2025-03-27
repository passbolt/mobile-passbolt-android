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

package com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser

import com.passbolt.mobile.android.jsonmodel.JsonModel
import com.passbolt.mobile.android.jsonmodel.delegates.RootRelativeJsonPathNullableStringDelegate
import com.passbolt.mobile.android.jsonmodel.delegates.RootRelativeJsonPathStringDelegate
import com.passbolt.mobile.android.jsonmodel.delegates.RootRelativeJsonPathTotpDelegate
import com.passbolt.mobile.android.jsonmodel.delegates.StringDelegate
import com.passbolt.mobile.android.jsonmodel.delegates.TotpSecret
import com.passbolt.mobile.android.ui.OtpParseResult

class SecretJsonModel(override var json: String) : JsonModel {

    var objectType: String? by RootRelativeJsonPathNullableStringDelegate(jsonPath = "object_type")

    var resourceTypeId: String? by RootRelativeJsonPathNullableStringDelegate(jsonPath = "resource_type_id")

    // simple-password is just a string (not a valid JSON)
    var password: String by StringDelegate()

    var secret: String by RootRelativeJsonPathStringDelegate(jsonPath = "password")

    var description: String? by RootRelativeJsonPathNullableStringDelegate(jsonPath = "description")

    var totp: TotpSecret? by RootRelativeJsonPathTotpDelegate(jsonPath = "totp")

    companion object {

        fun emptyPassword(): SecretJsonModel = SecretJsonModel(
            """
                {
                    "password": ""
                }
            """
                .trimIndent()
        )

        fun emptyPasswordWithTotp(): SecretJsonModel = SecretJsonModel(
            """
                {
                    "password": "",
                    "totp": {
                        "secret_key": "",
                        "period": ${OtpParseResult.OtpQr.TotpQr.DEFAULT_PERIOD_SECONDS},
                        "digits": ${OtpParseResult.OtpQr.TotpQr.DEFAULT_DIGITS},
                        "algorithm": ${OtpParseResult.OtpQr.Algorithm.DEFAULT.name}
                    }
                }
            """
                .trimIndent()
        )

        fun emptyTotp(): SecretJsonModel = SecretJsonModel(
            """
                {
                    "totp": {
                        "secret_key": "",
                        "period": ${OtpParseResult.OtpQr.TotpQr.DEFAULT_PERIOD_SECONDS},
                        "digits": ${OtpParseResult.OtpQr.TotpQr.DEFAULT_DIGITS},
                        "algorithm": ${OtpParseResult.OtpQr.Algorithm.DEFAULT.name}
                    }
                }
            """
                .trimIndent()
        )
    }
}
