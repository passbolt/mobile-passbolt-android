package com.passbolt.mobile.android.createresourcemenu.compose

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.createresourcemenu.compose.CreateResourceMenuIntent.Close
import com.passbolt.mobile.android.createresourcemenu.compose.CreateResourceMenuIntent.CreateFolder
import com.passbolt.mobile.android.createresourcemenu.compose.CreateResourceMenuIntent.CreatePassword
import com.passbolt.mobile.android.createresourcemenu.compose.CreateResourceMenuIntent.CreateTotp
import com.passbolt.mobile.android.createresourcemenu.compose.CreateResourceMenuIntent.Initialize
import com.passbolt.mobile.android.createresourcemenu.compose.CreateResourceMenuSideEffect.Dismiss
import com.passbolt.mobile.android.createresourcemenu.compose.CreateResourceMenuSideEffect.InvokeCreateFolder
import com.passbolt.mobile.android.createresourcemenu.compose.CreateResourceMenuSideEffect.InvokeCreatePassword
import com.passbolt.mobile.android.createresourcemenu.compose.CreateResourceMenuSideEffect.InvokeCreateTotp
import com.passbolt.mobile.android.createresourcemenu.usecase.CreateCreateResourceMenuModelUseCase
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
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

class CreateResourceMenuViewModel(
    private val createCreateResourceMoreMenuModelUseCase: CreateCreateResourceMenuModelUseCase,
) : SideEffectViewModel<CreateResourceMenuState, CreateResourceMenuSideEffect>(CreateResourceMenuState()) {
    fun onIntent(intent: CreateResourceMenuIntent) {
        when (intent) {
            Close -> emitSideEffect(Dismiss)
            CreatePassword -> emitSideEffect(InvokeCreatePassword)
            CreateTotp -> emitSideEffect(InvokeCreateTotp)
            CreateFolder -> emitSideEffect(InvokeCreateFolder)
            is Initialize -> initialize(intent.homeDisplayViewModel)
        }
    }

    private fun initialize(homeDisplayViewModel: HomeDisplayViewModel?) {
        viewModelScope.launch {
            createCreateResourceMoreMenuModelUseCase
                .execute(
                    CreateCreateResourceMenuModelUseCase.Input(homeDisplayViewModel),
                ).model
                .apply {
                    updateViewState {
                        copy(
                            showPasswordButton = isPasswordEnabled,
                            showTotpButton = isTotpEnabled,
                            showFoldersButton = isFolderEnabled,
                        )
                    }
                }
        }
    }
}
