package com.passbolt.mobile.android.feature.setup.scanqr

import android.util.SparseArray
import com.google.gson.Gson
import com.passbolt.mobile.android.core.mvp.CoroutineLaunchContext
import com.passbolt.mobile.android.dto.response.qrcode.AggregatedQrDto
import com.passbolt.mobile.android.dto.response.qrcode.QrFirstPageDto
import com.passbolt.mobile.android.dto.response.qrcode.ReservedBytesDto
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

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
class ScanQrParser @Inject constructor(
    private val coroutineContext: CoroutineLaunchContext,
    private val gson: Gson
) {

    val parseResultsChannel = Channel<ParseResult>()

    private var totalPages: Int? = null
    private var subsequentPages: SparseArray<ParseResult.SubsequentPage>? = null

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
        subsequentPages = SparseArray<ParseResult.SubsequentPage>(parsedFirstPage.content.totalPages - 1)
        parseResultsChannel.send(parsedFirstPage)
    }

    private suspend fun parseSubsequentPage(
        reservedBytesDto: ReservedBytesDto,
        payloadBytes: ByteArray
    ) {
        val parsedSubsequentPage = ParseResult.SubsequentPage(
            reservedBytesDto,
            String(payloadBytes)
        )
        subsequentPages?.put(reservedBytesDto.page, parsedSubsequentPage)
        parseResultsChannel.send(parsedSubsequentPage)
    }

    private fun processReservedBytes(bytes: ByteArray): ReservedBytesDto {
        // the first byte contains transfer protocol version
        val version = bytes[0].toInt()
        // the second and third bytes contain page number
        // TODO confirm bytes assemble logic
        val pageNumber = (bytes[1].toString() + bytes[2].toString()).toInt()
        return ReservedBytesDto(version, pageNumber)
    }

    fun assembleKey() {
        val keyBuilder = StringBuilder()
        totalPages?.let { totalPages ->
            (0 until totalPages - 1)
                .mapNotNull { subsequentPages?.valueAt(it)?.content }
                .forEach { keyBuilder.append(it) }
            val key = gson.fromJson(keyBuilder.toString(), AggregatedQrDto::class.java)
            // TODO verify & save key
        }
    }

    sealed class ParseResult {
        data class FirstPage(
            val reservedBytesDto: ReservedBytesDto,
            val content: QrFirstPageDto
        ) : ParseResult()

        data class SubsequentPage(
            val reservedBytesDto: ReservedBytesDto,
            val content: String
        ) : ParseResult()

        object Error : ParseResult()
    }

    private companion object {
        private const val RESERVED_BYTES_COUNT = 3
        private const val FIRST_PAGE_INDEX = 0
    }
}
