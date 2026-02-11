/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
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

package com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris

import com.passbolt.mobile.android.common.validation.StringMaxLength
import java.util.UUID

internal interface FormValidator<T> {
    fun validateAll(input: T): ValidationResult

    data class ValidationResult(
        val mainUriError: String?,
        val additionalUris: LinkedHashMap<UUID, AdditionalUriItemState>,
        val hasErrors: Boolean,
    )
}

internal data class AdditionalUrisValidationInput(
    val mainUri: String,
    val additionalUris: LinkedHashMap<UUID, AdditionalUriItemState>,
)

internal class AdditionalUrisFormValidator : FormValidator<AdditionalUrisValidationInput> {
    private val uriMaxLengthRule = StringMaxLength(URI_MAX_LENGTH)

    override fun validateAll(input: AdditionalUrisValidationInput): FormValidator.ValidationResult {
        val mainUriError = validateUri(input.mainUri)
        val validatedAdditionalUris = validateAdditionalUris(input.additionalUris)
        val hasErrors = mainUriError != null || validatedAdditionalUris.values.any { it.error != null }

        return FormValidator.ValidationResult(
            mainUriError = mainUriError,
            additionalUris = validatedAdditionalUris,
            hasErrors = hasErrors,
        )
    }

    private fun validateUri(uri: String): String? =
        if (!uriMaxLengthRule.condition(uri)) {
            URI_MAX_LENGTH.toString()
        } else {
            null
        }

    private fun validateAdditionalUris(
        additionalUris: LinkedHashMap<UUID, AdditionalUriItemState>,
    ): LinkedHashMap<UUID, AdditionalUriItemState> {
        val validated = LinkedHashMap<UUID, AdditionalUriItemState>()
        additionalUris.forEach { (uriId, itemState) ->
            val error = validateUri(itemState.uri)
            validated[uriId] = itemState.copy(error = error)
        }
        return validated
    }

    private companion object {
        private const val URI_MAX_LENGTH = 1024
    }
}
