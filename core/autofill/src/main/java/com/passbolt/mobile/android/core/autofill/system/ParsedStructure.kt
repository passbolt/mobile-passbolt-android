package com.passbolt.mobile.android.core.autofill.system

import android.view.autofill.AutofillId

data class ParsedStructures(
    val domain: String? = null,
    val packageId: String? = null,
    val structures: Set<ParsedStructure>,
)

data class ParsedStructure(
    var id: AutofillId,
    val autofillHints: List<String>? = null,
    val inputType: Int? = null,
)
