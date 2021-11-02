package com.passbolt.mobile.android.feature.autofill.autofill

import android.content.res.Resources
import android.view.View
import com.passbolt.mobile.android.feature.autofill.R

class AutofillHintsFactory(
    private val resources: Resources
) {

    fun getHintValues(field: AutofillField) = when (field) {
        AutofillField.USERNAME -> resources.getStringArray(R.array.username_hint_values) + View.AUTOFILL_HINT_USERNAME
        AutofillField.PASSWORD -> resources.getStringArray(R.array.password_hint_values) + View.AUTOFILL_HINT_PASSWORD
    }
}
