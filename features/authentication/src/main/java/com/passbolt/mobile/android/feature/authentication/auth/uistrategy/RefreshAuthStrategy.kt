package com.passbolt.mobile.android.feature.authentication.auth.uistrategy

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import com.passbolt.mobile.android.feature.authentication.R
import com.passbolt.mobile.android.feature.authentication.auth.AuthFragment

class RefreshAuthStrategy(override var authFragment: AuthFragment?) : AuthStrategy {

    override fun title() =
        activeAuthFragment.getString(R.string.auth_enter_passphrase)

    override fun navigateBack() {
        activeAuthFragment.requireActivity().finishAffinity()
    }

    override fun authSuccess() {
        (activeAuthFragment.requireActivity() as AppCompatActivity).apply {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    override fun domainVisible(): Boolean = true

    override fun showLeaveConfirmationDialog(): Boolean = false
}
