package com.passbolt.mobile.android.otpmoremenu

import com.passbolt.mobile.android.core.fulldatarefresh.base.DataRefreshViewReactivePresenter
import com.passbolt.mobile.android.core.idlingresource.CreateMenuModelIdlingResource
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.otpmoremenu.usecase.CreateOtpMoreMenuModelUseCase
import com.passbolt.mobile.android.ui.OtpMoreMenuModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class OtpMoreMenuPresenter(
    private val createOtpMoreMenuModelUseCase: CreateOtpMoreMenuModelUseCase,
    private val createMenuModelIdlingResource: CreateMenuModelIdlingResource,
    coroutineLaunchContext: CoroutineLaunchContext
) : DataRefreshViewReactivePresenter<OtpMoreMenuContract.View>(coroutineLaunchContext),
    OtpMoreMenuContract.Presenter {

    override var view: OtpMoreMenuContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    private lateinit var resourceId: String
    private lateinit var menuModel: OtpMoreMenuModel

    override fun argsRetrieved(resourceId: String, canShowTotp: Boolean) {
        this.resourceId = resourceId
        if (canShowTotp) {
            view?.showShowOtpButton()
        }
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<DataRefreshViewReactivePresenter>.detach()
    }

    override fun refreshSuccessAction() {
        createMenuModelIdlingResource.setIdle(false)
        scope.launch {
            menuModel = createOtpMoreMenuModelUseCase.execute(
                CreateOtpMoreMenuModelUseCase.Input(resourceId)
            ).otpMoreMenuModel
            view?.showTitle(menuModel.title)
            processEditAndDeleteButtons()
            createMenuModelIdlingResource.setIdle(true)
        }
    }

    override fun refreshFailureAction() {
        view?.showRefreshFailure()
    }

    private fun processEditAndDeleteButtons() {
        if (menuModel.canDelete || menuModel.canEdit) {
            view?.showSeparator()
        }

        if (menuModel.canEdit) {
            view?.showEditButton()
        }

        if (menuModel.canDelete) {
            view?.showDeleteButton()
        }
    }
}
