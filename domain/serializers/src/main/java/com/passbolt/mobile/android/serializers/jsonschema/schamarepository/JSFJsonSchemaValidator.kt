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

package com.passbolt.mobile.android.serializers.jsonschema.schamarepository

import net.jimblackler.jsonschemafriend.Schema
import net.jimblackler.jsonschemafriend.Validator
import timber.log.Timber

class JSFJsonSchemaValidator(
    private val schemaRepository: JsonSchemaRepository<Schema>,
    private val validator: Validator
) : JsonSchemaValidator {

    override suspend fun isResourceValid(resourceSlug: String, resourceJson: String?) =
        validate(schemaRepository.schemaForResource(resourceSlug), resourceJson, logValidationError = true)

    override suspend fun isSecretValid(resourceSlug: String, secretJson: String?) =
        validate(schemaRepository.schemaForSecret(resourceSlug), secretJson)

    // do not log the error by default as log message contains validated field value
    // which can lead to secret exposure in the internal log file when the secret is invalid
    private fun validate(schema: Schema, json: String?, logValidationError: Boolean = false) =
        try {
            validator.validateJson(schema, json)
            true
        } catch (exception: Exception) {
            if (logValidationError) {
                Timber.e("Validation error message: ${exception.message}")
            }
            false
        }
}
