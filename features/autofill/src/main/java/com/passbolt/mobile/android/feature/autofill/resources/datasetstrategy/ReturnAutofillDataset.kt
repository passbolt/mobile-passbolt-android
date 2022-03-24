package com.passbolt.mobile.android.feature.autofill.resources.datasetstrategy

import android.app.Activity
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import com.passbolt.mobile.android.feature.autofill.autofill.AssistStructureParser
import com.passbolt.mobile.android.feature.autofill.autofill.AutofillField
import com.passbolt.mobile.android.feature.autofill.autofill.FillableInputsFinder
import com.passbolt.mobile.android.feature.autofill.autofill.RemoteViewsFactory
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesContract

class ReturnAutofillDataset(
    override var view: AutofillResourcesContract.View?,
    private val appContext: Context,
    private val assistStructureParser: AssistStructureParser,
    private val fillableInputsFinder: FillableInputsFinder,
    private val remoteViewsFactory: RemoteViewsFactory
) : ReturnAutofillDatasetStrategy {

    override fun returnDataset(username: String, password: String, uri: String?) {
        val structure: AssistStructure = activeView.getAutofillStructure()
        val parsedStructures = assistStructureParser.parse(structure)

        val usernameParsedAssistStructure = fillableInputsFinder.findStructureForAutofillFields(
            AutofillField.USERNAME, parsedStructures.structures
        )
        val passwordParsedAssistStructure = fillableInputsFinder.findStructureForAutofillFields(
            AutofillField.PASSWORD, parsedStructures.structures
        )

        val fillResponse = FillResponse.Builder()
            .addDataset(
                Dataset.Builder()
                    .apply {
                        addDatasetValue(usernameParsedAssistStructure?.id, username)
                        addDatasetValue(passwordParsedAssistStructure?.id, password)
                    }.build()
            ).build()

        val replyIntent = Intent().apply {
            putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, fillResponse)
        }

        view?.setResultAndFinish(Activity.RESULT_OK, replyIntent)
    }

    private fun Dataset.Builder.addDatasetValue(id: AutofillId?, valueText: String) {
        if (id != null) {
            setValue(
                id,
                AutofillValue.forText(valueText),
                remoteViewsFactory.getAutofillFillDropdown(appContext.packageName)
            )
        }
    }
}
