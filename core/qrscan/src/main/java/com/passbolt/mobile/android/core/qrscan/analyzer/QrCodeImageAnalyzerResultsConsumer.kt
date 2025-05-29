package com.passbolt.mobile.android.core.qrscan.analyzer

import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.core.util.Consumer
import com.google.mlkit.vision.barcode.BarcodeScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

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

class QrCodeImageAnalyzerResultsConsumer(
    private val barcodeScanner: BarcodeScanner,
) : Consumer<MlKitAnalyzer.Result> {
    val resultFlow: StateFlow<BarcodeScanResult>
        get() = _resultFlow.asStateFlow()

    private val _resultFlow = MutableStateFlow<BarcodeScanResult>(BarcodeScanResult.NoBarcodeInRange)

    override fun accept(result: MlKitAnalyzer.Result?) {
        if (result == null) return

        result.getThrowable(barcodeScanner)?.let { throwable ->
            Timber.e("Error during qr code scan", throwable)
            _resultFlow.tryEmit(BarcodeScanResult.Failure(throwable))
        }

        result.getValue(barcodeScanner)?.let { value ->
            _resultFlow.tryEmit(
                when {
                    value.isEmpty() -> BarcodeScanResult.NoBarcodeInRange
                    value.size == 1 -> BarcodeScanResult.SingleBarcode(value.first().rawBytes)
                    else -> BarcodeScanResult.MultipleBarcodes
                },
            )
        }
    }
}
