package com.passbolt.mobile.android.feature.main.mainscreen

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManager
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.findNavHostFragment
import com.passbolt.mobile.android.core.extension.getRootView
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.core.security.runtimeauth.RuntimeAuthenticatedFlag
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedActivity
import com.passbolt.mobile.android.feature.main.databinding.ActivityMainBinding
import com.passbolt.mobile.android.feature.main.mainscreen.bottomnavigation.MainBottomNavigationModel
import com.passbolt.mobile.android.feature.main.mainscreen.encouragements.chromenativeautofill.EncourageChromeNativeAutofillServiceDialog
import org.koin.android.ext.android.inject
import timber.log.Timber
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR
import com.passbolt.mobile.android.feature.createotpmanually.R as CreateOtpManuallyR
import com.passbolt.mobile.android.feature.otp.R as OtpR
import com.passbolt.mobile.android.feature.resourceform.R as ResourceFormR
import com.passbolt.mobile.android.feature.resourcepicker.R as ResourcePickerR
import com.passbolt.mobile.android.feature.scanotp.R as ScanOtpR

class MainActivity :
    BindingScopedAuthenticatedActivity<ActivityMainBinding, MainContract.View>(ActivityMainBinding::inflate),
    MainContract.View, EncourageChromeNativeAutofillServiceDialog.Listener {

    override val presenter: MainContract.Presenter by inject()

    private val bottomNavController by lifecycleAwareLazy {
        findNavHostFragment(binding.fragmentContainer.id).navController
    }
    private val runtimeAuthenticatedFlag: RuntimeAuthenticatedFlag by inject()
    private val appUpdateManager: AppUpdateManager by inject()
    private val appUpdateStatusListener = InstallStateUpdatedListener { state ->
        val installStatus = state.installStatus()
        Timber.d("App update install status: $installStatus")
        when (installStatus) {
            InstallStatus.DOWNLOADED -> {
                presenter.appUpdateDownloaded()
            }
        }
    }
    private val appReviewManager: ReviewManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runtimeAuthenticatedFlag.require(this)
        presenter.attach(this)
    }

    override fun setupBottomNavigation(navigationModel: MainBottomNavigationModel) {
        with(binding.mainNavigation) {
            setupWithNavController(bottomNavController)
            menu.findItem(OtpR.id.otpNav).isVisible = navigationModel.isOtpTabVisible
        }

        bottomNavController.addOnDestinationChangedListener { _, destination, _ ->
            binding.mainNavigation.isVisible = !noBottomNavFragmentIds.contains(destination.id)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_APP_UPDATE) {
            if (resultCode != RESULT_OK) {
                Timber.e("Update flow not completed. Result code: $resultCode")
            }
        }
    }

    override fun onDestroy() {
        appUpdateManager.unregisterListener(appUpdateStatusListener)
        presenter.detach()
        super.onDestroy()
    }

    override fun checkForAppUpdates() {
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
            REQUEST_APP_UPDATE
        )
    }

    override fun showAppUpdateDownloadedSnackbar() {
        Snackbar.make(
            getRootView(),
            getString(LocalizationR.string.main_update_downloaded),
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            anchorView = binding.mainNavigation
            setAction(
                getString(LocalizationR.string.main_update_downloaded_install)
            ) {
                appUpdateManager.completeUpdate()
            }
            show()
        }
    }

    override fun tryLaunchReviewFlow() {
        // review flow launch success depends on app review API quota
        appReviewManager.requestReviewFlow()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    it.result?.let { result -> appReviewManager.launchReviewFlow(this, result) }
                } else {
                    Timber.e("In app review request to start flow failed: ${it.exception?.message}")
                }
            }
    }

    override fun showChromeNativeAutofillEncouragement() {
        EncourageChromeNativeAutofillServiceDialog().show(
            supportFragmentManager,
            EncourageChromeNativeAutofillServiceDialog::class.java.name
        )
    }

    override fun chromeNativeAutofillSetupSuccessfully() {
        showSnackbar(
            getString(LocalizationR.string.main_chrome_native_autofill_setup_success),
            backgroundColor = CoreUiR.color.green
        )
    }

    private companion object {
        private const val REQUEST_APP_UPDATE = 8000
        private val noBottomNavFragmentIds = listOf(
            ScanOtpR.id.scanOtpFragment,
            CreateOtpManuallyR.id.createOtpManuallyFragment,
            CreateOtpManuallyR.id.createOtpAdvancedSettingsFragment,
            OtpR.id.scanOtpSuccessFragment,
            ResourcePickerR.id.resourcePickerFragment,
            ResourceFormR.id.resourceFormFragment
        )
    }
}
