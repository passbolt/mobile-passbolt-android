package com.passbolt.mobile.android.feature.autofill.resources

import com.passbolt.mobile.android.ui.ResourceModel

sealed interface AutofillResourcesIntent {
    data object UserAuthenticated : AutofillResourcesIntent

    data class SelectAutofillItem(
        val resourceModel: ResourceModel,
    ) : AutofillResourcesIntent

    data class NewResourceCreated(
        val resourceId: String,
    ) : AutofillResourcesIntent
}
