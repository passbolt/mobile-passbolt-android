package com.passbolt.mobile.android.feature.autofill.resources.datasetstrategy

interface ReturnAutofillDatasetStrategy {
    fun returnDataset(
        username: String,
        password: String,
        uri: String?,
    )
}
