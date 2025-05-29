package com.passbolt.mobile.android.feature.setup.scanqr

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.accounts.usecase.accounts.CheckAccountExistsUseCase
import com.passbolt.mobile.android.core.accounts.usecase.privatekey.SavePrivateKeyUseCase
import com.passbolt.mobile.android.core.qrscan.analyzer.BarcodeScanResult
import com.passbolt.mobile.android.dto.response.qrcode.QrFirstPageDto
import com.passbolt.mobile.android.dto.response.qrcode.ReservedBytesDto
import com.passbolt.mobile.android.feature.setup.di.testModule
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult.UserResolvableError.ErrorType.MULTIPLE_BARCODES
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult.UserResolvableError.ErrorType.NOT_A_PASSBOLT_QR
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult.UserResolvableError.ErrorType.NO_BARCODES_IN_RANGE
import com.passbolt.mobile.android.feature.setup.scanqr.usecase.UpdateTransferUseCase
import com.passbolt.mobile.android.feature.setup.summary.ResultStatus
import com.passbolt.mobile.android.ui.UpdateTransferModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

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

@ExperimentalCoroutinesApi
class ScanQrPresenterTest : KoinTest {
    private val presenter: ScanQrContract.Presenter by inject()
    private var view: ScanQrContract.View = mock()

    private val scanningFlow =
        MutableStateFlow<BarcodeScanResult>(
            BarcodeScanResult.NoBarcodeInRange,
        )

