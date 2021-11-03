package com.passbolt.mobile.android.feature.autofill.autofill

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.view.View
import androidx.annotation.ArrayRes
import com.passbolt.mobile.android.feature.autofill.R
import java.util.Locale

class AutofillHintsFactory(
    private val resources: Resources,
    private val appContext: Context
) {

    fun getHintValues(field: AutofillField) = when (field) {
        AutofillField.USERNAME -> getLocalizedOrEnglishValues(R.array.username_hint_values) +
                View.AUTOFILL_HINT_USERNAME
        AutofillField.PASSWORD -> getLocalizedOrEnglishValues(R.array.password_hint_values) +
                View.AUTOFILL_HINT_PASSWORD
    }

    private fun getLocalizedOrEnglishValues(@ArrayRes stringArrayResId: Int) =
        getLocalized(stringArrayResId) + getEnglish(stringArrayResId)

    private fun getEnglish(@ArrayRes stringArrayResId: Int): Array<String> {
        val config = Configuration(appContext.resources.configuration)
        config.setLocale(Locale.ENGLISH)
        return appContext.createConfigurationContext(config).resources.getStringArray(stringArrayResId)
    }

    private fun getLocalized(@ArrayRes stringArrayResId: Int) =
        resources.getStringArray(stringArrayResId)
}
