package com.passbolt.mobile.android.feature.setup.scanqr.parser

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.qrscan.analyzer.BarcodeScanResult
import com.passbolt.mobile.android.feature.setup.di.testModule
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ScanQrParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import java.io.IOException
import kotlin.time.ExperimentalTime

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

@ExperimentalTime
@ExperimentalCoroutinesApi
class QrParserTest : KoinTest {

    private val scanQrParser: ScanQrParser by inject()
    private val mockScanningFlow = MutableSharedFlow<BarcodeScanResult>()
    private lateinit var scanningJob: Job

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testModule, testParserModule)
    }

    @Test
    fun `parser should parse user resolvable states correct`() = runBlockingTest {
        scanningJob = launch {
            scanQrParser.startParsing(mockScanningFlow)
        }

        launch {
            scanQrParser.parseResultFlow.test {
                assertNoBarcodesInRange(expectItem())
                assertMultipleBarcodesItem(expectItem())
                assertNotAPassboltQrCode(expectItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
        mockScanningFlow.emit(BarcodeScanResult.MultipleBarcodes)
        mockScanningFlow.emit(BarcodeScanResult.SingleBarcode(SAMPLE_BYTE_ARRAY))

        scanningJob.cancel()
    }

    private fun assertNotAPassboltQrCode(item: ParseResult) {
        assertThat(item).isInstanceOf(ParseResult.UserResolvableError::class.java)
        assertThat((item as ParseResult.UserResolvableError).errorType)
            .isEqualTo(ParseResult.UserResolvableError.ErrorType.NOT_A_PASSBOLT_QR)
    }

    private fun assertMultipleBarcodesItem(item: ParseResult) {
        assertThat(item).isInstanceOf(ParseResult.UserResolvableError::class.java)
        assertThat((item as ParseResult.UserResolvableError).errorType)
            .isEqualTo(ParseResult.UserResolvableError.ErrorType.MULTIPLE_BARCODES)
    }

    private fun assertNoBarcodesInRange(item: ParseResult) {
        assertThat(item).isInstanceOf(ParseResult.UserResolvableError::class.java)
        assertThat((item as ParseResult.UserResolvableError).errorType)
            .isEqualTo(ParseResult.UserResolvableError.ErrorType.NO_BARCODES_IN_RANGE)
    }

    @Test
    fun `parser should parse passbolt qr correct`() = runBlockingTest {
        scanningJob = launch {
            scanQrParser.startParsing(mockScanningFlow)
        }

        launch {
            scanQrParser.parseResultFlow.test {
                assertNoBarcodesInRange(expectItem())
                assertPassboltQrFirstPage(expectItem())
                assertPassboltQrSubsequentPage(expectItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
        mockScanningFlow.emit(BarcodeScanResult.SingleBarcode(PASSBOLT_FIRST_PAGE_SCAN))
        mockScanningFlow.emit(BarcodeScanResult.SingleBarcode(PASSBOLT_SUBSEQUENT_PAGE_SCAN))

        scanningJob.cancel()
    }

    @Test
    fun `parser should not react to already parsed first page qr`() = runBlockingTest {
        scanningJob = launch {
            scanQrParser.startParsing(mockScanningFlow)
        }

        launch {
            scanQrParser.parseResultFlow.test {
                assertNoBarcodesInRange(expectItem())
                assertPassboltQrFirstPage(expectItem())
                assertNoBarcodesInRange(expectItem())
                expectComplete()
            }
        }
        mockScanningFlow.emit(BarcodeScanResult.SingleBarcode(PASSBOLT_FIRST_PAGE_SCAN))
        mockScanningFlow.emit(BarcodeScanResult.SingleBarcode(PASSBOLT_FIRST_PAGE_SCAN))
        mockScanningFlow.emit(BarcodeScanResult.NoBarcodeInRange)
        mockScanningFlow.emit(BarcodeScanResult.SingleBarcode(PASSBOLT_FIRST_PAGE_SCAN))
        mockScanningFlow.emit(BarcodeScanResult.SingleBarcode(PASSBOLT_FIRST_PAGE_SCAN))

        scanningJob.cancel()
    }

    @Test
    fun `parser should not react to already parsed subsequent page qr`() = runBlockingTest {
        scanningJob = launch {
            scanQrParser.startParsing(mockScanningFlow)
        }

        launch {
            scanQrParser.parseResultFlow.test {
                assertNoBarcodesInRange(expectItem())
                assertPassboltQrFirstPage(expectItem())
                assertPassboltQrSubsequentPage(expectItem())
                assertNoBarcodesInRange(expectItem())
                expectComplete()
            }
        }
        mockScanningFlow.emit(BarcodeScanResult.SingleBarcode(PASSBOLT_FIRST_PAGE_SCAN))
        mockScanningFlow.emit(BarcodeScanResult.SingleBarcode(PASSBOLT_SUBSEQUENT_PAGE_SCAN))
        mockScanningFlow.emit(BarcodeScanResult.NoBarcodeInRange)
        mockScanningFlow.emit(BarcodeScanResult.SingleBarcode(PASSBOLT_SUBSEQUENT_PAGE_SCAN))
        mockScanningFlow.emit(BarcodeScanResult.SingleBarcode(PASSBOLT_SUBSEQUENT_PAGE_SCAN))

        scanningJob.cancel()
    }

    @Test
    fun `parser should report error when subsequent page scaned without first page`() = runBlockingTest {
        scanningJob = launch {
            scanQrParser.startParsing(mockScanningFlow)
        }

        launch {
            scanQrParser.parseResultFlow.test {
                assertNoBarcodesInRange(expectItem())
                assertParserError(expectItem())
                expectComplete()
            }
        }
        mockScanningFlow.emit(BarcodeScanResult.SingleBarcode(PASSBOLT_SUBSEQUENT_PAGE_SCAN))

        scanningJob.cancel()
    }

    private fun assertPassboltQrFirstPage(item: ParseResult) {
        assertThat(item).isInstanceOf(ParseResult.PassboltQr.FirstPage::class.java)
    }


    private fun assertPassboltQrSubsequentPage(item: ParseResult) {
        assertThat(item).isInstanceOf(ParseResult.PassboltQr.SubsequentPage::class.java)
    }

    private fun assertParserError(item: ParseResult) {
        assertThat(item).isInstanceOf(ParseResult.Failure::class.java)
    }

    @Test
    fun `parser should handle exceptions correct`() = runBlockingTest {
        scanningJob = launch {
            scanQrParser.startParsing(mockScanningFlow)
        }

        launch {
            scanQrParser.parseResultFlow.test {
                assertNoBarcodesInRange(expectItem())
                assertFailWithScanException(expectItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
        mockScanningFlow.emit(BarcodeScanResult.Failure(TEST_EXCEPTION))

        scanningJob.cancel()
    }

    private fun assertFailWithScanException(item: ParseResult) {
        assertThat(item).isInstanceOf(ParseResult.ScanFailure::class.java)
        val exception = (item as ParseResult.ScanFailure).exception
        assertThat(exception).isEqualTo(TEST_EXCEPTION)
    }

    private companion object {
        private const val TEST_EXCEPTION_MESSAGE = "Test exception"
        private val TEST_EXCEPTION = IOException(TEST_EXCEPTION_MESSAGE)
    }
}
