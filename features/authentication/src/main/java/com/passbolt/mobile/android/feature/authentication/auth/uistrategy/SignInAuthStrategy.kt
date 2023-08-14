package com.passbolt.mobile.android.feature.authentication.auth.uistrategy

import android.app.Activity
import androidx.navigation.fragment.findNavController
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.auth.AuthFragment
import com.passbolt.mobile.android.core.localization.R as LocalizationR

class SignInAuthStrategy(
    override var authFragment: AuthFragment?,
    override val appContext: AppContext
) : AuthStrategy {

    override fun title() =
        activeAuthFragment.getString(LocalizationR.string.auth_enter_passphrase)

    override fun navigateBack() {
        activeAuthFragment.findNavController().popBackStack()
    }

    private fun finishWithResult(result: Int) {
        with(activeAuthFragment.requireActivity()) {
            setResult(result)
            finish()
        }
    }

    override fun authSuccess() {
        finishWithResult(Activity.RESULT_OK)
    }

    override fun showLeaveConfirmationDialog(): Boolean = false
}
