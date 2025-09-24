package com.passbolt.mobile.android.otpmoremenu.compose

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuIntent.Close
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuIntent.CopyOtp
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuIntent.DeleteOtp
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuIntent.EditOtp
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuIntent.Initialize
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuIntent.ShowOtp
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuSideEffect.Dismiss
import com.passbolt.mobile.android.otpmoremenu.usecase.CreateOtpMoreMenuModelUseCase
import kotlinx.coroutines.launch

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

class OtpMoreMenuViewModel(
    private val createOtpMoreMenuModelUseCase: CreateOtpMoreMenuModelUseCase,
    private val fullDataRefreshExecutor: FullDataRefreshExecutor,
) : SideEffectViewModel<OtpMoreMenuState, OtpMoreMenuSideEffect>(OtpMoreMenuState()) {
    fun onIntent(intent: OtpMoreMenuIntent) {
        when (intent) {
            Close -> emitSideEffect(Dismiss)
            CopyOtp -> emitSideEffect(OtpMoreMenuSideEffect.InvokeCopyOtp)
            DeleteOtp -> emitSideEffect(OtpMoreMenuSideEffect.InvokeDeleteOtp)
            EditOtp -> emitSideEffect(OtpMoreMenuSideEffect.InvokeEditOtp)
            ShowOtp -> emitSideEffect(OtpMoreMenuSideEffect.InvokeShowOtp)
            is Initialize -> initialize(intent)
        }
    }

    private fun initialize(initialize: Initialize) {
        updateViewState { copy(title = initialize.resourceName, showShowOtpButton = initialize.canShowTotp) }
        viewModelScope.launch {
            val menuModel =
                createOtpMoreMenuModelUseCase
                    .execute(
                        CreateOtpMoreMenuModelUseCase.Input(initialize.resourceId),
                    ).otpMoreMenuModel

            fullDataRefreshExecutor.awaitFinish()

            updateViewState {
                copy(
                    showDeleteButton = menuModel.canDelete,
                    showEditButton = menuModel.canEdit,
                    showSeparator = menuModel.canEdit || menuModel.canDelete,
                )
            }
        }
    }
}
