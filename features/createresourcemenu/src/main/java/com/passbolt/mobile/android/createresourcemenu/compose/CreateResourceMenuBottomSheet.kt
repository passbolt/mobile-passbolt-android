package com.passbolt.mobile.android.createresourcemenu.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.ui.compose.bottomsheet.BottomSheetHeader
import com.passbolt.mobile.android.core.ui.compose.menu.OpenableSettingsItem
import com.passbolt.mobile.android.createresourcemenu.compose.CreateResourceMenuIntent.Close
import com.passbolt.mobile.android.createresourcemenu.compose.CreateResourceMenuIntent.CreateFolder
import com.passbolt.mobile.android.createresourcemenu.compose.CreateResourceMenuIntent.CreatePassword
import com.passbolt.mobile.android.createresourcemenu.compose.CreateResourceMenuIntent.CreateTotp
import com.passbolt.mobile.android.createresourcemenu.compose.CreateResourceMenuIntent.Initialize
import com.passbolt.mobile.android.createresourcemenu.compose.CreateResourceMenuSideEffect.Dismiss
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import org.koin.androidx.compose.koinViewModel
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR
import com.passbolt.mobile.android.createresourcemenu.compose.CreateResourceMenuSideEffect.InvokeCreateFolder as CreateFolderEffect
import com.passbolt.mobile.android.createresourcemenu.compose.CreateResourceMenuSideEffect.InvokeCreatePassword as CreatePasswordEffect
import com.passbolt.mobile.android.createresourcemenu.compose.CreateResourceMenuSideEffect.InvokeCreateTotp as CreateTotpEffect

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateResourceMenuBottomSheet(
    onCreatePassword: () -> Unit,
    onCreateTotp: () -> Unit,
    onDismissRequest: () -> Unit,
    onCreateFolder: (() -> Unit)? = null,
    homeDisplayViewModel: HomeDisplayViewModel? = null,
    viewModel: CreateResourceMenuViewModel = koinViewModel(),
) {
    viewModel.onIntent(Initialize(homeDisplayViewModel))

    val state by viewModel.viewState.collectAsState()

    CreateResourceMenuBottomSheet(
        onIntent = viewModel::onIntent,
        onDismissRequest = onDismissRequest,
        state = state,
    )

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            Dismiss -> onDismissRequest()
            CreatePasswordEffect -> onCreatePassword()
            CreateTotpEffect -> onCreateTotp()
            CreateFolderEffect -> onCreateFolder?.invoke()
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CreateResourceMenuBottomSheet(
    onIntent: (CreateResourceMenuIntent) -> Unit,
    onDismissRequest: () -> Unit,
    state: CreateResourceMenuState,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            BottomSheetHeader(
                title = stringResource(LocalizationR.string.create_resource_menu_create_a_resource),
                onClose = { onIntent(Close) },
            )

            if (state.showPasswordButton) {
                OpenableSettingsItem(
                    title = stringResource(LocalizationR.string.create_resource_menu_create_password),
                    iconPainter = painterResource(CoreUiR.drawable.ic_key),
                    onClick = { onIntent(CreatePassword) },
                    opensInternally = false,
                )
            }

            if (state.showTotpButton) {
                OpenableSettingsItem(
                    title = stringResource(LocalizationR.string.create_resource_menu_create_totp),
                    iconPainter = painterResource(CoreUiR.drawable.ic_time_lock),
                    onClick = { onIntent(CreateTotp) },
                    opensInternally = false,
                )
            }

            if (state.showFoldersButton) {
                OpenableSettingsItem(
                    title = stringResource(LocalizationR.string.create_resource_menu_create_folder),
                    iconPainter = painterResource(CoreUiR.drawable.ic_folder),
                    onClick = { onIntent(CreateFolder) },
                    opensInternally = false,
                )
            }
        }
    }
}
