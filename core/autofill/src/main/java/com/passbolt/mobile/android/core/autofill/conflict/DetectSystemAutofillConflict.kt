package com.passbolt.mobile.android.core.autofill.conflict

import com.passbolt.mobile.android.common.autofill.DetectAutofillConflict
import com.passbolt.mobile.android.core.autofill.AutofillInformationProvider

class DetectSystemAutofillConflict(
    private val autofillInformationProvider: AutofillInformationProvider,
) : DetectAutofillConflict {
    override fun invoke(): Boolean {
        val isAccessibilityAutofillChecked = autofillInformationProvider.isAccessibilityAutofillSetup()
        val isNativeAutofillChecked =
            autofillInformationProvider.isAutofillServiceSupported() &&
                autofillInformationProvider.isPassboltAutofillServiceSet()

        return isAccessibilityAutofillChecked && isNativeAutofillChecked
    }
}
