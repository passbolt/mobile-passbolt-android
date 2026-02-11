package com.passbolt.mobile.android.common.autofill

fun interface DetectAutofillConflict {
    operator fun invoke(): Boolean
}
