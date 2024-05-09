package com.passbolt.mobile.android.feature.setup.scanqr.qrparser

import com.passbolt.mobile.android.dto.response.qrcode.AccountKitPageDto
import com.passbolt.mobile.android.dto.response.qrcode.QrFirstPageDto
import com.passbolt.mobile.android.dto.response.qrcode.ReservedBytesDto

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

sealed class ParseResult {

    sealed class PassboltQr(open val reservedBytesDto: ReservedBytesDto) : ParseResult() {

        data class FirstPage(
            override val reservedBytesDto: ReservedBytesDto,
            val content: QrFirstPageDto
        ) : PassboltQr(reservedBytesDto)

        data class SubsequentPage(
            override val reservedBytesDto: ReservedBytesDto,
            val content: ByteArray
        ) : PassboltQr(reservedBytesDto) {

            // generated
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as SubsequentPage

                if (reservedBytesDto != other.reservedBytesDto) return false
                if (!content.contentEquals(other.content)) return false

                return true
            }

            // generated
            override fun hashCode(): Int {
                var result = reservedBytesDto.hashCode()
                result = 31 * result + content.contentHashCode()
                return result
            }
        }

        data class AccountKitPage(
            override val reservedBytesDto: ReservedBytesDto,
            val content: AccountKitPageDto
        ) : PassboltQr(reservedBytesDto) {

            // generated
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as AccountKitPage

                if (reservedBytesDto != other.reservedBytesDto) return false
                if (content != other.content) return false

                return true
            }

            // generated
            override fun hashCode(): Int {
                var result = reservedBytesDto.hashCode()
                result = 31 * result + content.hashCode()
                return result
            }
        }
    }

    class Failure(val exception: Throwable? = null) : ParseResult()

    class ScanFailure(val exception: Throwable? = null) : ParseResult()

    class UserResolvableError(val errorType: ErrorType) : ParseResult() {

        enum class ErrorType {
            MULTIPLE_BARCODES,
            NO_BARCODES_IN_RANGE,
            NOT_A_PASSBOLT_QR
        }
    }

    class FinishedWithSuccess(
        val armoredKey: String
    ) : ParseResult()
}
