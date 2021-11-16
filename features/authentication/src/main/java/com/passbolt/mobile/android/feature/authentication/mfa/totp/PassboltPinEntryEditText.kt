package com.passbolt.mobile.android.feature.authentication.mfa.totp

import android.content.Context
import android.util.AttributeSet
import com.alimuzaffar.lib.pin.PinEntryEditText

class PassboltPinEntryEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : PinEntryEditText(context, attrs, defStyle) {

    fun setCustomTextColor(color: Int) {
        mCharPaint.color = color
        mLastCharPaint.color = color
        invalidate()
    }
}
