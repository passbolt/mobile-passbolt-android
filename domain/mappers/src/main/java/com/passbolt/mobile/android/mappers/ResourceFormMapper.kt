package com.passbolt.mobile.android.mappers

import com.passbolt.mobile.android.jsonmodel.delegates.TotpSecret
import com.passbolt.mobile.android.ui.PasswordUiModel
import com.passbolt.mobile.android.ui.TotpUiModel

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
class ResourceFormMapper {
    fun mapToUiModel(
        totp: TotpSecret?,
        issuer: String,
    ): TotpUiModel =
        totp?.let {
            TotpUiModel(
                secret = it.key,
                issuer = issuer,
                expiry = it.period.toString(),
                length = it.digits.toString(),
                algorithm = it.algorithm,
            )
        } ?: TotpUiModel.emptyWithDefaults(issuer)

    fun mapToJsonModel(totpUiModel: TotpUiModel?): TotpSecret? =
        totpUiModel?.let {
            TotpSecret(
                key = totpUiModel.secret,
                period = totpUiModel.expiry.toLong(),
                digits = totpUiModel.length.toInt(),
                algorithm = totpUiModel.algorithm,
            )
        }

    fun mapToUiModel(
        password: String,
        mainUri: String,
        username: String,
    ) = PasswordUiModel(
        password = password,
        mainUri = mainUri,
        username = username,
    )
}
