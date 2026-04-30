package com.passbolt.mobile.android.feature.main.mainscreen

import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.core.autofill.AutofillInformationProvider
import com.passbolt.mobile.android.core.autofill.AutofillInformationProvider.ChromeNativeAutofillStatus.ENABLED
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.inappreview.InAppReviewInteractor
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.feature.main.mainscreen.MainIntent.AppUpdateDownloaded
import com.passbolt.mobile.android.feature.main.mainscreen.MainIntent.CloseChromeNativeAutofill
import com.passbolt.mobile.android.feature.main.mainscreen.MainIntent.GoToSettings
import com.passbolt.mobile.android.feature.main.mainscreen.MainIntent.Resumed
import com.passbolt.mobile.android.feature.main.mainscreen.MainIntent.TabSelected
import com.passbolt.mobile.android.feature.main.mainscreen.MainSideEffect.CheckForAppUpdates
import com.passbolt.mobile.android.feature.main.mainscreen.MainSideEffect.LaunchChromeNativeAutofillDeeplink
import com.passbolt.mobile.android.feature.main.mainscreen.MainSideEffect.PerformFullDataRefresh
import com.passbolt.mobile.android.feature.main.mainscreen.MainSideEffect.ShowSnackbar
import com.passbolt.mobile.android.feature.main.mainscreen.MainSideEffect.TryLaunchReviewFlow
import com.passbolt.mobile.android.feature.main.mainscreen.bottomnavigation.MainBottomNavigationModel
import com.passbolt.mobile.android.feature.main.mainscreen.encouragements.EncouragementsInteractor
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase

class MainViewModel(
    private val inAppReviewInteractor: InAppReviewInteractor,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
    private val getFeatureFlagsUseCase: GetFeatureFlagsUseCase,
    private val encouragementsInteractor: EncouragementsInteractor,
    private val autofillInformationProvider: AutofillInformationProvider,
    private val appNavigator: AppNavigator,
) : SideEffectViewModel<MainState, MainSideEffect>(MainState()) {
    init {
        setupBottomNavigation()
        performFullDataRefresh()
        checkEncouragements()
        emitSideEffect(CheckForAppUpdates)
        checkReviewFlow()
        collectTabSwitchRequests()
    }

    fun onIntent(intent: MainIntent) {
        when (intent) {
            AppUpdateDownloaded -> emitSideEffect(ShowSnackbar(SnackbarType.APP_UPDATE_DOWNLOADED))
            GoToSettings -> emitSideEffect(LaunchChromeNativeAutofillDeeplink)
            CloseChromeNativeAutofill -> updateViewState { copy(showChromeNativeAutofillDialog = false) }
            Resumed -> checkChromeNativeAutofillStatus()
            is TabSelected -> updateViewState { copy(selectedTab = intent.tab) }
        }
    }

    private fun setupBottomNavigation() {
        launch {
            val isTotpFeatureFlagEnabled = getFeatureFlagsUseCase.execute(Unit).featureFlags.isTotpAvailable
            updateViewState {
                copy(bottomNavigationModel = MainBottomNavigationModel(isOtpTabVisible = isTotpFeatureFlagEnabled))
            }
        }
    }

    private fun performFullDataRefresh() {
        emitSideEffect(PerformFullDataRefresh)
        launch {
            dataRefreshTrackingFlow.awaitIdle()
            setupBottomNavigation()
        }
    }

    private fun checkEncouragements() {
        if (encouragementsInteractor.shouldShowChromeNativeAutofillEncouragement()) {
            encouragementsInteractor.chromeNativeAutofillEncouragementShown()
            updateViewState { copy(showChromeNativeAutofillDialog = true) }
        }
    }

    private fun checkReviewFlow() {
        if (inAppReviewInteractor.shouldShowInAppReviewFlow()) {
            emitSideEffect(TryLaunchReviewFlow)
            inAppReviewInteractor.inAppReviewFlowShowed()
        }
    }

    private fun checkChromeNativeAutofillStatus() {
        if (viewState.value.showChromeNativeAutofillDialog &&
            autofillInformationProvider.getChromeNativeAutofillStatus() == ENABLED
        ) {
            updateViewState { copy(showChromeNativeAutofillDialog = false) }
            emitSideEffect(ShowSnackbar(SnackbarType.CHROME_NATIVE_AUTOFILL_SETUP_SUCCESS))
        }
    }

    private fun collectTabSwitchRequests() {
        launch {
            appNavigator.tabSwitchRequest.collect { tab ->
                updateViewState { copy(selectedTab = tab) }
            }
        }
    }
}
