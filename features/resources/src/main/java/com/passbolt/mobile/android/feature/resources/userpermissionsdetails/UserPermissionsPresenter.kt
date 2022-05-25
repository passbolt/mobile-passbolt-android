package com.passbolt.mobile.android.feature.resources.userpermissionsdetails

import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.database.impl.users.GetLocalUserUseCase
import com.passbolt.mobile.android.feature.resources.permissions.ResourcePermissionsMode
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class UserPermissionsPresenter(
    private val getLocalUserUseCase: GetLocalUserUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : UserPermissionsContract.Presenter,
    BaseAuthenticatedPresenter<UserPermissionsContract.View>(coroutineLaunchContext) {

    override var view: UserPermissionsContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun argsRetrieved(userId: String, permission: ResourcePermission, mode: ResourcePermissionsMode) {
        scope.launch {
            getLocalUserUseCase.execute(GetLocalUserUseCase.Input(userId)).user.let {
                view?.showUserData(it)
            }
        }
        when (mode) {
            ResourcePermissionsMode.VIEW -> view?.showPermission(permission)
            ResourcePermissionsMode.EDIT -> view?.showPermissionChoices(permission)
        }
    }

    override fun onPermissionSelected(permission: ResourcePermission) {
        // TODO
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }
}