    private val parseFlow =
        MutableStateFlow<ParseResult>(
            ParseResult.UserResolvableError(NO_BARCODES_IN_RANGE),
        )

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testModule, testScanQrModule)
        }

    @Before
    fun setUp() {
        whenever(view.scanResultChannel()).thenReturn(scanningFlow)
        qrParser.stub {
            onBlocking { startParsing(any()) }.then { }
            on { parseResultFlow }.doReturn(parseFlow)
        }
        presenter.attach(view)
    }

    @After
    fun resetMocks() {
        reset(checkAccountExistsUseCase, updateTransferUseCase)
    }

    @Test
    fun `click information dialog should display help menu`() {
        presenter.argsRetrieved(null)
        presenter.infoIconClick()
        verify(view).showHelpMenu()
    }

    @Test
    fun `click back should display proper dialog`() {
        presenter.argsRetrieved(null)
        presenter.backClick()
        verify(view).showExitConfirmation()
    }

    @Test
    fun `click exit confirmation should navigate back`() {
        presenter.argsRetrieved(null)
        presenter.exitConfirmClick()
        verify(view).navigateBack()
    }

    @Test
    fun `camera error should display start camera error tooltip`() {
        presenter.argsRetrieved(null)
        presenter.startCameraError(Exception())
        verify(view).showStartCameraError()
    }

    @Test
    fun `view should show correct user resolvable error tooltips`() =
        runTest {
            presenter.argsRetrieved(null)

            parseFlow.emit(ParseResult.UserResolvableError(NO_BARCODES_IN_RANGE))
            parseFlow.emit(ParseResult.UserResolvableError(MULTIPLE_BARCODES))
            parseFlow.emit(ParseResult.UserResolvableError(NOT_A_PASSBOLT_QR))

            verify(view, times(2)).showCenterCameraOnBarcode()
            verify(view).showMultipleCodesInRange()
            verify(view).showNotAPassboltQr()
        }

    @Test
    fun `view should initialize progress and show keep going after first page scan`() =
        runTest {
            presenter.argsRetrieved(null)

            whenever(uuidProvider.get()).doReturn(testUserId.toString())
            whenever(checkAccountExistsUseCase.execute(any())).doReturn(CheckAccountExistsUseCase.Output(false))
            whenever(httpsVerifier.isHttps(anyOrNull())).thenReturn(true)
            whenever(updateTransferUseCase.execute(any())).doReturn(
                UpdateTransferUseCase.Output.Success(
                    UpdateTransferModel("id", null, null, null, null),
                ),
            )

            parseFlow.emit(ParseResult.PassboltQr.FirstPage(FIRST_PAGE_RESERVED_BYTES_DTO, FIRST_PAGE_CONTENT))

            verify(view).initializeProgress(TOTAL_PAGES)
            verify(view).showKeepGoing()
        }

    @Test
    fun `view should navigate to scanning error after scan failure`() =
        runTest {
            presenter.argsRetrieved(null)

            parseFlow.emit(ParseResult.Failure())

            argumentCaptor<ResultStatus>().apply {
                verify(view).navigateToSummary(capture())
                assertThat(firstValue).isInstanceOf(ResultStatus.Failure::class.java)
            }
        }

    @Test
    fun `view should navigate to scanning success after scanning finished`() =
        runTest {
            presenter.argsRetrieved(null)

            whenever(uuidProvider.get()).doReturn(testUserId.toString())
            whenever(savePrivateKeyUseCase.execute(any())).doReturn(SavePrivateKeyUseCase.Output.Success)
            whenever(checkAccountExistsUseCase.execute(any())).doReturn(CheckAccountExistsUseCase.Output(false))
            whenever(httpsVerifier.isHttps(anyOrNull())).thenReturn(true)
            whenever(updateTransferUseCase.execute(any())).doReturn(
                UpdateTransferUseCase.Output.Success(
                    UpdateTransferModel("id", null, null, null, null),
                ),
            )
            parseFlow.emit(ParseResult.PassboltQr.FirstPage(FIRST_PAGE_RESERVED_BYTES_DTO, FIRST_PAGE_CONTENT))
            parseFlow.emit(ParseResult.FinishedWithSuccess("key"))

            verify(view).initializeProgress(TOTAL_PAGES)
            verify(view).showKeepGoing()
            argumentCaptor<ResultStatus>().apply {
                verify(view).navigateToSummary(capture())
                assertThat(firstValue).isInstanceOf(ResultStatus.Success::class.java)
            }
        }

    @Test
    fun `view should navigate to failure after scanning non https domain`() =
        runTest {
            presenter.argsRetrieved(null)

            whenever(uuidProvider.get()).doReturn(testUserId.toString())
            whenever(savePrivateKeyUseCase.execute(any())).doReturn(SavePrivateKeyUseCase.Output.Success)
            whenever(checkAccountExistsUseCase.execute(any())).doReturn(CheckAccountExistsUseCase.Output(false))
            whenever(httpsVerifier.isHttps(anyOrNull())).thenReturn(false)
            whenever(updateTransferUseCase.execute(any())).doReturn(
                UpdateTransferUseCase.Output.Success(
                    UpdateTransferModel("id", null, null, null, null),
                ),
            )

            parseFlow.emit(ParseResult.PassboltQr.FirstPage(FIRST_PAGE_RESERVED_BYTES_DTO, FIRST_PAGE_CONTENT))

            argumentCaptor<ResultStatus>().apply {
                verify(view).navigateToSummary(capture())
                assertThat(firstValue).isInstanceOf(ResultStatus.HttpNotSupported::class.java)
            }
            verify(updateTransferUseCase, never()).execute(any())
        }

    @Test
    fun `view should navigate to already linked after scanning existing key`() =
        runTest {
            presenter.argsRetrieved(null)

            whenever(httpsVerifier.isHttps(anyOrNull())).thenReturn(true)
            whenever(uuidProvider.get()).doReturn(testUserId.toString())
            whenever(savePrivateKeyUseCase.execute(any())).doReturn(SavePrivateKeyUseCase.Output.Failure)
            whenever(updateTransferUseCase.execute(any())).doReturn(
                UpdateTransferUseCase.Output.Success(
                    UpdateTransferModel("id", null, null, null, null),
                ),
            )
            whenever(checkAccountExistsUseCase.execute(any())).doReturn(
                CheckAccountExistsUseCase.Output(
                    true,
                    testUserId.toString(),
                ),
            )

            parseFlow.emit(ParseResult.PassboltQr.FirstPage(FIRST_PAGE_RESERVED_BYTES_DTO, FIRST_PAGE_CONTENT))

            argumentCaptor<ResultStatus>().apply {
                verify(view).navigateToSummary(capture())
                assertThat(firstValue).isInstanceOf(ResultStatus.AlreadyLinked::class.java)
            }
        }

    private companion object {
        private const val TOTAL_PAGES = 10
        private val FIRST_PAGE_RESERVED_BYTES_DTO = ReservedBytesDto(1, 0)
        private val testTransferId = UUID.randomUUID()
        private val testUserId = UUID.randomUUID()
        private val FIRST_PAGE_CONTENT =
            QrFirstPageDto(
                testTransferId,
                testUserId,
                TOTAL_PAGES,
                "testAuthToken",
                "testHash",
                "testDomain",
            )
    }
}
