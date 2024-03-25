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
    val koinTestRule = KoinTestRule.create {
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
}
