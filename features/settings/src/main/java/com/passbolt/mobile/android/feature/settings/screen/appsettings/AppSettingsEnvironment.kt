package com.passbolt.mobile.android.feature.settings.screen.appsettings

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.biometric.BiometricPrompt
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.Executor

data class AppSettingsEnvironment(
    val context: Context,
    val authenticationLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    val biometricPromptBuilder: BiometricPrompt.PromptInfo.Builder,
    val executor: Executor,
    val snackbarHostState: SnackbarHostState,
    val coroutineScope: CoroutineScope,
)
