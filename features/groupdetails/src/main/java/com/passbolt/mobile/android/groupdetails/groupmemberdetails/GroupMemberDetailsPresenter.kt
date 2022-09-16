package com.passbolt.mobile.android.groupdetails.groupmemberdetails

import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.database.impl.users.GetLocalUserUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class GroupMemberDetailsPresenter(
    private val getLocalUserUseCase: GetLocalUserUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : GroupMemberDetailsContract.Presenter,
    BaseAuthenticatedPresenter<GroupMemberDetailsContract.View>(coroutineLaunchContext) {

    override var view: GroupMemberDetailsContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun argsRetrieved(userId: String) {
        scope.launch {
            getLocalUserUseCase.execute(GetLocalUserUseCase.Input(userId)).user.let {
                view?.showUserData(it)
            }
        }
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }
}
