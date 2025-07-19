package com.passbolt.mobile.android.commontest.viewassertions

import android.view.View
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAssertion

class CastedViewAssertion<CAST_TO : View>(
    val assertion: (CAST_TO) -> Boolean,
) : ViewAssertion {
    @Suppress("UNCHECKED_CAST")
    override fun check(
        view: View?,
        noViewFoundException: NoMatchingViewException?,
    ) {
        try {
            assert(assertion(view as CAST_TO))
        } catch (exception: ClassCastException) {
            throw AssertionError("Supplied view (${view?.javaClass?.name}) cannot be casted to used type.")
        }
    }
}
