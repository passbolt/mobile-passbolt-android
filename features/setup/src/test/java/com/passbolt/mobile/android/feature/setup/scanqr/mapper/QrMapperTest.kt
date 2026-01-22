package com.passbolt.mobile.android.feature.setup.scanqr.mapper

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.qrscan.analyzer.BarcodeScanResult
import com.passbolt.mobile.android.feature.setup.di.testModule
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.QrScanResultsMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject

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

class QrMapperTest : KoinTest {
    private val qrMapper: QrScanResultsMapper by inject()

    @ExperimentalCoroutinesApi
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testModule, testMapperModule)
        }

    @Test
    fun `mapper should return failure when qr input cannot be parsed`() {
        val scanResult = BarcodeScanResult.SingleBarcode(PASSBOLT_FIRST_MISSING_VALUE_CONFIGURATION_PAGE_SCAN)
        val mapped = qrMapper.apply(scanResult)
        assertThat(mapped).isInstanceOf(ParseResult.Failure::class.java)
    }

    @Test
    fun `mapper should return failure when scan failure encountered`() {
        val scanResult = BarcodeScanResult.Failure(Exception())
        val mapped = qrMapper.apply(scanResult)
        assertThat(mapped).isInstanceOf(ParseResult.ScanFailure::class.java)
    }

    @Test
    fun `mapper should return first page type on first page input`() {
        val scanResult = BarcodeScanResult.SingleBarcode(PASSBOLT_FIRST_PAGE_SCAN)
        val mapped = qrMapper.apply(scanResult)
        assertThat(mapped).isInstanceOf(ParseResult.PassboltQr.FirstPage::class.java)
    }

    @Test
    fun `mapper should return subsequent page type on subsequent page input`() {
        val scanResult = BarcodeScanResult.SingleBarcode(PASSBOLT_SUBSEQUENT_PAGE_SCAN)
        val mapped = qrMapper.apply(scanResult)
        assertThat(mapped).isInstanceOf(ParseResult.PassboltQr.SubsequentPage::class.java)
    }

    @Test
    fun `mapper should return account kit first page input on account kit input`() {
        val scanResult = BarcodeScanResult.SingleBarcode(PASSBOLT_FIRST_PAGE_ACCOUNT_KIT_SCAN)
        val mapped = qrMapper.apply(scanResult)
        assertThat(mapped).isInstanceOf(ParseResult.PassboltQr.AccountKitPage::class.java)
    }

    @Test
    fun `mapper should return user resolvable error when multiple barcodes detected`() {
        val scanResult = BarcodeScanResult.MultipleBarcodes
        val mapped = qrMapper.apply(scanResult)
        assertThat(mapped).isInstanceOf(ParseResult.UserResolvableError::class.java)
        val error = mapped as ParseResult.UserResolvableError
        assertThat(error.errorType).isEqualTo(ParseResult.UserResolvableError.ErrorType.MULTIPLE_BARCODES)
    }

    @Test
    fun `mapper should return user resolvable error when no barcodes in range`() {
        val scanResult = BarcodeScanResult.NoBarcodeInRange
        val mapped = qrMapper.apply(scanResult)
        assertThat(mapped).isInstanceOf(ParseResult.UserResolvableError::class.java)
        val error = mapped as ParseResult.UserResolvableError
        assertThat(error.errorType).isEqualTo(ParseResult.UserResolvableError.ErrorType.NO_BARCODES_IN_RANGE)
    }

    @Test
    fun `mapper should return user resolvable error when barcode is not a passbolt qr`() {
        val scanResult = BarcodeScanResult.SingleBarcode("not a passbolt qr".toByteArray())
        val mapped = qrMapper.apply(scanResult)
        assertThat(mapped).isInstanceOf(ParseResult.UserResolvableError::class.java)
        val error = mapped as ParseResult.UserResolvableError
        assertThat(error.errorType).isEqualTo(ParseResult.UserResolvableError.ErrorType.NOT_A_PASSBOLT_QR)
    }

    @Test
    fun `mapper should return user resolvable error when barcode data is null`() {
        val scanResult = BarcodeScanResult.SingleBarcode(null)
        val mapped = qrMapper.apply(scanResult)
        assertThat(mapped).isInstanceOf(ParseResult.UserResolvableError::class.java)
        val error = mapped as ParseResult.UserResolvableError
        assertThat(error.errorType).isEqualTo(ParseResult.UserResolvableError.ErrorType.NOT_A_PASSBOLT_QR)
    }

    @Test
    fun `mapper should return failure when protocol version is unsupported`() {
        // Version 3 (unsupported) with page 0
        val scanResult = BarcodeScanResult.SingleBarcode("300{\"test\":\"data\"}".toByteArray())
        val mapped = qrMapper.apply(scanResult)
        assertThat(mapped).isInstanceOf(ParseResult.UserResolvableError::class.java)
    }

    @Test
    fun `mapper should return failure when reserved bytes are invalid`() {
        val scanResult = BarcodeScanResult.SingleBarcode("XX0{\"test\":\"data\"}".toByteArray())
        val mapped = qrMapper.apply(scanResult)
        assertThat(mapped).isInstanceOf(ParseResult.UserResolvableError::class.java)
    }

    @Test
    fun `mapper should return failure when qr data is too short`() {
        val scanResult = BarcodeScanResult.SingleBarcode("10".toByteArray())
        val mapped = qrMapper.apply(scanResult)
        assertThat(mapped).isInstanceOf(ParseResult.UserResolvableError::class.java)
    }

    @Test
    fun `mapper should return failure when json payload is invalid`() {
        val scanResult = BarcodeScanResult.SingleBarcode("100{invalid json".toByteArray())
        val mapped = qrMapper.apply(scanResult)
        assertThat(mapped).isInstanceOf(ParseResult.Failure::class.java)
    }

    @Test
    fun `mapper should return subsequent page for page index greater than 0`() {
        // Page 5 of version 1
        val scanResult = BarcodeScanResult.SingleBarcode("105{\"test\":\"data\"}".toByteArray())
        val mapped = qrMapper.apply(scanResult)
        assertThat(mapped).isInstanceOf(ParseResult.PassboltQr.SubsequentPage::class.java)
    }

    @Test
    fun `mapper should correctly parse first page with all required fields`() {
        val scanResult = BarcodeScanResult.SingleBarcode(PASSBOLT_FIRST_PAGE_SCAN)
        val mapped = qrMapper.apply(scanResult)
        assertThat(mapped).isInstanceOf(ParseResult.PassboltQr.FirstPage::class.java)
        val firstPage = mapped as ParseResult.PassboltQr.FirstPage
        assertThat(firstPage.reservedBytesDto.version).isEqualTo(1)
        assertThat(firstPage.reservedBytesDto.page).isEqualTo(0)
    }

    @Test
    fun `mapper should correctly parse subsequent page with correct version and page number`() {
        val scanResult = BarcodeScanResult.SingleBarcode(PASSBOLT_SUBSEQUENT_PAGE_SCAN)
        val mapped = qrMapper.apply(scanResult)
        assertThat(mapped).isInstanceOf(ParseResult.PassboltQr.SubsequentPage::class.java)
        val subsequentPage = mapped as ParseResult.PassboltQr.SubsequentPage
        assertThat(subsequentPage.reservedBytesDto.version).isEqualTo(1)
        assertThat(subsequentPage.reservedBytesDto.page).isEqualTo(1)
    }

    @Test
    fun `mapper should correctly parse account kit page with correct version`() {
        val scanResult = BarcodeScanResult.SingleBarcode(PASSBOLT_FIRST_PAGE_ACCOUNT_KIT_SCAN)
        val mapped = qrMapper.apply(scanResult)
        assertThat(mapped).isInstanceOf(ParseResult.PassboltQr.AccountKitPage::class.java)
        val accountKitPage = mapped as ParseResult.PassboltQr.AccountKitPage
        assertThat(accountKitPage.reservedBytesDto.version).isEqualTo(2)
        assertThat(accountKitPage.reservedBytesDto.page).isEqualTo(0)
    }
}
