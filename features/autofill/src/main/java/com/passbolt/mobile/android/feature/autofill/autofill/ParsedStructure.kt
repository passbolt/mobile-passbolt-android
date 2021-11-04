package com.passbolt.mobile.android.feature.autofill.autofill

import android.view.autofill.AutofillId

data class ParsedStructure(
    var id: AutofillId,
    val autofillHints: List<String>? = null,
    val inputType: Int? = null,
    val domain: String? = null,
    val packageName: String? = null
)
