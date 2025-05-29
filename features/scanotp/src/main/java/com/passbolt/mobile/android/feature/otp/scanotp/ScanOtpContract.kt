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

package com.passbolt.mobile.android.feature.otp.scanotp

import com.passbolt.mobile.android.core.mvp.BaseContract
import com.passbolt.mobile.android.core.qrscan.analyzer.BarcodeScanResult
import com.passbolt.mobile.android.ui.OtpParseResult
import kotlinx.coroutines.flow.StateFlow

interface ScanOtpContract {
    interface View : BaseContract.View {
        fun startAnalysis()

        fun showStartCameraError()

        fun scanResultChannel(): StateFlow<BarcodeScanResult>

        fun showBarcodeScanError(message: String?)

        fun showMultipleCodesInRange()

        fun showCenterCameraOnBarcode()

        fun setFlagSecure()

        fun removeFlagSecure()

        fun showNotAnOtpBarcode()

        fun setResultAndNavigateBack(parserResult: OtpParseResult.OtpQr.TotpQr)

        fun showCameraRequiredDialog()

        fun requestCameraPermission()

        fun showCameraPermissionRequiredDialog()

        fun navigateToAppSettings()

        fun navigateToScanOtpSuccess(parserResult: OtpParseResult.OtpQr.TotpQr)
    }

    interface Presenter : BaseContract.Presenter<View> {
        fun startCameraError(exc: Exception)

        fun viewResumed()

        fun viewPaused()

        fun cameraPermissionGranted()

        fun permissionRejectedClick()

        fun settingsButtonClick()

        fun argsRetrieved(mode: ScanOtpMode)
    }
}
