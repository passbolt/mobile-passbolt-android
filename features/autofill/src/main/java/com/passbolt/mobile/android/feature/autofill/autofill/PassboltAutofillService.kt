package com.passbolt.mobile.android.feature.autofill.autofill

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AutofillMode
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
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

@SuppressLint("UnspecifiedImmutableFlag")
class PassboltAutofillService : AutofillService(), KoinComponent {

    private val assistStructureParser: AssistStructureParser by inject()
    private val fillableInputsFinder: FillableInputsFinder by inject()
    private val remoteViewsFactory: RemoteViewsFactory by inject()

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        Timber.d("Received fill request")
        runCatching {
            val parsedAutofillStructures = assistStructureParser.parse(
                request.fillContexts.last().structure
            ).also {
                Timber.d("Parsed domain: ${it.domain}")
            }

            val autofillableViews = arrayOf(
                findAutofillableView(AutofillField.USERNAME, parsedAutofillStructures.structures),
                findAutofillableView(AutofillField.PASSWORD, parsedAutofillStructures.structures)
            )

            // autofillable views not found
            if (autofillableViews.all { it == null }) {
                Timber.d("Did not find any autofillable views")
                null
            } else {
                Timber.d("Showing authentication prompt")
                FillResponse.Builder()
                    .setAuthentication(
                        autofillableViews.filterNotNull().map { it.id }.toTypedArray(),
                        autofillResourcesPendingIntent(parsedAutofillStructures.domain).intentSender,
                        remoteViewsFactory.getAutofillSelectDropdown(packageName)
                    )
                    .build()
            }
        }
            .onFailure {
                Timber.e(it)
                callback.onFailure(it.message)
            }
            .onSuccess {
                callback.onSuccess(it)
            }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        // save is currently not supported
    }

    private fun findAutofillableView(field: AutofillField, autofillStructure: Set<ParsedStructure>) =
        fillableInputsFinder.findStructureForAutofillFields(field, autofillStructure)

    private fun autofillResourcesPendingIntent(uri: String?): PendingIntent = PendingIntent.getActivity(
        applicationContext,
        AUTOFILL_RESOURCES_REQUEST_CODE,
        ActivityIntents.autofill(this, AutofillMode.AUTOFILL.name, uri),
        PendingIntent.FLAG_CANCEL_CURRENT
    )

    private companion object {
        private const val AUTOFILL_RESOURCES_REQUEST_CODE = 1001
    }
}
