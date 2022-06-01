package com.passbolt.mobile.android.feature.resources.grouppermissionsdetails

import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.database.impl.groups.GetGroupWithUsersUseCase
import com.passbolt.mobile.android.feature.resources.permissionavatarlist.UsersDatasetCreator
import com.passbolt.mobile.android.feature.resources.permissions.ResourcePermissionsMode
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
        mode: ResourcePermissionsMode,
        membersRecyclerWidth: Int,
        membersItemWidth: Float
    ) {
        this.groupPermission = permission
        when (mode) {
            ResourcePermissionsMode.VIEW -> view?.showPermission(permission.permission)
            ResourcePermissionsMode.EDIT -> {
                view?.apply {
                    showPermissionChoices(permission.permission)
                    showSaveLayout()
                }
            }
        }
        scope.launch {
            getGroupWithUsersUseCase.execute(GetGroupWithUsersUseCase.Input(permission.group.groupId)).groupWithUsers
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
        groupPermission = PermissionModelUi.GroupPermissionModel(permission, groupPermission.group.copy())
    }

    override fun saveButtonClick() {
        view?.apply {
            setUpdatedPermissionResult(groupPermission)
            navigateBack()
        }
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }
}
