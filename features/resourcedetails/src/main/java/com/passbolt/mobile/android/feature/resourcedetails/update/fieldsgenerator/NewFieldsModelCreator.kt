package com.passbolt.mobile.android.feature.resourcedetails.update.fieldsgenerator

import com.passbolt.mobile.android.feature.resourcedetails.update.ResourceValue
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordAndDescription
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordDescriptionTotp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordString
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.Totp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5Default
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5DefaultWithTotp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5PasswordString
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5TotpStandalone
import com.passbolt.mobile.android.ui.ResourceField

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
class NewFieldsModelCreator(
    private val resourceFieldsComparator: ResourceFieldsComparator
) {

    fun create(contentType: ContentType): List<ResourceValue> {
        return when (contentType) {
            is PasswordString, V5PasswordString -> {
                passwordStringFields()
            }
            is PasswordAndDescription, V5Default -> {
                passwordAndDescriptionFields()
            }
            is Totp, V5TotpStandalone -> {
                throw IllegalArgumentException("Totp content type is not supported")
            }
            is PasswordDescriptionTotp, V5DefaultWithTotp -> {
                passwordWithTotp()
            }
        }
            .sortedWith(resourceFieldsComparator)
            .map { ResourceValue(it) }
    }

    private fun passwordAndDescriptionFields() = listOf(
        ResourceField(
            name = "name",
            isSecret = false,
            maxLength = 255,
            isRequired = true
        ),
        ResourceField(
            name = "username",
            isSecret = false,
            maxLength = 255,
            isRequired = false
        ),
        ResourceField(
            name = "uri",
            isSecret = false,
            maxLength = 1024,
            isRequired = false
        ),
        ResourceField(
            name = "description",
            isSecret = true,
            maxLength = 10000,
            isRequired = false
        ),
        ResourceField(
            name = "password",
            isSecret = true,
            maxLength = 4096,
            isRequired = true
        )
    )

    private fun passwordWithTotp() = listOf(
        ResourceField(
            name = "name",
            isSecret = false,
            maxLength = 255,
            isRequired = true
        ),
        ResourceField(
            name = "username",
            isSecret = false,
            maxLength = 255,
            isRequired = false
        ),
        ResourceField(
            name = "uri",
            isSecret = false,
            maxLength = 1024,
            isRequired = false
        ),
        ResourceField(
            name = "description",
            isSecret = true,
            maxLength = 10000,
            isRequired = false
        ),
        ResourceField(
            name = "password",
            isSecret = true,
            maxLength = 4096,
            isRequired = true
        )
    )

    private fun passwordStringFields() = listOf(
        ResourceField(
            name = "name",
            isSecret = false,
            maxLength = 255,
            isRequired = true
        ),
        ResourceField(
            name = "username",
            isSecret = false,
            maxLength = 255,
            isRequired = false
        ),
        ResourceField(
            name = "uri",
            isSecret = false,
            maxLength = 1024,
            isRequired = false
        ),
        ResourceField(
            name = "description",
            isSecret = false,
            maxLength = 10000,
            isRequired = false
        ),
        ResourceField(
            name = "secret",
            isSecret = true,
            maxLength = 4096,
            isRequired = true
        )
    )
}
