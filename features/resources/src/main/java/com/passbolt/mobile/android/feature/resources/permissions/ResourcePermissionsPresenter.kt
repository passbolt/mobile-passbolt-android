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
    private val permissionModelUiComparator: PermissionModelUiComparator,
    coroutineLaunchContext: CoroutineLaunchContext
) : ResourcePermissionsContract.Presenter,
    BaseAuthenticatedPresenter<ResourcePermissionsContract.View>(coroutineLaunchContext) {

    override var view: ResourcePermissionsContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    private lateinit var mode: ResourcePermissionsMode
    private lateinit var resourceId: String
    private lateinit var recipients: MutableList<PermissionModelUi>

    override fun argsReceived(resourceId: String, mode: ResourcePermissionsMode) {
        this.mode = mode
        this.resourceId = resourceId
        processItemsVisibility(mode)
        getResourcePermissions(resourceId)
    }

    private fun getResourcePermissions(resourceId: String) {
        scope.launch {
            if (!::recipients.isInitialized) {
                recipients = getLocalResourcePermissionsUseCase
                    .execute(GetLocalResourcePermissionsUseCase.Input(resourceId))
                    .permissions
                    .toMutableList()
            }
            view?.showPermissions(
                recipients.apply {
                    sortWith(permissionModelUiComparator)
                }
            )
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
                view?.navigateToGroupPermissionDetails(permission, mode)
            is PermissionModelUi.UserPermissionModel ->
                view?.navigateToUserPermissionDetails(permission, mode)
        }
    }

    override fun saveClick() {
        // TODO
    }

    override fun addPermissionClick() {
        view?.navigateToSelectShareRecipients(
            recipients.filterIsInstance<PermissionModelUi.GroupPermissionModel>(),
            recipients.filterIsInstance<PermissionModelUi.UserPermissionModel>()
        )
    }

    override fun shareRecipientsAdded(shareRecipients: ArrayList<PermissionModelUi>?) {
        shareRecipients?.let { newRecipients ->
            recipients = newRecipients
        }
    }

    override fun userPermissionModified(permission: PermissionModelUi.UserPermissionModel) {
        recipients
            .filterIsInstance<PermissionModelUi.UserPermissionModel>()
            .find { it.user.userId == permission.user.userId }
            ?.let { existingPermission ->
                val existingPermissionIndex = recipients.indexOf(existingPermission)
                recipients[existingPermissionIndex] =
                    PermissionModelUi.UserPermissionModel(permission.permission, existingPermission.user.copy())
            }
    }

    override fun groupPermissionModified(permission: PermissionModelUi.GroupPermissionModel) {
        recipients
            .filterIsInstance<PermissionModelUi.GroupPermissionModel>()
            .find { it.group.groupId == permission.group.groupId }
            ?.let { existingPermission ->
                val existingPermissionIndex = recipients.indexOf(existingPermission)
                recipients[existingPermissionIndex] =
                    PermissionModelUi.GroupPermissionModel(permission.permission, existingPermission.group.copy())
            }
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }
}
