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

package com.passbolt.mobile.android.groupdetails.groupmemberdetails

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.users.usecase.db.GetLocalUserUseCase
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticatedViewModel
import com.passbolt.mobile.android.groupdetails.groupmemberdetails.GroupMemberDetailsIntent.GoBack
import com.passbolt.mobile.android.groupdetails.groupmemberdetails.GroupMemberDetailsIntent.Initialize
import com.passbolt.mobile.android.groupdetails.groupmemberdetails.GroupMemberDetailsSideEffect.NavigateUp
import kotlinx.coroutines.launch

internal class GroupMemberDetailsViewModel(
    private val getLocalUserUseCase: GetLocalUserUseCase,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) : AuthenticatedViewModel<GroupMemberDetailsState, GroupMemberDetailsSideEffect>(GroupMemberDetailsState()) {
    fun onIntent(intent: GroupMemberDetailsIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateUp)
            is Initialize -> loadUserData(intent.userId)
        }
    }

    private fun loadUserData(userId: String) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            val user = getLocalUserUseCase.execute(GetLocalUserUseCase.Input(userId)).user
            updateViewState {
                copy(
                    userName = user.userName,
                    firstName = user.profile.firstName.orEmpty(),
                    lastName = user.profile.lastName.orEmpty(),
                    avatarUrl = user.profile.avatarUrl,
                    fingerprint = user.gpgKey.fingerprint,
                )
            }
        }
    }
}
