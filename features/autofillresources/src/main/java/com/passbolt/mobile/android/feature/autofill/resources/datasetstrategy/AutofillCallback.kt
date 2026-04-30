package com.passbolt.mobile.android.feature.autofill.resources.datasetstrategy

import android.app.assist.AssistStructure
import android.content.Intent

interface AutofillCallback {
    fun getAutofillStructure(): AssistStructure

    fun setResultAndFinish(
        result: Int,
        resultIntent: Intent,
    )

    fun finishAutofill()
}
