package com.passbolt.mobile.android.core.ui.textinputfield

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputLayout

class NoMinHeightTextInputLayout
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : TextInputLayout(context, attrs) {
        var minLines = -1

        // there is a bug in TextInputLayout that forces a minimum height on the EditText
        // after at least one set error call; then when deleting lines the view does not shrink
        // last test on material 1.14.0-alpha03
        // bug: https://github.com/material-components/material-components-android/issues/4146
        override fun onLayout(
            changed: Boolean,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
        ) {
            super.onLayout(changed, left, top, right, bottom)

            editText?.let { et ->
                // Undo the forced min height every layout pass
                et.minimumHeight = 0
                et.minHeight = 0

                if (minLines != -1 && minLines != et.minLines) {
                    et.minLines = minLines
                }
            }
        }
    }
