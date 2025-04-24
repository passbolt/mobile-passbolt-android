package com.passbolt.mobile.android.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

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

        @Parcelize
        data class TotpQr(
            override val label: String,
            override val secret: String,
            override val issuer: String?,
            override val algorithm: Algorithm,
            override val digits: Int,
            val period: Long
        ) : OtpQr(label, secret, issuer, algorithm, digits), Parcelable {

            companion object {
                const val DEFAULT_PERIOD_SECONDS = 30L
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

        companion object {
            val digitsRange = 6..8 step 1
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
            val period: Long?
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
