package com.passbolt.mobile.android.feature.main.mainscreen

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.passbolt.mobile.android.core.security.runtimeauth.RuntimeAuthenticatedFlag
import org.koin.android.ext.android.inject

// NOTE: When changing name or package read core/navigation/README.md
class MainActivity : AppCompatActivity() {
    private val runtimeAuthenticatedFlag: RuntimeAuthenticatedFlag by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        runtimeAuthenticatedFlag.require(this)
        setContent { MainScreen() }
    }
}
