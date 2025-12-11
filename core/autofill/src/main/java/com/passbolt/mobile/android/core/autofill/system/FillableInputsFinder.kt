package com.passbolt.mobile.android.core.autofill.system

import android.annotation.SuppressLint
import com.passbolt.mobile.android.core.autofill.BuildConfig
import com.passbolt.mobile.android.ui.ParsedStructure
import timber.log.Timber

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
class FillableInputsFinder(
    private val autofillHintsFactory: AutofillHintsFactory,
) {
    // TODO consider other methods apart from analyzing hints
    @SuppressLint("BinaryOperationInTimber")
    fun findStructureForAutofillFields(
        field: AutofillField,
        autofillParsedStructure: Set<ParsedStructure>,
    ): ParsedStructure? {
        val hintValues = autofillHintsFactory.getHintValues(field)
        return autofillParsedStructure
            .asSequence()
            .filter { !it.autofillHints.isNullOrEmpty() }
            .firstOrNull { parsedStructure ->
                val autofillMatching = isAutofillMatching(parsedStructure, hintValues)
                if (BuildConfig.DEBUG && autofillMatching) {
                    Timber.d(
                        "Autofill matching structure found for field. " +
                            "\nField hint values: %s " +
                            "\nStructure hints: %s " +
                            "\nWeb domain: %s",
                        hintValues.joinToString(separator = ","),
                        parsedStructure.autofillHints!!.joinToString(separator = ","),
                        parsedStructure.domain,
                    )
                }

                val hasDomainSet = hasDomainSet(parsedStructure)
                if (BuildConfig.DEBUG) {
                    Timber.d("Checking if found structure has domain set: %s", hasDomainSet)
                }

                autofillMatching && hasDomainSet
            }
    }

    private fun hasDomainSet(parsedStructure: ParsedStructure) = !parsedStructure.domain.isNullOrEmpty()

    private fun isAutofillMatching(
        parsedStructure: ParsedStructure,
        hintValues: Array<String>,
    ): Boolean =
        parsedStructure.autofillHints!!.any { structureHint ->
            // filtered above
            hintValues.any {
                val result = structureHint.contains(it, ignoreCase = true)
                if (BuildConfig.DEBUG) {
                    Timber.d(
                        "Marking input as fillable. \nHint values: %s \nStructure hint: %s",
                        hintValues.joinToString(separator = ","),
                        structureHint,
                    )
                }
                result
            }
        }
}
