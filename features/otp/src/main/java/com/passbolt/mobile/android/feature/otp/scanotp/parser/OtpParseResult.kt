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

package com.passbolt.mobile.android.feature.otp.scanotp.parser

typealias OtpAlgorithm = OtpParseResult.OtpQr.Algorithm

// Check details in the documentation
// intro: https://github.com/google/google-authenticator/wiki/Key-Uri-Format
// totp https://www.rfc-editor.org/rfc/rfc6238
// hotp https://www.rfc-editor.org/rfc/rfc4226
sealed class OtpParseResult {

    sealed class OtpQr(
        open val label: String,
        open val secret: String,
        open val issuer: String?,
        open val algorithm: Algorithm,
        open val digits: Int
    ) : OtpParseResult() {

        data class TotpQr(
            override val label: String,
            override val secret: String,
            override val issuer: String?,
            override val algorithm: Algorithm,
            override val digits: Int,
            val period: Int
        ) : OtpQr(label, secret, issuer, algorithm, digits) {

            companion object {
                const val DEFAULT_PERIOD_SECONDS = 30
                const val DEFAULT_DIGITS = 6
            }
        }

        data class HotpQr(
            override val label: String,
            override val secret: String,
            override val issuer: String?,
            override val algorithm: Algorithm,
            override val digits: Int,
            val counter: Int
        ) : OtpQr(label, secret, issuer, algorithm, digits)

        enum class Algorithm {
            SHA1,
            SHA256,
            SHA512;

            companion object {
                val DEFAULT = SHA1
            }
        }
    }

    sealed class IncompleteOtpParameters(
        open val label: String?,
        open val secret: String?,
        open val issuer: String?,
        open val algorithm: OtpQr.Algorithm?,
        open val digits: Int?
    ) : OtpParseResult() {

        data class IncompleteTotpParameters(
            override val label: String?,
            override val secret: String?,
            override val issuer: String?,
            override val algorithm: OtpQr.Algorithm?,
            override val digits: Int?,
            val period: Int?
        ) : IncompleteOtpParameters(label, secret, issuer, algorithm, digits)

        data class IncompleteHotpParametrs(
            override val label: String?,
            override val secret: String?,
            override val issuer: String?,
            override val algorithm: OtpQr.Algorithm?,
            override val digits: Int?,
            val counter: Int?
        ) : IncompleteOtpParameters(label, secret, issuer, algorithm, digits)
    }

    class Failure(val exception: Throwable? = null) : OtpParseResult()

    class ScanFailure(val exception: Throwable? = null) : OtpParseResult()

    class UserResolvableError(val errorType: ErrorType) : OtpParseResult() {

        enum class ErrorType {
            MULTIPLE_BARCODES,
            NO_BARCODES_IN_RANGE,
            NOT_A_OTP_QR
        }
    }
}
