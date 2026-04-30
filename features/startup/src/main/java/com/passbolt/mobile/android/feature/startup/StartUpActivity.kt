package com.passbolt.mobile.android.feature.startup

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.koin.android.ext.android.inject

// NOTE: When changing name or package read core/navigation/README.md
class StartUpActivity : AppCompatActivity() {
    private val accountSetupModelCreator: AccountSetupModelCreator by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StartUpScreen(
                accountSetupDataModel = accountSetupModelCreator.createFromIntent(intent),
            )
        }
    }
}
