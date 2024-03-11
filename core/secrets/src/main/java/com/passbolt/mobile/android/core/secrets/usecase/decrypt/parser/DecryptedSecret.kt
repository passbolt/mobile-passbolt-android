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

import com.google.gson.annotations.SerializedName
import com.passbolt.mobile.android.delegates.JsonModel
import com.passbolt.mobile.android.delegates.JsonPathDelegate
import com.passbolt.mobile.android.delegates.JsonPathNullableDelegate

class DecryptedSecret(override var json: String) : JsonModel {

    var password: String by JsonPathDelegate(jsonPath = "$")

    var secret: String by JsonPathDelegate(jsonPath = "$.password")
    var description: String? by JsonPathNullableDelegate(jsonPath = "$.description")

    var totpAlgorithm: String by JsonPathDelegate(jsonPath = "$.totp.algorithm")
    var totpKey: String by JsonPathDelegate(jsonPath = "$.totp.secret_key")
    var totpDigits: Int by JsonPathDelegate(jsonPath = "$.totp.digits")
    var totpPeriod: Int? by JsonPathNullableDelegate(jsonPath = "$.totp.period")
}

data class TotpSecret(
    val algorithm: String,
    @SerializedName("secret_key")
    val key: String,
    val digits: Int,
    val period: Long
)

class PasswordWithDescriptionSecret(
    val password: String,
    val description: String?
)

data class PasswordDescriptionTotpSecret(
    val password: String,
    val description: String?,
    val totp: TotpSecret
)
