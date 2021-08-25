package com.passbolt.mobile.android.feature.autofill.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.passbolt.mobile.android.feature.autofill.StructureParser
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesActivity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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
class PassboltAutofillService : AutofillService(), KoinComponent {

    private val structureParser: StructureParser by inject()

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        val structure: AssistStructure = request.fillContexts.last().structure
        val parsedStructure = structureParser.parse(structure)

        val passwordParsedAssistStructure = structureParser.extractHint(View.AUTOFILL_HINT_PASSWORD, parsedStructure)
        val usernameParsedAssistStructure = structureParser.extractHint(View.AUTOFILL_HINT_USERNAME, parsedStructure)

        if (usernameParsedAssistStructure == null || passwordParsedAssistStructure == null) {
            callback.onSuccess(null)
            return
        }

        callback.onSuccess(
            getManualFillResponse(usernameParsedAssistStructure, passwordParsedAssistStructure)
        )
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        // ignored now
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun getManualFillResponse(
        passwordParsedAssistStructure: ParsedStructure,
        usernameParsedAssistStructure: ParsedStructure
    ): FillResponse {
        val manualPresentation = preparePresentation("Tap to manually select data")

        val intent = Intent(applicationContext, AutofillResourcesActivity::class.java)

        val sender = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_CANCEL_CURRENT
        ).intentSender

        return FillResponse.Builder()
            .addDataset(
                Dataset.Builder()
                    .setAuthentication(sender)
                    .setValue(
                        passwordParsedAssistStructure.id,
                        AutofillValue.forText(null),
                        manualPresentation
                    )
                    .setValue(
                        usernameParsedAssistStructure.id,
                        AutofillValue.forText(null),
                        manualPresentation
                    )
                    .build()
            )
            .build()
    }

    private fun preparePresentation(text: String): RemoteViews {
        return RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
            setTextViewText(android.R.id.text1, text)
        }
    }
}

data class ParsedStructure(
    var id: AutofillId,
    val hints: List<String>,
    val domain: String?,
    val packageName: String?
)
