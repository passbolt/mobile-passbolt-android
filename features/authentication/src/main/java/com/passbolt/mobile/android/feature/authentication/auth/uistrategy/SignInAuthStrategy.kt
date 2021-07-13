package com.passbolt.mobile.android.feature.authentication.auth.uistrategy

import androidx.navigation.fragment.findNavController
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.feature.authentication.R
import com.passbolt.mobile.android.feature.authentication.auth.AuthFragment

class SignInAuthStrategy(override var authFragment: AuthFragment?) : AuthStrategy {

    override fun title() =
        activeAuthFragment.getString(R.string.auth_sign_in)

    override fun navigateBack() {
        activeAuthFragment.findNavController().popBackStack()
    }

    override fun authSuccess() {
        activeAuthFragment.apply {
            startActivity(ActivityIntents.home(requireActivity()))
            requireActivity().finish()
        }
    }

    override fun domainVisible(): Boolean = false

    override fun showLeaveConfirmationDialog(): Boolean = false
}
