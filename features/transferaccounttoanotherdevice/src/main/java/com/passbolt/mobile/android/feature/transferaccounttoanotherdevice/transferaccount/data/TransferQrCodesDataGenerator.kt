package com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.data

import com.passbolt.mobile.android.dto.response.qrcode.QrFirstPageDto
import com.passbolt.mobile.android.dto.response.qrcode.ReservedBytesDto
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.data.QrGenerationConstants.MAX_QR_DATA_BYTES_EXCLUDING_RESERVED_BYTES
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.data.QrGenerationConstants.PROTOCOL_VERSION
import com.passbolt.mobile.android.storage.usecase.accountdata.GetSelectedAccountDataUseCase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */
class TransferQrCodesDataGenerator(
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase
) {

    suspend fun generateQrCodesDataPages(input: Input): Output {
        return try {
            val pages = mutableListOf<String>()
            appendFirstPage(input, pages)
            appendSubsequentPages(input, pages)
            Output.QrPages(pages)
        } catch (exception: Exception) {
            Timber.e("Error during qr data pages generation", exception)
            return Output.Error
        }
    }

    private fun appendFirstPage(input: Input, pages: MutableList<String>) {
        val accountData = getSelectedAccountDataUseCase.execute(Unit)
        val userServerId = requireNotNull(accountData.serverId)
        val domain = requireNotNull(accountData.url)

        val firstPageReservedBytes = ReservedBytesDto(PROTOCOL_VERSION, page = 0)
        val firstPage = QrFirstPageDto(
            input.transferId,
            userServerId,
            input.pagesCount,
            input.authenticationToken,
            input.hash,
            domain
        )
        pages.add(firstPageReservedBytes.encodeToString() + Json.encodeToString(firstPage))
    }

    private fun appendSubsequentPages(input: Input, pages: MutableList<String>) {
        val chunkedKeyJson = input.keyJson.chunked(MAX_QR_DATA_BYTES_EXCLUDING_RESERVED_BYTES)

        // subtract 1 for the first page
        (0 until input.pagesCount - 1)
            .map {
                // add 1 since first page already added
                val pageReservedBytes = ReservedBytesDto(PROTOCOL_VERSION, page = it + 1)
                pages.add(pageReservedBytes.encodeToString() + chunkedKeyJson[it])
            }
    }

    data class Input(
        val transferId: String,
        val authenticationToken: String,
        val pagesCount: Int,
        val hash: String,
        val keyJson: String
    )

    sealed class Output {

        data class QrPages(
            val pages: List<String>
        ) : Output()

        object Error : Output()
    }
}
