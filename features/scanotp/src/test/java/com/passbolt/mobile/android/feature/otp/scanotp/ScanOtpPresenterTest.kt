package com.passbolt.mobile.android.feature.otp.scanotp

import com.passbolt.mobile.android.core.qrscan.analyzer.BarcodeScanResult
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.OtpParseResult.UserResolvableError.ErrorType.MULTIPLE_BARCODES
import com.passbolt.mobile.android.ui.OtpParseResult.UserResolvableError.ErrorType.NOT_A_OTP_QR
import com.passbolt.mobile.android.ui.OtpParseResult.UserResolvableError.ErrorType.NO_BARCODES_IN_RANGE
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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
class ScanOtpPresenterTest : KoinTest {
    private val presenter: ScanOtpContract.Presenter by inject()
    private var view: ScanOtpContract.View = mock()
    private val scanningFlow = MutableStateFlow<BarcodeScanResult>(
        BarcodeScanResult.NoBarcodeInRange
    )

    private val parseFlow = MutableStateFlow<OtpParseResult>(
        OtpParseResult.UserResolvableError(NO_BARCODES_IN_RANGE)
    )


    @ExperimentalCoroutinesApi
    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(scanOtpTestModule)
    }

    @Before
    fun setup() {
        whenever(view.scanResultChannel()).thenReturn(scanningFlow)
        qrParser.stub {
            onBlocking { startParsing(any()) }.then { }
            on { parseResultFlow }.doReturn(parseFlow)
        }
    }

    @Test
    fun `missing camera should show proper dialog`() {
        whenever(cameraInformationProvider.isCameraAvailable()).thenReturn(false)

        presenter.attach(view)

        verify(view).showCameraRequiredDialog()
    }

    @Test
    fun `missing camera permission should request camera permission`() {
        whenever(cameraInformationProvider.isCameraAvailable()).thenReturn(true)
        whenever(cameraInformationProvider.isCameraPermissionGranted()).thenReturn(false)

        presenter.attach(view)

        verify(view).requestCameraPermission()
    }

    @Test
    fun `rejecting camera permissions should display information dialog`() {
        presenter.attach(view)

        presenter.permissionRejectedClick()

        verify(view).showCameraPermissionRequiredDialog()
    }

    @Test
    fun `click settings button should open settings screen`() {
        presenter.attach(view)

        presenter.settingsButtonClick()

        verify(view).navigateToAppSettings()
    }

    @Test
    fun `view should show correct user resolvable error tooltips`() = runTest {
        whenever(cameraInformationProvider.isCameraAvailable()).thenReturn(true)
        whenever(cameraInformationProvider.isCameraPermissionGranted()).thenReturn(true)

        presenter.attach(view)

        parseFlow.emit(OtpParseResult.UserResolvableError(NO_BARCODES_IN_RANGE))
        parseFlow.emit(OtpParseResult.UserResolvableError(MULTIPLE_BARCODES))
        parseFlow.emit(OtpParseResult.UserResolvableError(NOT_A_OTP_QR))

        verify(view, times(2)).showCenterCameraOnBarcode()
        verify(view).showMultipleCodesInRange()
        verify(view).showNotAnOtpBarcode()
    }

    @Test
    fun `successful scan should finish scanning and show success`() = runTest {
        whenever(cameraInformationProvider.isCameraAvailable()).thenReturn(true)
        whenever(cameraInformationProvider.isCameraPermissionGranted()).thenReturn(true)

        presenter.attach(view)

        val successfulResult = OtpParseResult.OtpQr.TotpQr(
            label = "label",
            secret = "secret",
            issuer = "issuer",
            algorithm = OtpParseResult.OtpQr.Algorithm.SHA1,
            digits = 6,
            period = 30
        )
        parseFlow.emit(successfulResult)

        verify(view).setResultAndNavigateBack(successfulResult)
    }

    @Test
    fun `view should navigate to scanning error after scan failure`() = runTest {
        whenever(cameraInformationProvider.isCameraAvailable()).thenReturn(true)
        whenever(cameraInformationProvider.isCameraPermissionGranted()).thenReturn(true)

        presenter.attach(view)

        val errorMessage = "Exception occurred"
        parseFlow.emit(OtpParseResult.Failure(RuntimeException(errorMessage)))

        verify(view).showBarcodeScanError(errorMessage)
    }
}
