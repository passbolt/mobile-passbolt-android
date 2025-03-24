package com.passbolt.mobile.android.mappers

import com.passbolt.mobile.android.jsonmodel.delegates.TotpSecret
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordAndDescription
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordDescriptionTotp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordString
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.Totp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5Default
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5DefaultWithTotp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5PasswordString
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5TotpStandalone
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.ResourceFormUiModel
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Metadata.DESCRIPTION
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Secret.SECURE_NOTE
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Secret.TOTP
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

    fun map(contentType: ContentType): ResourceFormUiModel =
        ResourceFormUiModel(
            leadingContentType = if (contentType in setOf(Totp, V5TotpStandalone)) {
                LeadingContentType.TOTP
            } else {
                LeadingContentType.PASSWORD
            },
            supportedAdditionalSecrets = when (contentType) {
                PasswordString, V5PasswordString -> emptyList()
                PasswordAndDescription, V5Default -> listOf(SECURE_NOTE, TOTP)
                PasswordDescriptionTotp, V5DefaultWithTotp -> listOf(SECURE_NOTE, TOTP)
                Totp, V5TotpStandalone -> listOf(SECURE_NOTE)
            },
            supportedMetadata = when (contentType) {
                PasswordString, V5PasswordString -> listOf(DESCRIPTION)
                PasswordAndDescription, V5Default -> emptyList()
                PasswordDescriptionTotp, V5DefaultWithTotp -> emptyList()
                Totp, V5TotpStandalone -> emptyList()
            }
        )

    fun mapToUiModel(totp: TotpSecret?, issuer: String): TotpUiModel =
        totp?.let {
            TotpUiModel(
                secret = it.key,
                issuer = issuer,
                expiry = it.period.toString(),
                length = it.digits.toString(),
                algorithm = it.algorithm
            )
        } ?: TotpUiModel.emptyWithDefaults(issuer)

    fun mapToJsonModel(totpUiModel: TotpUiModel?): TotpSecret? =
        totpUiModel?.let {
            TotpSecret(
                key = it.secret,
                period = it.expiry.toLong(),
                digits = it.length.toInt(),
                algorithm = it.algorithm
            )
        }
}
