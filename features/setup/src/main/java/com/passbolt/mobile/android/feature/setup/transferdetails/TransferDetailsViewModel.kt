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

package com.passbolt.mobile.android.feature.setup.transferdetails

import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.qrscan.CameraInformationProvider
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsIntent.DismissCameraPermissionRequiredDialog
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsIntent.DismissCameraRequiredDialog
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsIntent.GoBack
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsIntent.GoToSettings
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsIntent.GrantCameraPermission
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsIntent.RejectCameraPermission
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsIntent.StartQrCodeScanning
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsSideEffect.NavigateToAppSettings
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsSideEffect.NavigateToScanQr
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsSideEffect.RequestCameraPermission
import org.koin.core.component.KoinComponent

internal class TransferDetailsViewModel(
    private val cameraInformationProvider: CameraInformationProvider,
) : SideEffectViewModel<TransferDetailsState, TransferDetailsSideEffect>(TransferDetailsState()),
    KoinComponent {
    fun onIntent(intent: TransferDetailsIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateBack)
            StartQrCodeScanning -> startQrCodeScanning()
            RejectCameraPermission -> updateViewState { copy(showCameraPermissionRequiredDialog = true) }
            DismissCameraRequiredDialog -> updateViewState { copy(showCameraRequiredDialog = false) }
            DismissCameraPermissionRequiredDialog -> updateViewState { copy(showCameraPermissionRequiredDialog = false) }
            GoToSettings -> emitSideEffect(NavigateToAppSettings)
            GrantCameraPermission -> startQrCodeScanning()
        }
    }

    private fun startQrCodeScanning() {
        when {
            !cameraInformationProvider.isCameraAvailable() -> {
                updateViewState { copy(showCameraRequiredDialog = true) }
            }
            !cameraInformationProvider.isCameraPermissionGranted() -> {
                emitSideEffect(RequestCameraPermission)
            }
            else -> {
                emitSideEffect(NavigateToScanQr)
            }
        }
    }
}
