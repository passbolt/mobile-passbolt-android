package com.passbolt.mobile.android.feature.autofill.resources.datasetstrategy

import com.passbolt.mobile.android.core.autofill.accessibility.AccessibilityCommunicator

class ReturnAccessibilityDataset(
    private val autofillCallback: AutofillCallback,
) : ReturnAutofillDatasetStrategy {
    override fun returnDataset(
        username: String,
        password: String,
        uri: String?,
    ) {
        AccessibilityCommunicator.lastCredentials =
            AccessibilityCommunicator.Credentials(
                username,
                password,
                uri,
            )
        autofillCallback.finishAutofill()
    }
}
