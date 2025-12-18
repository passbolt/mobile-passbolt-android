package com.passbolt.mobile.android.ui

import android.os.Parcelable
import android.view.autofill.AutofillId
import kotlinx.parcelize.Parcelize

data class ParsedStructures(
    val structures: Set<ParsedStructure>,
) {
    val hasDifferentDomains: Boolean
        get() = structures.mapNotNull { it.domain }.toSet().size > 1
}

@Parcelize
data class ParsedStructure(
    var id: AutofillId,
    val autofillHints: List<String>? = null,
    val inputType: Int? = null,
    val domain: String? = null,
    val packageId: String? = null,
) : Parcelable
