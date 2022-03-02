package com.passbolt.mobile.android.feature.setup.scanqr.qrparser

import androidx.annotation.VisibleForTesting
import com.passbolt.mobile.android.core.qrscan.analyzer.BarcodeScanResult
import com.passbolt.mobile.android.dto.response.qrcode.ReservedBytesDto
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult.UserResolvableError.ErrorType.MULTIPLE_BARCODES
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult.UserResolvableError.ErrorType.NOT_A_PASSBOLT_QR
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult.UserResolvableError.ErrorType.NO_BARCODES_IN_RANGE
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import timber.log.Timber

class QrScanResultsMapper {

    fun apply(scanResult: BarcodeScanResult) = when (scanResult) {
        is BarcodeScanResult.Failure ->
            ParseResult.ScanFailure(scanResult.exception)
        is BarcodeScanResult.MultipleBarcodes ->
            ParseResult.UserResolvableError(MULTIPLE_BARCODES)
        is BarcodeScanResult.NoBarcodeInRange ->
            ParseResult.UserResolvableError(NO_BARCODES_IN_RANGE)
        is BarcodeScanResult.SingleBarcode ->
            if (isPassboltQr(scanResult.data)) {
                mapPassboltQr(scanResult.data)
            } else {
                ParseResult.UserResolvableError(NOT_A_PASSBOLT_QR)
            }
    }

    private fun mapPassboltQr(scanResult: ByteArray?): ParseResult {
        return try {
            val data = requireNotNull(scanResult) // already checked for null during isPassboltQr
            val reservedBytesDto = createReservedBytesDto(data)
            val payloadBytes = ByteArray(data.size - RESERVED_BYTES_COUNT) { data[it + RESERVED_BYTES_COUNT] }

            if (reservedBytesDto.page == FIRST_PAGE_INDEX) {
                ParseResult.PassboltQr.FirstPage(
                    reservedBytesDto, Json.decodeFromString(String(payloadBytes))
                )
            } else {
                ParseResult.PassboltQr.SubsequentPage(reservedBytesDto, payloadBytes)
            }
        } catch (exception: Exception) {
            Timber.e(exception)
            ParseResult.Failure(exception)
        }
    }

    private fun createReservedBytesDto(bytes: ByteArray): ReservedBytesDto {
        // the first byte contains transfer protocol version
        val version = String(ByteArray(PROTOCOL_VERSION_BYTES_COUNT) { bytes[it] }).toInt(RESERVED_BYTES_NUMBER_RADIX)
        // the second and third bytes contain page number
        val pageNumber = String(ByteArray(PAGE_NUMBER_BYTES_COUNT) { bytes[it + 1] }).toInt(RESERVED_BYTES_NUMBER_RADIX)
        return ReservedBytesDto(version, pageNumber)
    }

    private fun isPassboltQr(qrData: ByteArray?) = if (qrData == null) {
        false
    } else {
        try {
            val reservedBytes = createReservedBytesDto(qrData)
            reservedBytes.version == PROTOCOL_VERSION && reservedBytes.page in (0..Short.MAX_VALUE)
        } catch (exception: Exception) {
            Timber.e(exception, "Could not process reserved bytes from QR code")
            false
        }
    }

    companion object {
        @VisibleForTesting
        const val PROTOCOL_VERSION = 1

        @VisibleForTesting
        const val PAGE_NUMBER_BYTES_COUNT = 2

        @VisibleForTesting
        const val PROTOCOL_VERSION_BYTES_COUNT = 1

        const val FIRST_PAGE_INDEX = 0

        private const val RESERVED_BYTES_NUMBER_RADIX = 16
        private const val RESERVED_BYTES_COUNT = 3
    }
}
