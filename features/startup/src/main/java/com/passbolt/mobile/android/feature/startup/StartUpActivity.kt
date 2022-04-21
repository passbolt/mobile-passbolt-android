package com.passbolt.mobile.android.feature.startup

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedActivity
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.feature.startup.databinding.ActivityStartupBinding
import org.koin.android.ext.android.inject

class StartUpActivity : BindingScopedActivity<ActivityStartupBinding>(ActivityStartupBinding::inflate),
    StartUpContract.View {

    private val presenter: StartUpContract.Presenter by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        presenter.attach(this)
    }

    override fun navigateToSetup() {
        startActivity(ActivityIntents.setup(this))
        finish()
    }

    override fun navigateToSignIn() {
        startActivity(ActivityIntents.authentication(this, ActivityIntents.AuthConfig.Startup))
        finish()
    }
}
