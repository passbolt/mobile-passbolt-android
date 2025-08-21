package com.passbolt.mobile.android.core.mvp

import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

object EdgeToEdge {
    // to be used on dialogs and bottom sheets
    fun addEdgeToEdgeBottomPadding(
        window: Window,
        v: View,
    ) {
        window.decorView.let { decorView ->
            decorView.post {
                val insets = ViewCompat.getRootWindowInsets(decorView)
                if (insets != null) {
                    val params = v.layoutParams as ViewGroup.MarginLayoutParams
                    params.bottomMargin = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                    v.layoutParams = params
                }
            }
        }
    }
}
