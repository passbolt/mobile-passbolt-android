package com.passbolt.mobile.android.core.autofill.system

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.view.View
import androidx.annotation.ArrayRes
import java.util.Locale
import com.passbolt.mobile.android.core.localization.R as LocalizationR

class AutofillHintsFactory(
    private val resources: Resources,
    private val appContext: Context
) {

    fun getHintValues(field: AutofillField) = when (field) {
        AutofillField.USERNAME -> getLocalizedOrEnglishValues(LocalizationR.array.username_hint_values) +
                View.AUTOFILL_HINT_USERNAME
        AutofillField.PASSWORD -> getLocalizedOrEnglishValues(LocalizationR.array.password_hint_values) +
                View.AUTOFILL_HINT_PASSWORD
    }

    private fun getLocalizedOrEnglishValues(@ArrayRes stringArrayResId: Int) =
        getLocalized(stringArrayResId) + getEnglish(stringArrayResId)

    @SuppressLint("AppBundleLocaleChanges")
    private fun getEnglish(@ArrayRes stringArrayResId: Int): Array<String> {
        val config = Configuration(appContext.resources.configuration)
        config.setLocale(Locale.ENGLISH)
        return appContext.createConfigurationContext(config).resources.getStringArray(stringArrayResId)
    }

    private fun getLocalized(@ArrayRes stringArrayResId: Int) =
        resources.getStringArray(stringArrayResId)
}
