package com.passbolt.mobile.android.core.mvp.viewbinding

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

open class NoLimitsBottomSheetDialogFragment : BottomSheetDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener {
            dialog.window?.apply {
                addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            }
        }

        return dialog
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Set up insets listener to handle navigation bar padding
        ViewCompat.setOnApplyWindowInsetsListener(view) { view, insets ->
            val navigationBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updatePadding(bottom = view.paddingBottom + navigationBarInsets.bottom)
            insets
        }
    }
}
