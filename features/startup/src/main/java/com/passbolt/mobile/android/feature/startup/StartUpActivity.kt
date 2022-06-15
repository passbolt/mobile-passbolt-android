package com.passbolt.mobile.android.feature.startup

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedActivity
import com.passbolt.mobile.android.core.navigation.AccountSetupDataModel
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.feature.startup.databinding.ActivityStartupBinding
import org.koin.android.ext.android.inject

class StartUpActivity : BindingScopedActivity<ActivityStartupBinding>(ActivityStartupBinding::inflate),
    StartUpContract.View {

    private val presenter: StartUpContract.Presenter by inject()
    private val accountSetupModelCreator: AccountSetupModelCreator by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        with(presenter) {
            attach(this@StartUpActivity)
            accountSetupDataRetrieved(
                accountSetupModelCreator.createFromIntent(intent)
            )
        }
    }

    override fun navigateToSetup(accountSetupDataModel: AccountSetupDataModel?) {
        startActivity(ActivityIntents.setup(this, accountSetupDataModel))
        finish()
    }

    override fun navigateToSignIn() {
        startActivity(ActivityIntents.authentication(this, ActivityIntents.AuthConfig.Startup))
        finish()
    }
}
