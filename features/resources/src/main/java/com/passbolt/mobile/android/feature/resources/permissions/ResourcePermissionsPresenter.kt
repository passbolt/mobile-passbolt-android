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
    private lateinit var mode: ResourcePermissionsMode
    private lateinit var resourceId: String

    override fun argsReceived(resourceId: String, mode: ResourcePermissionsMode) {
        this.mode = mode
        this.resourceId = resourceId
        processItemsVisibility(mode)
        getResourcePermissions(resourceId)
    }

    private fun getResourcePermissions(resourceId: String) {
        scope.launch {
            getLocalResourcePermissionsUseCase
                .execute(GetLocalResourcePermissionsUseCase.Input(resourceId)).permissions
                .let { view?.showPermissions(it) }
        }
    }

    private fun processItemsVisibility(mode: ResourcePermissionsMode) {
        if (mode == ResourcePermissionsMode.EDIT) {
            view?.apply {
                showAddUserButton()
                showSaveButton()
            }
        }
    }

    override fun permissionClick(permission: PermissionModelUi) {
        when (permission) {
            is PermissionModelUi.GroupPermissionModel ->
                view?.navigateToGroupPermissionDetails(permission.group.groupId, permission.permission, mode)
            is PermissionModelUi.UserPermissionModel ->
                view?.navigateToUserPermissionDetails(permission.user.userId, permission.permission, mode)
        }
    }

    override fun saveClick() {
        // TODO
    }

    override fun addPermissionClick() {
        view?.navigateToSelectShareRecipients(resourceId)
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }
}
