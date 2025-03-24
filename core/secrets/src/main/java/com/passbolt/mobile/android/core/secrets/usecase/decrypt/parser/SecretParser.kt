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

import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.serializers.gson.validation.JsonSchemaValidationRunner
import com.passbolt.mobile.android.serializers.validationwrapper.PlainSecretValidationWrapper
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.DecryptedSecretOrError
import timber.log.Timber
import java.util.UUID

class SecretParser(
    private val secretValidationRunner: JsonSchemaValidationRunner,
    private val resourceTypeIdToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider
) {

    suspend fun parseSecret(
        resourceTypeId: String,
        decryptedSecret: ByteArray
    ): DecryptedSecretOrError<SecretJsonModel> {

        val slug = resourceTypeIdToSlugMappingProvider
            .provideMappingForSelectedAccount()[UUID.fromString(resourceTypeId)]

        return try {
            // in case of simple password the backend returns a string (not a json string)
            val plainSecret = String(decryptedSecret)
            if (secretValidationRunner.isSecretValid(
                    PlainSecretValidationWrapper(plainSecret, ContentType.fromSlug(slug!!)).validationPlainSecret,
                    slug
                )
            ) {
                val parsedSecret = SecretJsonModel(plainSecret)
                DecryptedSecretOrError.DecryptedSecret(parsedSecret)
            } else {
                val errorMessage = "Invalid secret in $slug resource type"
                Timber.e(errorMessage)
                DecryptedSecretOrError.Error.ValidationError(errorMessage)
            }
        } catch (exception: Exception) {
            val errorMessage = "Error during secret parsing: ${exception.message}"
            Timber.e(exception, errorMessage)
            DecryptedSecretOrError.Error.ParsingError(errorMessage)
        }
    }
}
