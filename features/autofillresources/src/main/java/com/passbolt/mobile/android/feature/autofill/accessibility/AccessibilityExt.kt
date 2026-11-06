package com.passbolt.mobile.android.feature.autofill.accessibility

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes

internal fun View.findViewByContentDescription(
    context: Context,
    @StringRes resId: Int,
): View? {
    val description = context.getString(resId)

    fun View.find(): View? {
        if (contentDescription?.toString() == description) return this
        if (this is ViewGroup) {
            for (i in 0 until childCount) {
                getChildAt(i).find()?.let { return it }
            }
        }
        return null
    }
    return find()
}
