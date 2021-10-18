package com.passbolt.mobile.android.feature.authentication.auth.uistrategy

import android.app.Activity
import android.content.Intent
import androidx.navigation.fragment.findNavController
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.R
import com.passbolt.mobile.android.feature.authentication.auth.AuthFragment

class StartupAuthStrategy(
    override var authFragment: AuthFragment?,
    override val appContext: AppContext
) : AuthStrategy {

    override fun title() =
        activeAuthFragment.getString(R.string.auth_sign_in)

    override fun navigateBack() {
        activeAuthFragment.findNavController().popBackStack()
    }

    override fun authSuccess() {
        activeAuthFragment.apply {
            requireActivity().setResult(Activity.RESULT_OK)
            val startIntent = when (appContext) {
                AppContext.APP -> ActivityIntents.home(requireActivity())
                AppContext.AUTOFILL -> ActivityIntents.autofill(requireActivity()).apply {
                    flags =
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            }
            requireActivity().finish()
            startActivity(startIntent)
        }
    }

    override fun showLeaveConfirmationDialog(): Boolean = false
}
