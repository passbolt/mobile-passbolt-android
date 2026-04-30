package com.passbolt.mobile.android.feature.main.mainscreen

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManager
import com.passbolt.mobile.android.core.compose.OnResumeEffect
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.fulldatarefresh.service.DataRefreshService
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.feature.main.mainscreen.MainIntent.AppUpdateDownloaded
import com.passbolt.mobile.android.feature.main.mainscreen.MainIntent.Resumed
import com.passbolt.mobile.android.feature.main.mainscreen.MainSideEffect.CheckForAppUpdates
import com.passbolt.mobile.android.feature.main.mainscreen.MainSideEffect.LaunchChromeNativeAutofillDeeplink
import com.passbolt.mobile.android.feature.main.mainscreen.MainSideEffect.PerformFullDataRefresh
import com.passbolt.mobile.android.feature.main.mainscreen.MainSideEffect.ShowSnackbar
import com.passbolt.mobile.android.feature.main.mainscreen.MainSideEffect.TryLaunchReviewFlow
import kotlinx.coroutines.flow.Flow
import org.koin.compose.koinInject
import timber.log.Timber
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Composable
internal fun MainScreenEffects(
    sideEffect: Flow<MainSideEffect>,
    onIntent: (MainIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val appUpdateManager: AppUpdateManager = koinInject()
    val appReviewManager: ReviewManager = koinInject()
    val appNavigator: AppNavigator = koinInject()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val currentOnIntent by rememberUpdatedState(onIntent)
    val appUpdateResultLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { /* update progress tracked by InstallStateUpdatedListener */ }

    val appUpdateStatusListener =
        remember {
            InstallStateUpdatedListener { installState ->
                val installStatus = installState.installStatus()
                Timber.d("App update install status: $installStatus")
                when (installStatus) {
                    InstallStatus.DOWNLOADED -> {
                        currentOnIntent(AppUpdateDownloaded)
                    }
                    else -> {}
                }
            }
        }

    DisposableEffect(appUpdateManager) {
        onDispose {
            appUpdateManager.unregisterListener(appUpdateStatusListener)
        }
    }

    OnResumeEffect { currentOnIntent(Resumed) }

    SideEffectDispatcher(sideEffect) {
        when (it) {
            CheckForAppUpdates -> checkForAppUpdates(appUpdateManager, appUpdateStatusListener, appUpdateResultLauncher)
            TryLaunchReviewFlow -> activity?.let { act -> tryLaunchReviewFlow(appReviewManager, act) }
            PerformFullDataRefresh -> DataRefreshService.start(context)
            LaunchChromeNativeAutofillDeeplink -> appNavigator.openChromeNativeAutofillSettings(context)
            is ShowSnackbar ->
                handleSnackbar(
                    it.message,
                    snackbarHostState,
                    context,
                ) { appUpdateManager.completeUpdate() }
        }
    }
}

private fun checkForAppUpdates(
    appUpdateManager: AppUpdateManager,
    listener: InstallStateUpdatedListener,
    activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>,
) {
    appUpdateManager.registerListener(listener)
    appUpdateManager.appUpdateInfo
        .addOnFailureListener { Timber.e(it, "Application update failed.") }
        .addOnCanceledListener { Timber.d("Application update cancelled") }
        .addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                Timber.d("Application update available. Starting update flow.")
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    activityResultLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                )
            }
        }
}

private fun tryLaunchReviewFlow(
    appReviewManager: ReviewManager,
    activity: Activity,
) {
    appReviewManager
        .requestReviewFlow()
        .addOnCompleteListener {
            if (it.isSuccessful) {
                it.result?.let { result -> appReviewManager.launchReviewFlow(activity, result) }
            } else {
                Timber.e("In app review request to start flow failed: ${it.exception?.message}")
            }
        }
}

private suspend fun handleSnackbar(
    message: SnackbarType,
    snackbarHostState: SnackbarHostState,
    context: android.content.Context,
    onCompleteAppUpdate: () -> Unit,
) {
    when (message) {
        SnackbarType.APP_UPDATE_DOWNLOADED -> {
            val result =
                snackbarHostState.showSnackbar(
                    message = context.getString(LocalizationR.string.main_update_downloaded),
                    actionLabel = context.getString(LocalizationR.string.main_update_downloaded_install),
                    duration = SnackbarDuration.Indefinite,
                )
            if (result == SnackbarResult.ActionPerformed) {
                onCompleteAppUpdate()
            }
        }
        SnackbarType.CHROME_NATIVE_AUTOFILL_SETUP_SUCCESS -> {
            snackbarHostState.showSnackbar(
                message = context.getString(LocalizationR.string.main_chrome_native_autofill_setup_success),
            )
        }
    }
}
