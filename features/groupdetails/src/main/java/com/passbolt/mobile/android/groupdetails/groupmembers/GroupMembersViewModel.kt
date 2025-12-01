/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

package com.passbolt.mobile.android.groupdetails.groupmembers

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.core.commongroups.usecase.db.GetGroupWithUsersUseCase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticatedViewModel
import com.passbolt.mobile.android.groupdetails.groupmembers.GroupMembersIntent.GoBack
import com.passbolt.mobile.android.groupdetails.groupmembers.GroupMembersIntent.GoToMemberDetails
import com.passbolt.mobile.android.groupdetails.groupmembers.GroupMembersIntent.Initialize
import com.passbolt.mobile.android.groupdetails.groupmembers.GroupMembersSideEffect.NavigateToMemberDetails
import com.passbolt.mobile.android.groupdetails.groupmembers.GroupMembersSideEffect.NavigateUp
import kotlinx.coroutines.launch

internal class GroupMembersViewModel(
    private val getGroupWithUsersUseCase: GetGroupWithUsersUseCase,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) : AuthenticatedViewModel<GroupMembersState, GroupMembersSideEffect>(GroupMembersState()) {
    fun onIntent(intent: GroupMembersIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateUp)
            is Initialize -> loadGroupMembers(intent.groupId)
            is GoToMemberDetails -> emitSideEffect(NavigateToMemberDetails(intent.userId))
        }
    }

    private fun loadGroupMembers(groupId: String) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            val result = getGroupWithUsersUseCase.execute(GetGroupWithUsersUseCase.Input(groupId))
            updateViewState {
                copy(
                    groupName = result.groupWithUsers.group.groupName,
                    members = result.groupWithUsers.users,
                )
            }
        }
    }
}
