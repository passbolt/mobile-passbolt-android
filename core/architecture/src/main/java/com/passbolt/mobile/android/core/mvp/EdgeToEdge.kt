package com.passbolt.mobile.android.core.mvp

import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.passbolt.mobile.android.core.mvp.EdgeToEdge.Inset.BOTTOM
import com.passbolt.mobile.android.core.mvp.EdgeToEdge.Inset.TOP

object EdgeToEdge {
    // to be used on dialogs and bottom sheets
    fun addEdgeToEdgeBottomPadding(
        window: Window,
        v: View,
        insetsToApply: List<Inset> = listOf(BOTTOM),
    ) {
        window.decorView.let { decorView ->
            decorView.post {
                val insets = ViewCompat.getRootWindowInsets(decorView)
                if (insets != null) {
                    val params = v.layoutParams as ViewGroup.MarginLayoutParams
                    val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    if (insetsToApply.contains(TOP)) params.topMargin = systemInsets.top
                    if (insetsToApply.contains(BOTTOM)) params.bottomMargin = systemInsets.bottom
                    v.layoutParams = params
                }
            }
        }
    }

    enum class Inset {
        TOP,
        BOTTOM,
    }
}
