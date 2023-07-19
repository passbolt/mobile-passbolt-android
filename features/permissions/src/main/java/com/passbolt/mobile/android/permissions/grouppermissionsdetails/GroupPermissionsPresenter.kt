package com.passbolt.mobile.android.permissions.grouppermissionsdetails

import com.passbolt.mobile.android.core.commongroups.usecase.db.GetGroupWithUsersUseCase
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.permissions.permissions.PermissionsMode
import com.passbolt.mobile.android.permissions.recycler.UsersDatasetCreator
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class GroupPermissionsPresenter(
    private val getGroupWithUsersUseCase: GetGroupWithUsersUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : GroupPermissionsContract.Presenter,
    BaseAuthenticatedPresenter<GroupPermissionsContract.View>(coroutineLaunchContext) {

    override var view: GroupPermissionsContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    private lateinit var groupPermission: PermissionModelUi.GroupPermissionModel

    override fun argsRetrieved(
        permission: PermissionModelUi.GroupPermissionModel,
        mode: PermissionsMode,
        membersRecyclerWidth: Int,
        membersItemWidth: Float
    ) {
        this.groupPermission = permission
        when (mode) {
            PermissionsMode.VIEW -> view?.showPermission(permission.permission)
            PermissionsMode.EDIT -> {
                view?.apply {
                    showPermissionChoices(permission.permission)
                    showSaveLayout()
                }
            }
        }
        scope.launch {
            getGroupWithUsersUseCase.execute(
                GetGroupWithUsersUseCase.Input(
                    permission.group.groupId
                )
            ).groupWithUsers
                .let {
                    view?.showGroupName(it.group.groupName)

                    val usersDisplayDataset = UsersDatasetCreator(membersRecyclerWidth, membersItemWidth)
                        .prepareDataset(it.users)

                    view?.showGroupUsers(
                        usersDisplayDataset.users,
                        usersDisplayDataset.counterValue,
                        usersDisplayDataset.overlap
                    )
                }
        }
    }

    override fun groupMembersRecyclerClick() {
        view?.navigateToGroupMembers(groupPermission.group.groupId)
    }

    override fun onPermissionSelected(permission: ResourcePermission) {
        groupPermission = PermissionModelUi.GroupPermissionModel(
            permission,
            groupPermission.permissionId,
            groupPermission.group.copy()
        )
    }

    override fun saveButtonClick() {
        view?.apply {
            setUpdatedPermissionResult(groupPermission)
            navigateBack()
        }
    }

    override fun deletePermissionClick() {
        view?.showPermissionDeleteConfirmation()
    }

    override fun permissionDeleteConfirmClick() {
        view?.apply {
            setDeletePermissionResult(groupPermission)
            navigateBack()
        }
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }
}
