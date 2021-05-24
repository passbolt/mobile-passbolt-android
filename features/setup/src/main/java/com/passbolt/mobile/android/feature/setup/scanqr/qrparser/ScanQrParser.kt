package com.passbolt.mobile.android.feature.setup.scanqr.qrparser

import com.passbolt.mobile.android.common.extension.eraseArray
import com.passbolt.mobile.android.core.mvp.CoroutineLaunchContext
import com.passbolt.mobile.android.core.qrscan.analyzer.BarcodeScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okio.Buffer
import timber.log.Timber

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

class ScanQrParser(
    private val coroutineLaunchContext: CoroutineLaunchContext,
    private val qrScanResultsMapper: QrScanResultsMapper,
    private val keyAssembler: KeyAssembler
) {

    val parseResultFlow: SharedFlow<ParseResult>
        get() = _pareResultFlow.asStateFlow()
    private val _pareResultFlow = MutableStateFlow<ParseResult>(
        ParseResult.UserResolvableError(ParseResult.UserResolvableError.ErrorType.NO_BARCODES_IN_RANGE)
    )

    private val alreadyScannedPages = sortedSetOf<Int>()
    private val isFirstPageScanned
        get() = alreadyScannedPages.contains(QrScanResultsMapper.FIRST_PAGE_INDEX)

    private var hash: String? = null
    private val scannedBytes = Buffer()

    suspend fun startParsing(scanFlow: SharedFlow<BarcodeScanResult>) {
        scanFlow
            .map { qrScanResultsMapper.apply(it) }
            .collect {
                if (it is ParseResult.PassboltQr.FirstPage) {
                    processFirstPageData(it)
                    _pareResultFlow.tryEmit(it)
                } else if (it is ParseResult.PassboltQr.SubsequentPage) {
                    if (isFirstPageScanned) {
                        processSubsequentPageData(it)
                        _pareResultFlow.tryEmit(it)
                    } else {
                        Timber.e("First page was not scanned, but subsequent page received")
                        _pareResultFlow.tryEmit(ParseResult.Failure())
                    }
                } else {
                    it?.let {
                        _pareResultFlow.tryEmit(it)
                    }
                }
            }
    }

    private suspend fun processFirstPageData(firstPage: ParseResult.PassboltQr.FirstPage) =
        withContext(coroutineLaunchContext.io) {
            hash = firstPage.content.hash
            alreadyScannedPages.add(QrScanResultsMapper.FIRST_PAGE_INDEX)
        }

    private suspend fun processSubsequentPageData(parseResult: ParseResult.PassboltQr.SubsequentPage) =
        withContext(coroutineLaunchContext.io) {
            if (!alreadyScannedPages.contains(parseResult.reservedBytesDto.page)) {
                scannedBytes.write(parseResult.content)
                alreadyScannedPages.add(parseResult.reservedBytesDto.page)
            }
        }

    suspend fun verifyScannedKey() = withContext(coroutineLaunchContext.io) {
        try {
            if (scannedBytes.sha512().hex() == hash) {
                val assembledKey = keyAssembler.assemblePrivateKey(scannedBytes)
                _pareResultFlow.tryEmit(ParseResult.FinishedWithSuccess(assembledKey))
                assembledKey.eraseArray()
            } else {
                _pareResultFlow.tryEmit(ParseResult.Failure())
            }
            scannedBytes.clear()
        } catch (exception: Exception) {
            Timber.e(exception, "Error during verifying the key")
            _pareResultFlow.tryEmit(ParseResult.Failure())
        }
    }
}
