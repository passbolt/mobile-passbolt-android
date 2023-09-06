package com.passbolt.mobile.android.feature.otp.scanotp.parser

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.qrscan.analyzer.BarcodeScanResult
import org.junit.Test
import java.io.IOException

class OtpQrScanResultsMapperTest {

    private val mapper = OtpQrScanResultsMapper()

    @Test
    fun `mapperShouldMapACorrectFullTotpUri`() {
        val correctFullUri = "otpauth://totp/ACME%20Co:john.doe@email.com" +
                "?secret=HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ" +
                "&issuer=ACME%20Co" +
                "&algorithm=SHA256" +
                "&digits=6" +
                "&period=60"
        val result = mapper.apply(BarcodeScanResult.SingleBarcode(correctFullUri.toByteArray()))

        assertThat(result).isInstanceOf(OtpParseResult.OtpQr.TotpQr::class.java)
        (result as OtpParseResult.OtpQr.TotpQr).let {
            assertThat(it.label).isEqualTo("ACME Co:john.doe@email.com")
            assertThat(it.secret).isEqualTo("HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ")
            assertThat(it.issuer).isEqualTo("ACME Co")
            assertThat(it.algorithm).isEqualTo(OtpParseResult.OtpQr.Algorithm.SHA256)
            assertThat(it.digits).isEqualTo(6)
            assertThat(it.period).isEqualTo(60)
        }
    }

    @Test
    fun `mapperShouldMapToIncompleteParametersWhenRequiredParametersAreMissing`() {
        val secretMissingUri = "otpauth://totp/ACME%20Co:john.doe@email.com" +
                "?issuer=ACME%20Co" +
                "&algorithm=SHA256" +
                "&digits=6" +
                "&period=60"
        val result = mapper.apply(BarcodeScanResult.SingleBarcode(secretMissingUri.toByteArray()))

        assertThat(result).isInstanceOf(OtpParseResult.IncompleteOtpParameters.IncompleteTotpParameters::class.java)
        (result as OtpParseResult.IncompleteOtpParameters.IncompleteTotpParameters).let {
            assertThat(it.label).isEqualTo("ACME Co:john.doe@email.com")
            assertThat(it.secret).isEqualTo(null)
            assertThat(it.issuer).isEqualTo("ACME Co")
            assertThat(it.algorithm).isEqualTo(OtpParseResult.OtpQr.Algorithm.SHA256)
            assertThat(it.digits).isEqualTo(6)
            assertThat(it.period).isEqualTo(60)
        }
    }

    @Test
    fun `mapperShouldMapToInvalidOtpWhenRequiredParametersCausingParsingErrorAreMissing`() {
        // missing label path
        val labelMissingUri = "otpauth://totp" +
                "&issuer=ACME%20Co" +
                "&algorithm=SHA256" +
                "&digits=6" +
                "&period=60"
        val incorrectSchemeUri = "http://totp/ACME%20Co:john.doe@email.com" +
                "?secret=HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ" +
                "&issuer=ACME%20Co" +
                "&algorithm=SHA256" +
                "&digits=6" +
                "&period=60"
        val incorrectTypeUri = "otpauth://wrong_type/ACME%20Co:john.doe@email.com" +
                "?secret=HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ" +
                "&issuer=ACME%20Co" +
                "&algorithm=SHA256" +
                "&digits=6" +
                "&period=60"
        val results = listOf(
            mapper.apply(BarcodeScanResult.SingleBarcode(labelMissingUri.toByteArray())),
            mapper.apply(BarcodeScanResult.SingleBarcode(incorrectSchemeUri.toByteArray())),
            mapper.apply(BarcodeScanResult.SingleBarcode(incorrectTypeUri.toByteArray()))
        )

        results.forEach { result ->
            assertThat(result).isInstanceOf(OtpParseResult.UserResolvableError::class.java)
            assertThat((result as OtpParseResult.UserResolvableError).errorType)
                .isEqualTo(OtpParseResult.UserResolvableError.ErrorType.NOT_A_OTP_QR)
        }
    }

    @Test
    fun `mapperShouldMapToScanFailureWhenThereIsQrScanException`() {
        val result = mapper.apply(BarcodeScanResult.Failure(IOException()))

        assertThat(result).isInstanceOf(OtpParseResult.ScanFailure::class.java)
    }

    @Test
    fun `mapperShouldMapCorrectToMultipleCodesInRange`() {
        val result = mapper.apply(BarcodeScanResult.MultipleBarcodes)

        assertThat(result).isInstanceOf(OtpParseResult.UserResolvableError::class.java)
        assertThat((result as OtpParseResult.UserResolvableError).errorType)
            .isEqualTo(OtpParseResult.UserResolvableError.ErrorType.MULTIPLE_BARCODES)
    }

    @Test
    fun `mapperShouldMapCorrectToNoCodesInRange`() {
        val result = mapper.apply(BarcodeScanResult.NoBarcodeInRange)

        assertThat(result).isInstanceOf(OtpParseResult.UserResolvableError::class.java)
        assertThat((result as OtpParseResult.UserResolvableError).errorType)
            .isEqualTo(OtpParseResult.UserResolvableError.ErrorType.NO_BARCODES_IN_RANGE)
    }

    @Test
    fun `mapperShouldApplyDefaultValuesIfNotPresentInCode`() {
        val correctFullUri = "otpauth://totp/ACME%20Co:john.doe@email.com" +
                "?secret=HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ" +
                "&issuer=ACME%20Co"
        val result = mapper.apply(BarcodeScanResult.SingleBarcode(correctFullUri.toByteArray()))

        assertThat(result).isInstanceOf(OtpParseResult.OtpQr.TotpQr::class.java)
        (result as OtpParseResult.OtpQr.TotpQr).let {
            assertThat(it.label).isEqualTo("ACME Co:john.doe@email.com")
            assertThat(it.secret).isEqualTo("HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ")
            assertThat(it.issuer).isEqualTo("ACME Co")
            assertThat(it.algorithm).isEqualTo(OtpParseResult.OtpQr.Algorithm.DEFAULT)
            assertThat(it.digits).isEqualTo(OtpParseResult.OtpQr.TotpQr.DEFAULT_DIGITS)
            assertThat(it.period).isEqualTo(OtpParseResult.OtpQr.TotpQr.DEFAULT_PERIOD_SECONDS)
        }
    }
}
