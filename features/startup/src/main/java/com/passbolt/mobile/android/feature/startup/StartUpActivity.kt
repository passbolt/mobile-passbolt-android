package com.passbolt.mobile.android.feature.startup

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.passbolt.mobile.android.core.navigation.ActivityIntents

class StartUpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO add logic for navigating between sign in and setup
        startActivity(ActivityIntents.setup(this))
    }
}
