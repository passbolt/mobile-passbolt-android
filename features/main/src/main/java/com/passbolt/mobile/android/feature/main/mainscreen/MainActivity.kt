package com.passbolt.mobile.android.feature.main.mainscreen

import PassboltTheme
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup.LayoutParams
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.isVisible
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManager
import com.passbolt.mobile.android.common.ExternalDeeplinkHandler
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.findNavHostFragment
import com.passbolt.mobile.android.core.extension.getRootView
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.core.fulldatarefresh.service.DataRefreshService
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedActivity
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.BottomTab
import com.passbolt.mobile.android.core.navigation.compose.keys.HomeNavigationKey.Home
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.Otp
import com.passbolt.mobile.android.core.navigation.compose.keys.SettingsNavigationKey.SettingsMain
import com.passbolt.mobile.android.core.security.runtimeauth.RuntimeAuthenticatedFlag
import com.passbolt.mobile.android.feature.main.databinding.ActivityMainBinding
import com.passbolt.mobile.android.feature.main.mainscreen.MainIntent.AppUpdateDownloaded
import com.passbolt.mobile.android.feature.main.mainscreen.MainIntent.CloseChromeNativeAutofill
import com.passbolt.mobile.android.feature.main.mainscreen.MainIntent.GoToSettings
import com.passbolt.mobile.android.feature.main.mainscreen.MainIntent.Resumed
import com.passbolt.mobile.android.feature.main.mainscreen.MainSideEffect.CheckForAppUpdates
import com.passbolt.mobile.android.feature.main.mainscreen.MainSideEffect.LaunchChromeNativeAutofillDeeplink
import com.passbolt.mobile.android.feature.main.mainscreen.MainSideEffect.PerformFullDataRefresh
import com.passbolt.mobile.android.feature.main.mainscreen.MainSideEffect.ShowSnackbar
import com.passbolt.mobile.android.feature.main.mainscreen.MainSideEffect.TryLaunchReviewFlow
import com.passbolt.mobile.android.feature.main.mainscreen.encouragements.chromenativeautofill.EncourageChromeNativeAutofillDialog
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR
import com.passbolt.mobile.android.feature.home.R as HomeR
import com.passbolt.mobile.android.feature.otp.R as OtpR
import com.passbolt.mobile.android.feature.settings.R as SettingsR

// NOTE: When changing name or package read core/navigation/README.md
class MainActivity : BindingScopedActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {
    private val mainViewModel: MainViewModel by viewModel()

    private val bottomNavController by lifecycleAwareLazy {
        findNavHostFragment(requiredBinding.fragmentContainer.id).navController
    }
    private val runtimeAuthenticatedFlag: RuntimeAuthenticatedFlag by inject()
    private val appUpdateManager: AppUpdateManager by inject()
    private val appUpdateStatusListener =
        InstallStateUpdatedListener { state ->
            val installStatus = state.installStatus()
            Timber.d("App update install status: $installStatus")
            when (installStatus) {
                InstallStatus.DOWNLOADED -> {
                    mainViewModel.onIntent(AppUpdateDownloaded)
                }
            }
        }
    private val appReviewManager: ReviewManager by inject()
    private val appNavigator: AppNavigator by inject()
    private val externalDeeplinkHandler: ExternalDeeplinkHandler by inject()

