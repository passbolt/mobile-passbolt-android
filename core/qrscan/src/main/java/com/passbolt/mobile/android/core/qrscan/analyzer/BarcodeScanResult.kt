package com.passbolt.mobile.android.core.qrscan.analyzer

sealed class BarcodeScanResult {

    data class SingleBarcode(val data: ByteArray?) : BarcodeScanResult() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SingleBarcode

            if (data != null) {
                if (other.data == null) return false
                if (!data.contentEquals(other.data)) return false
            } else if (other.data != null) return false

            return true
        }

        override fun hashCode(): Int {
            return data?.contentHashCode() ?: 0
        }
    }

    data object MultipleBarcodes : BarcodeScanResult()

    data object NoBarcodeInRange : BarcodeScanResult()

    class Failure(val throwable: Throwable) : BarcodeScanResult()
}
