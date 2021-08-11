package com.passbolt.mobile.android.feature.autofill.resources

import android.app.assist.AssistStructure
import android.service.autofill.Dataset
import android.view.View
import android.view.autofill.AutofillValue
import com.passbolt.mobile.android.feature.autofill.StructureParser
import com.passbolt.mobile.android.feature.autofill.service.ParsedStructure

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
class AutofillResourcesPresenter(
    private val structureParser: StructureParser
) : AutofillResourcesContract.Presenter {

    override var view: AutofillResourcesContract.View? = null
    private lateinit var parsedStructure: Set<ParsedStructure>

    override fun returnClick() {
        val usernameParsedAssistStructure = structureParser.extractHint(View.AUTOFILL_HINT_USERNAME, parsedStructure)
        val passwordParsedAssistStructure = structureParser.extractHint(View.AUTOFILL_HINT_PASSWORD, parsedStructure)

        if (passwordParsedAssistStructure == null || usernameParsedAssistStructure == null) {
            view?.navigateBack()
            return
        }

        val mockedDataSet = mockDataSet(usernameParsedAssistStructure, passwordParsedAssistStructure)
        view?.returnData(mockedDataSet)
    }

    override fun argsReceived(structure: AssistStructure) {
        parsedStructure = structureParser.parse(structure)
    }

    // TODO
    private fun mockDataSet(username: ParsedStructure, password: ParsedStructure) =
        Dataset.Builder()
            .setValue(
                username.id,
                AutofillValue.forText("Mocked usernamed")
            )
            .setValue(
                password.id,
                AutofillValue.forText("Mocked password")
            )
            .build()
}