    private var bottomNavInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runtimeAuthenticatedFlag.require(this)
        setupComposeDialogs()
        observeViewState()
        observeSideEffects()
    }

    private fun setupComposeDialogs() {
        val composeView = ComposeView(this)
        addContentView(
            composeView,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            ),
        )
        composeView.setContent {
            PassboltTheme {
                val state by mainViewModel.viewState.collectAsStateWithLifecycle()
                if (state.showChromeNativeAutofillDialog) {
                    EncourageChromeNativeAutofillDialog(
                        onGoToChromeSettings = {
                            mainViewModel.onIntent(GoToSettings)
                        },
                        onClose = {
                            mainViewModel.onIntent(CloseChromeNativeAutofill)
                        },
                    )
                }
            }
        }
    }

    private fun observeViewState() {
        lifecycleScope.launch {
            mainViewModel.viewState.collect { state ->
                state.bottomNavigationModel?.let { navigationModel ->
                    if (!bottomNavInitialized) {
                        bottomNavInitialized = true
                        requiredBinding.mainNavigation.setupWithNavController(bottomNavController)
                        hideBottomNavigationForComposables()
                        handleTabSwitchRequests()
                    }
                    requiredBinding.mainNavigation.menu
                        .findItem(OtpR.id.otpNav)
                        .isVisible = navigationModel.isOtpTabVisible
                }
            }
        }
    }

    private fun observeSideEffects() {
        lifecycleScope.launch {
            mainViewModel.sideEffect.collect { effect ->
                when (effect) {
                    CheckForAppUpdates -> checkForAppUpdates()
                    is ShowSnackbar -> handleSnackbar(effect.message)
                    TryLaunchReviewFlow -> tryLaunchReviewFlow()
                    PerformFullDataRefresh -> DataRefreshService.start(this@MainActivity)
                    LaunchChromeNativeAutofillDeeplink ->
                        externalDeeplinkHandler.openChromeNativeAutofillSettings(this@MainActivity)
                }
            }
        }
    }

    private fun handleSnackbar(message: SnackbarType) {
        when (message) {
            SnackbarType.APP_UPDATE_DOWNLOADED -> showAppUpdateDownloadedSnackbar()
            SnackbarType.CHROME_NATIVE_AUTOFILL_SETUP_SUCCESS ->
                showSnackbar(
                    getString(LocalizationR.string.main_chrome_native_autofill_setup_success),
                    backgroundColor = CoreUiR.color.green,
                )
        }
    }

    private fun hideBottomNavigationForComposables() {
        lifecycleScope.launch {
            appNavigator.currentBackStackItem.collect { destinationKey ->
                destinationKey?.let {
                    requiredBinding.mainNavigation.isVisible =
                        bottomNavComposableTypes.any { type ->
                            type.isInstance(destinationKey)
                        }
                }
            }
        }
    }

    private fun handleTabSwitchRequests() {
        lifecycleScope.launch {
            appNavigator.tabSwitchRequest.collect { tab ->
                requiredBinding.mainNavigation.selectedItemId =
                    when (tab) {
                        BottomTab.HOME -> HomeR.id.homeNav
                        BottomTab.OTP -> OtpR.id.otpNav
                        BottomTab.SETTINGS -> SettingsR.id.settingsNavCompose
                    }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.onIntent(Resumed)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_APP_UPDATE) {
            if (resultCode != RESULT_OK) {
                Timber.e("Update flow not completed. Result code: $resultCode")
            }
        }
    }

    override fun onDestroy() {
        appUpdateManager.unregisterListener(appUpdateStatusListener)
        super.onDestroy()
    }

    private fun checkForAppUpdates() {
        appUpdateManager.registerListener(appUpdateStatusListener)
        appUpdateManager.appUpdateInfo
            .addOnFailureListener { Timber.e(it, "Application update failed.") }
            .addOnCanceledListener { Timber.d("Application update cancelled") }
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                ) {
                    Timber.d("Application update available. Starting update flow.")
                    startAppUpdateFlow(appUpdateInfo)
                }
            }
    }

    private fun startAppUpdateFlow(appUpdateInfo: AppUpdateInfo) {
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            AppUpdateType.FLEXIBLE,
            this,
            REQUEST_APP_UPDATE,
        )
    }

    private fun showAppUpdateDownloadedSnackbar() {
        Snackbar
            .make(
                getRootView(),
                getString(LocalizationR.string.main_update_downloaded),
                Snackbar.LENGTH_INDEFINITE,
            ).apply {
                anchorView = requiredBinding.mainNavigation
                setAction(
                    getString(LocalizationR.string.main_update_downloaded_install),
                ) {
                    appUpdateManager.completeUpdate()
                }
                show()
            }
    }

    private fun tryLaunchReviewFlow() {
        // review flow launch success depends on app review API quota
        appReviewManager
            .requestReviewFlow()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    it.result?.let { result -> appReviewManager.launchReviewFlow(this, result) }
                } else {
                    Timber.e("In app review request to start flow failed: ${it.exception?.message}")
                }
            }
    }

    private companion object {
        private const val REQUEST_APP_UPDATE = 8000

        private val bottomNavComposableTypes =
            listOf(
                Home::class,
                Otp::class,
                SettingsMain::class,
            )
    }
}
