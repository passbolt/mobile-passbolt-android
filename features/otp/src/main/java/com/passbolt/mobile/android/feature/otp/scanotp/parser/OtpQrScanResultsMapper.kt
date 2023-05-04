package com.passbolt.mobile.android.feature.otp.scanotp.parser

import android.net.Uri
import com.passbolt.mobile.android.common.validation.UriIsOfAuthority
import com.passbolt.mobile.android.common.validation.UriIsOfScheme
import com.passbolt.mobile.android.common.validation.validation
import com.passbolt.mobile.android.core.qrscan.analyzer.BarcodeScanResult
import com.passbolt.mobile.android.feature.otp.scanotp.parser.OtpParseResult.UserResolvableError.ErrorType.MULTIPLE_BARCODES
import com.passbolt.mobile.android.feature.otp.scanotp.parser.OtpParseResult.UserResolvableError.ErrorType.NOT_A_OTP_QR
import com.passbolt.mobile.android.feature.otp.scanotp.parser.OtpParseResult.UserResolvableError.ErrorType.NO_BARCODES_IN_RANGE
import timber.log.Timber
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

class OtpQrScanResultsMapper {

    fun apply(scanResult: BarcodeScanResult) = when (scanResult) {
        is BarcodeScanResult.Failure ->
            OtpParseResult.ScanFailure(scanResult.throwable)
        is BarcodeScanResult.MultipleBarcodes ->
            OtpParseResult.UserResolvableError(MULTIPLE_BARCODES)
        is BarcodeScanResult.NoBarcodeInRange ->
            OtpParseResult.UserResolvableError(NO_BARCODES_IN_RANGE)
        is BarcodeScanResult.SingleBarcode ->
            if (isOtpQr(scanResult.data)) {
                mapOtpQr(URI(String(scanResult.data!!))) // validated in isOtpQr
            } else {
                OtpParseResult.UserResolvableError(NOT_A_OTP_QR)
            }
    }

    // hotp not supported yet
    private fun mapOtpQr(totpUri: URI): OtpParseResult {
        return try {
            val otpUri = Uri.parse(totpUri.toString())
            val label = otpUri.lastPathSegment
            val secret = otpUri.getQueryParameter(OTP_URI_PARAMETER_SECRET)
            val issuer = otpUri.getQueryParameter(OTP_URI_PARAMETER_ISSUER)
            val algorithm = otpUri.getQueryParameter(OTP_URI_PARAMETER_ALGORITHM)?.let {
                OtpAlgorithm.valueOf(it)
            } ?: OtpAlgorithm.DEFAULT
            val digits = otpUri.getQueryParameter(OTP_URI_PARAMETER_DIGITS)?.toInt()
                ?: OtpParseResult.OtpQr.TotpQr.DEFAULT_DIGITS
            val period = otpUri.getQueryParameter(OTP_URI_PARAMETER_PERIOD)?.toInt()
                ?: OtpParseResult.OtpQr.TotpQr.DEFAULT_PERIOD_SECONDS

            if (label == null || secret == null) {
                OtpParseResult.IncompleteOtpParameters.IncompleteTotpParameters(
                    label = label,
                    secret = secret,
                    issuer = issuer,
                    algorithm = algorithm,
                    digits = digits,
                    period = period
                )
            } else {
                OtpParseResult.OtpQr.TotpQr(
                    label = label,
                    secret = secret,
                    issuer = issuer,
                    algorithm = algorithm,
                    digits = digits,
                    period = period
                )
            }
        } catch (exception: Exception) {
            // Note: don't log the exception here - the stacktrace might contain a secret from URI
            Timber.e("Error during parsing totp parameters")
            OtpParseResult.Failure(IOException("Error during parsing totp parameters"))
        }
    }

    private fun isOtpQr(qrData: ByteArray?): Boolean {
        if (qrData == null) return false

        return try {
            val otpUri = URI(String(qrData))
            var otpUriValid = true
            validation {
                of(otpUri) {
                    withRules(UriIsOfScheme(OTP_SCHEME)) {
                        onInvalid { Timber.e("OTP URI has incorrect scheme: ${otpUri.scheme}") }
                    }
                    withRules(UriIsOfAuthority(TOTP_AUTHORITY)) {
                        onInvalid { Timber.e("OTP URI has unsupported authority: ${otpUri.authority}") }
                    }
                }
                onInvalid { otpUriValid = false }
            }
            otpUriValid
        } catch (exception: URISyntaxException) {
            // Note: don't log the exception here - the stacktrace might contain a secret from URI
            Timber.e("The URI syntax is incorrect")
            false
        }
    }

    private companion object {
        private const val OTP_SCHEME = "otpauth"
        private const val TOTP_AUTHORITY = "totp"
        private const val OTP_URI_PARAMETER_SECRET = "secret"
        private const val OTP_URI_PARAMETER_ISSUER = "issuer"
        private const val OTP_URI_PARAMETER_ALGORITHM = "algorithm"
        private const val OTP_URI_PARAMETER_DIGITS = "digits"
        private const val OTP_URI_PARAMETER_PERIOD = "period"
    }
}
