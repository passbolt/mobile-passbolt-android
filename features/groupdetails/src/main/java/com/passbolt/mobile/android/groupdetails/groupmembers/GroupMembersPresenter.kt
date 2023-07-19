package com.passbolt.mobile.android.groupdetails.groupmembers

import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.commongroups.usecase.db.GetGroupWithUsersUseCase
import com.passbolt.mobile.android.ui.UserModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class GroupMembersPresenter(
    private val getGroupWithUsersUseCase: GetGroupWithUsersUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : GroupMembersContract.Presenter,
    BaseAuthenticatedPresenter<GroupMembersContract.View>(coroutineLaunchContext) {

    override var view: GroupMembersContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun argsReceived(groupId: String) {
        scope.launch {
            getGroupWithUsersUseCase.execute(GetGroupWithUsersUseCase.Input(groupId)).let {
                view?.apply {
                    showGroupName(it.groupWithUsers.group.groupName)
                    showGroupMembers(it.groupWithUsers.users)
                }
            }
        }
    }

    override fun groupMemberClick(userModel: UserModel) {
        view?.navigateToGroupMemberDetails(userModel.id)
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }
}
