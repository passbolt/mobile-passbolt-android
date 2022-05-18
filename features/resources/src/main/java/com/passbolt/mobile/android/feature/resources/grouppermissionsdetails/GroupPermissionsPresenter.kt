package com.passbolt.mobile.android.feature.resources.grouppermissionsdetails

import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.database.impl.groups.GetGroupWithUsersUseCase
import com.passbolt.mobile.android.feature.resources.permissionavatarlist.UsersDatasetCreator
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

    private lateinit var groupId: String

    override fun argsRetrieved(
        groupId: String,
        permission: ResourcePermission,
        membersRecyclerWidth: Int,
        membersItemWidth: Float
    ) {
        this.groupId = groupId
        view?.showPermission(permission)
        scope.launch {
            getGroupWithUsersUseCase.execute(GetGroupWithUsersUseCase.Input(groupId)).groupWithUsers.let {
                view?.apply {
                    showGroupName(it.group.groupName)

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
    }

    override fun groupMembersRecyclerClick() {
        view?.navigateToGroupMembers(groupId)
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }
}
