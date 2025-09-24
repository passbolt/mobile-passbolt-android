package com.passbolt.mobile.android.createresourcemenu.view

import com.passbolt.mobile.android.core.idlingresource.CreateMenuModelIdlingResource
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.createresourcemenu.usecase.CreateCreateResourceMenuModelUseCase
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class CreateResourceMenuPresenter(
    private val createCreateResourceMoreMenuModelUseCase: CreateCreateResourceMenuModelUseCase,
    private val menuModelIdlingResource: CreateMenuModelIdlingResource,
    coroutineLaunchContext: CoroutineLaunchContext,
) : CreateResourceMenuContract.Presenter {
    override var view: CreateResourceMenuContract.View? = null
    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun argsRetrieved(homeDisplayViewModel: HomeDisplayViewModel?) {
        processFieldsVisibility(homeDisplayViewModel)
    }

    override fun detach() {
        coroutineScope.coroutineContext.cancelChildren()
        super.detach()
    }

    private fun processFieldsVisibility(homeDisplayViewModel: HomeDisplayViewModel?) {
        coroutineScope.launch {
            menuModelIdlingResource.setIdle(false)
            createCreateResourceMoreMenuModelUseCase
                .execute(
                    CreateCreateResourceMenuModelUseCase.Input(homeDisplayViewModel),
                ).model
                .apply {
                    if (isPasswordEnabled) {
                        view?.showPasswordButton()
                    }
                    if (isTotpEnabled) {
                        view?.showTotpButton()
                    }
                    if (isFolderEnabled) {
                        view?.showFoldersButton()
                    }
                }
            menuModelIdlingResource.setIdle(true)
        }
    }
}
