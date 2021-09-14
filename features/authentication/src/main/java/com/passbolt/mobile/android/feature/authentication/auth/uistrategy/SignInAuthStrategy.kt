package com.passbolt.mobile.android.feature.authentication.auth.uistrategy

import android.app.Activity
import androidx.navigation.fragment.findNavController
import com.passbolt.mobile.android.feature.authentication.R
import com.passbolt.mobile.android.feature.authentication.auth.AuthFragment

class SignInAuthStrategy(override var authFragment: AuthFragment?) : AuthStrategy {

    override fun title() =
        activeAuthFragment.getString(R.string.auth_enter_passphrase)

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

    override fun domainVisible(): Boolean = false

    override fun showLeaveConfirmationDialog(): Boolean = false
}
