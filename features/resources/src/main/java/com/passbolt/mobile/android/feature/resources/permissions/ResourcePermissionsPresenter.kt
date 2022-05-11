package com.passbolt.mobile.android.feature.resources.permissions

import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.database.impl.resources.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.ui.PermissionModelUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class ResourcePermissionsPresenter(
    private val getLocalResourcePermissionsUseCase: GetLocalResourcePermissionsUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : ResourcePermissionsContract.Presenter,
    BaseAuthenticatedPresenter<ResourcePermissionsContract.View>(coroutineLaunchContext) {

    override var view: ResourcePermissionsContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun argsReceived(resourceId: String) {
        scope.launch {
            getLocalResourcePermissionsUseCase
                .execute(GetLocalResourcePermissionsUseCase.Input(resourceId)).permissions
                .let { view?.showPermissions(it) }
        }
    }

    override fun permissionClick(permission: PermissionModelUi) {
        when (permission) {
            is PermissionModelUi.GroupPermissionModel ->
                view?.navigateToGroupPermissionDetails(permission.group.groupId, permission.permission)
            is PermissionModelUi.UserPermissionModel ->
                view?.navigateToUserPermissionDetails(permission.user.userId, permission.permission)
        }
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }
}
