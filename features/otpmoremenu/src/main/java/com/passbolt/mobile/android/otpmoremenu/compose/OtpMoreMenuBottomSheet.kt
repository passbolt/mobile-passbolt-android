package com.passbolt.mobile.android.otpmoremenu.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.compose.bottomsheet.BottomSheetHeader
import com.passbolt.mobile.android.core.ui.compose.menu.OpenableSettingsItem
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuIntent.Close
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuIntent.CopyOtp
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuIntent.DeleteOtp
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuIntent.EditOtp
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuIntent.Initialize
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuIntent.ShowOtp
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuSideEffect.Dismiss
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuSideEffect.InvokeCopyOtp
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuSideEffect.InvokeDeleteOtp
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuSideEffect.InvokeEditOtp
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuSideEffect.InvokeShowOtp
import org.koin.androidx.compose.koinViewModel
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

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
fun OtpMoreMenuBottomSheet(
    resourceId: String,
    resourceName: String,
    onCopyOtp: () -> Unit,
    onEditOtp: () -> Unit,
    onDeleteOtp: () -> Unit,
    onDismissRequest: () -> Unit,
    onShowOtp: (() -> Unit)? = null,
    viewModel: OtpMoreMenuViewModel = koinViewModel(),
) {
    viewModel.onIntent(Initialize(resourceId, resourceName, onShowOtp != null))

    val state by viewModel.viewState.collectAsState()

    OtpMoreMenuBottomSheet(
        onIntent = viewModel::onIntent,
        onDismissRequest = onDismissRequest,
        state = state,
    )

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            Dismiss -> onDismissRequest()
            InvokeShowOtp -> onShowOtp?.invoke()
            InvokeCopyOtp -> onCopyOtp()
            InvokeEditOtp -> onEditOtp()
            InvokeDeleteOtp -> onDeleteOtp()
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun OtpMoreMenuBottomSheet(
    onIntent: (OtpMoreMenuIntent) -> Unit,
    onDismissRequest: () -> Unit,
    state: OtpMoreMenuState,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = colorResource(R.color.elevated_background),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            BottomSheetHeader(
                title = state.title,
                onClose = { onIntent(Close) },
            )

            if (state.showShowOtpButton) {
                OpenableSettingsItem(
                    title = stringResource(LocalizationR.string.otp_more_show_otp),
                    iconPainter = painterResource(CoreUiR.drawable.ic_eye_invisible),
                    onClick = { onIntent(ShowOtp) },
                    opensInternally = false,
                )
            }

            OpenableSettingsItem(
                title = stringResource(LocalizationR.string.otp_more_copy_otp),
                iconPainter = painterResource(CoreUiR.drawable.ic_copy),
                onClick = { onIntent(CopyOtp) },
                opensInternally = false,
            )

            if (state.showSeparator) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = colorResource(id = CoreUiR.color.divider),
                    thickness = 1.dp,
                )
            }

            if (state.showEditButton) {
                OpenableSettingsItem(
                    title = stringResource(LocalizationR.string.otp_more_edit_resource),
                    iconPainter = painterResource(CoreUiR.drawable.ic_edit),
                    onClick = { onIntent(EditOtp) },
                    opensInternally = false,
                )
            }

            if (state.showDeleteButton) {
                OpenableSettingsItem(
                    title = stringResource(LocalizationR.string.otp_more_delete_otp),
                    iconPainter = painterResource(CoreUiR.drawable.ic_trash),
                    onClick = { onIntent(DeleteOtp) },
                    opensInternally = false,
                )
            }
        }
    }
}
