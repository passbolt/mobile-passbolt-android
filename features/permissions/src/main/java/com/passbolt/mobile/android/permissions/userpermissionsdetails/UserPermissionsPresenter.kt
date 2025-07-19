package com.passbolt.mobile.android.permissions.userpermissionsdetails

import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.users.usecase.db.GetLocalUserUseCase
import com.passbolt.mobile.android.permissions.permissions.PermissionsMode
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class UserPermissionsPresenter(
    private val getLocalUserUseCase: GetLocalUserUseCase,
    coroutineLaunchContext: CoroutineLaunchContext,
) : BaseAuthenticatedPresenter<UserPermissionsContract.View>(coroutineLaunchContext),
    UserPermissionsContract.Presenter {
    override var view: UserPermissionsContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    private lateinit var userPermission: PermissionModelUi.UserPermissionModel

    override fun argsRetrieved(
        userPermission: PermissionModelUi.UserPermissionModel,
        mode: PermissionsMode,
    ) {
        this.userPermission = userPermission
        scope.launch {
            getLocalUserUseCase.execute(GetLocalUserUseCase.Input(userPermission.user.userId)).user.let {
                view?.showUserData(it)
            }
        }
        when (mode) {
            PermissionsMode.VIEW -> view?.showPermission(userPermission.permission)
            PermissionsMode.EDIT -> {
                view?.apply {
                    showPermissionChoices(userPermission.permission)
                    showSaveLayout()
                }
            }
        }
    }

    override fun onPermissionSelected(permission: ResourcePermission) {
        userPermission =
            PermissionModelUi.UserPermissionModel(
                permission,
                userPermission.permissionId,
                userPermission.user.copy(),
            )
    }

    override fun saveClick() {
        view?.apply {
            setUpdatedPermissionResult(userPermission)
            navigateBack()
        }
    }

    override fun deletePermissionClick() {
        view?.showPermissionDeleteConfirmation()
    }

    override fun permissionDeleteConfirmClick() {
        view?.apply {
            setDeletePermissionResult(userPermission)
            navigateBack()
        }
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }
}
