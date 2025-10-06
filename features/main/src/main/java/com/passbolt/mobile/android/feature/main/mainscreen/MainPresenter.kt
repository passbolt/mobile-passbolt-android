package com.passbolt.mobile.android.feature.main.mainscreen

import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.core.inappreview.InAppReviewInteractor
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.main.mainscreen.bottomnavigation.MainBottomNavigationModel
import com.passbolt.mobile.android.feature.main.mainscreen.encouragements.EncouragementsInteractor
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class MainPresenter(
    private val inAppReviewInteractor: InAppReviewInteractor,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
    private val getFeatureFlagsUseCase: GetFeatureFlagsUseCase,
    private val encouragementsInteractor: EncouragementsInteractor,
    coroutineLaunchContext: CoroutineLaunchContext,
) : MainContract.Presenter {
    override var view: MainContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun attach(view: MainContract.View) {
        super.attach(view)
        setupBottomNavigation()
        performFullDataRefresh()
        if (encouragementsInteractor.shouldShowChromeNativeAutofillEncouragement()) {
            view.showChromeNativeAutofillEncouragement()
        }
        view.checkForAppUpdates()
        if (inAppReviewInteractor.shouldShowInAppReviewFlow()) {
            view.tryLaunchReviewFlow()
            inAppReviewInteractor.inAppReviewFlowShowed()
        }
    }

    private fun setupBottomNavigation() {
        scope.launch {
            val isTotpFeatureFlagEnabled = getFeatureFlagsUseCase.execute(Unit).featureFlags.isTotpAvailable
            view?.setupBottomNavigation(
                MainBottomNavigationModel(isOtpTabVisible = isTotpFeatureFlagEnabled),
            )
        }
    }

    override fun performFullDataRefresh() {
        view?.performFullDataRefresh()
        scope.launch {
            dataRefreshTrackingFlow.awaitIdle()
            setupBottomNavigation()
        }
    }

    override fun appUpdateDownloaded() {
        view?.showAppUpdateDownloadedSnackbar()
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super.detach()
    }
}
