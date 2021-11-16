package com.passbolt.mobile.android.feature.autofill.resources.datasetstrategy

import com.passbolt.mobile.android.feature.autofill.accessibility.AccessibilityCommunicator
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesContract

class ReturnAccessibilityDataset(
    override var view: AutofillResourcesContract.View?
) : ReturnAutofillDatasetStrategy {

    override fun returnDataset(username: String, password: String, uri: String?) {
        AccessibilityCommunicator.lastCredentials = AccessibilityCommunicator.Credentials(
            username,
            password,
            uri
        )
        activeView.finishAutofill()
    }
}
