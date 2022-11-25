package com.passbolt.mobile.android.feature.main.mainscreen

import com.passbolt.mobile.android.core.inappreview.InAppReviewInteractor
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren

class MainPresenter(
    private val inAppReviewInteractor: InAppReviewInteractor,
    coroutineLaunchContext: CoroutineLaunchContext
) : BaseAuthenticatedPresenter<MainContract.View>(coroutineLaunchContext), MainContract.Presenter {

    override var view: MainContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun attach(view: MainContract.View) {
        super<BaseAuthenticatedPresenter>.attach(view)
        view.checkForAppUpdates()
        if (inAppReviewInteractor.shouldShowInAppReviewFlow()) {
            view.tryLaunchReviewFlow()
            inAppReviewInteractor.inAppReviewFlowShowed()
        }
    }

    override fun appUpdateDownloaded() {
        view?.showAppUpdateDownloadedSnackbar()
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }
}
