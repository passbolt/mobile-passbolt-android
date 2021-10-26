package com.passbolt.mobile.android.feature.autofill.autofill

import android.view.autofill.AutofillId

data class ParsedStructure(
    var id: AutofillId,
    val hints: List<String>,
    val domain: String?,
    val packageName: String?
)
