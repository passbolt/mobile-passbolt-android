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

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.qrscan.CameraInformationProvider
import com.passbolt.mobile.android.feature.otp.scanotp.parser.OtpParseResult
import com.passbolt.mobile.android.feature.otp.scanotp.parser.OtpQrParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import timber.log.Timber

class ScanOtpPresenter(
    private val otpQrParser: OtpQrParser,
    private val cameraInformationProvider: CameraInformationProvider,
    coroutineLaunchContext: CoroutineLaunchContext
) : ScanOtpContract.Presenter {

    override var view: ScanOtpContract.View? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun attach(view: ScanOtpContract.View) {
        super.attach(view)
        if (!cameraInformationProvider.isCameraAvailable()) {
            view.showCameraRequiredDialog()
        } else if (!cameraInformationProvider.isCameraPermissionGranted()) {
            view.requestCameraPermission()
        } else {
            initQrScanning()
        }
    }

    override fun cameraPermissionGranted() {
        initQrScanning()
    }

    private fun initQrScanning() {
        scope.launch {
            launch { otpQrParser.startParsing(view!!.scanResultChannel()) }
            launch {
                otpQrParser.parseResultFlow.collect { processParseResult(it) }
            }
        }
        view?.startAnalysis()
    }

    private fun processParseResult(parserResult: OtpParseResult) {
        when (parserResult) {
            is OtpParseResult.Failure -> parserFailure(parserResult.exception)
            is OtpParseResult.OtpQr -> when (parserResult) {
                is OtpParseResult.OtpQr.TotpQr -> {
                    scope.coroutineContext.cancelChildren()
                    view?.setResultAndNavigateBack(parserResult)
                }
                is OtpParseResult.OtpQr.HotpQr -> {} // HOTP is not supported yet
            }
            is OtpParseResult.UserResolvableError -> when (parserResult.errorType) {
                OtpParseResult.UserResolvableError.ErrorType.MULTIPLE_BARCODES -> view?.showMultipleCodesInRange()
                OtpParseResult.UserResolvableError.ErrorType.NO_BARCODES_IN_RANGE -> view?.showCenterCameraOnBarcode()
                OtpParseResult.UserResolvableError.ErrorType.NOT_A_OTP_QR -> view?.showNotAnOtpBarcode()
            }
            is OtpParseResult.ScanFailure -> view?.showBarcodeScanError(parserResult.exception?.message)
            is OtpParseResult.IncompleteOtpParameters.IncompleteHotpParametrs -> {} // HOTP is not supported yet
            is OtpParseResult.IncompleteOtpParameters.IncompleteTotpParameters -> {
                Timber.d("Incomplete TOTP parameters")
            }
        }
    }

    private fun parserFailure(exception: Throwable?) {
        exception?.let { Timber.e(it) }
        view?.showBarcodeScanError(exception?.message)
    }

    override fun permissionRejectedClick() {
        view?.showCameraPermissionRequiredDialog()
    }

    override fun startCameraError(exc: Exception) {
        Timber.e(exc)
        view?.showStartCameraError()
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super.detach()
    }

    override fun viewResumed() {
        view?.setFlagSecure()
    }

    override fun viewPaused() {
        view?.removeFlagSecure()
    }

    override fun settingsButtonClick() {
        view?.navigateToAppSettings()
    }
}
