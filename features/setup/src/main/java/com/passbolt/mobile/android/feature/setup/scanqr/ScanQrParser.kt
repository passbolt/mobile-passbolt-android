package com.passbolt.mobile.android.feature.setup.scanqr

import androidx.collection.ArrayMap
import com.google.gson.Gson
import com.passbolt.mobile.android.common.extension.eraseArray
import com.passbolt.mobile.android.common.extension.findPosition
import com.passbolt.mobile.android.common.extension.toCharArray
import com.passbolt.mobile.android.core.mvp.CoroutineLaunchContext
import com.passbolt.mobile.android.core.qrscan.analyzer.CameraBarcodeAnalyzer
import com.passbolt.mobile.android.dto.response.qrcode.QrFirstPageDto
import com.passbolt.mobile.android.dto.response.qrcode.ReservedBytesDto
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import okio.Buffer
import timber.log.Timber
import java.nio.ByteBuffer

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
    private val coroutineContext: CoroutineLaunchContext,
    private val gson: Gson
) {

    val parseResultsChannel = Channel<ParseResult>()

    private var totalPages: Int? = null
    private var hash: String? = null
    private var subsequentPages: ArrayMap<Int, ParseResult.SubsequentPage>? = null
    private val contentBytes = Buffer()

    suspend fun process(data: ByteArray?) {
        try {
            requireNotNull(data)
            withContext(coroutineContext.io) {
                val reservedBytes = ByteArray(RESERVED_BYTES_COUNT) { data[it] }
                val reservedBytesDto = processReservedBytes(reservedBytes)
                val payloadBytes = ByteArray(data.size - RESERVED_BYTES_COUNT) { data[it + RESERVED_BYTES_COUNT] }
                if (reservedBytesDto.page == FIRST_PAGE_INDEX) {
                    parseFirstPage(reservedBytesDto, payloadBytes)
                } else {
                    parseSubsequentPage(reservedBytesDto, payloadBytes)
                }
            }
        } catch (exception: Exception) {
            Timber.e(exception)
            parseResultsChannel.offer(ParseResult.Error)
        }
    }

    private suspend fun parseFirstPage(reservedBytesDto: ReservedBytesDto, payloadBytes: ByteArray) {
        val parsedFirstPage = ParseResult.FirstPage(
            reservedBytesDto,
            gson.fromJson(String(payloadBytes), QrFirstPageDto::class.java)
        )
        totalPages = parsedFirstPage.content.totalPages
        hash = parsedFirstPage.content.hash
        subsequentPages = ArrayMap(parsedFirstPage.content.totalPages - 1)
        parseResultsChannel.send(parsedFirstPage)
    }

    private suspend fun parseSubsequentPage(
        reservedBytesDto: ReservedBytesDto,
        payloadBytes: ByteArray
    ) {
        if (validateFirstPageParsed() && subsequentPages?.contains(reservedBytesDto.page) == false) {
            contentBytes.write(payloadBytes)
            val parsedSubsequentPage = ParseResult.SubsequentPage(reservedBytesDto)
            subsequentPages?.put(reservedBytesDto.page, parsedSubsequentPage)
            parseResultsChannel.send(parsedSubsequentPage)
        }
    }

    private fun validateFirstPageParsed(): Boolean {
        totalPages ?: error("First page was not scanned")
        return totalPages != null
    }

    private fun processReservedBytes(bytes: ByteArray): ReservedBytesDto {
        // the first byte contains transfer protocol version
        val version = bytes[0].toInt()
        // the second and third bytes contain page number
        val pageNumber = ByteBuffer.wrap(ByteArray(PAGE_NUMBER_BYTES_COUNT) { bytes[it + 1] }).short.toInt()
        return ReservedBytesDto(version, pageNumber)
    }

    fun verifyKey() {
        if (contentBytes.sha512().hex() == hash) {
            val assembledKey = assemblePrivateKey()
            parseResultsChannel.offer(ParseResult.Success(assembledKey))
            assembledKey.eraseArray()
        } else {
            Timber.e("Incorrect key hash.")
            parseResultsChannel.offer(ParseResult.Error)
        }
        contentBytes.clear()
    }

    private fun assemblePrivateKey(): CharArray {
        val charArray = contentBytes.readByteArray().toCharArray()
        val keyStartPosition = charArray.findPosition(ARMORED_KEY_TEXT.toCharArray()) + ARMORED_KEY_TEXT.length
        val keyEndPosition = charArray.lastIndexOf(ARMORED_KEY_END_CHAR) - 1

        val privateKey = charArray.slice(IntRange(keyStartPosition, keyEndPosition)).toCharArray()
        charArray.eraseArray()

        return privateKey
    }

    fun isPassboltQr(barcodeScanResult: CameraBarcodeAnalyzer.BarcodeScanResult.SingleBarcode): Boolean {
        barcodeScanResult.data?.let {
            val reservedBytes = processReservedBytes(it)
            return reservedBytes.version == PROTOCOL_VERSION && reservedBytes.page in (0..(totalPages ?: 0))
        } ?: return false
    }

    sealed class ParseResult {
        data class FirstPage(
            val reservedBytesDto: ReservedBytesDto,
            val content: QrFirstPageDto
        ) : ParseResult()

        data class SubsequentPage(
            val reservedBytesDto: ReservedBytesDto
        ) : ParseResult()

        object Error : ParseResult()

        class Success(
            val armoredKey: CharArray
        ) : ParseResult()
    }

    private companion object {
        private const val RESERVED_BYTES_COUNT = 3
        private const val PAGE_NUMBER_BYTES_COUNT = 2
        private const val FIRST_PAGE_INDEX = 0
        private const val PROTOCOL_VERSION = 1
        private const val ARMORED_KEY_TEXT = "\"armored_key\":\""
        private const val ARMORED_KEY_END_CHAR = '}'
    }
}
