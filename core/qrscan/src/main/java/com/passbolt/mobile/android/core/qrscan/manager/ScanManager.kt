package com.passbolt.mobile.android.core.qrscan.manager

import androidx.camera.core.CameraSelector
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.passbolt.mobile.android.core.qrscan.analyzer.BarcodeScanResult
import com.passbolt.mobile.android.core.qrscan.analyzer.QrCodeImageAnalyzer
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executor

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

class ScanManager(
    private val mainExecutor: Executor,
    private val barcodeImageAnalyzer: QrCodeImageAnalyzer,
    private val cameraController: LifecycleCameraController,
    private val barcodeScanner: BarcodeScanner,
) {
    val barcodeScanPublisher: StateFlow<BarcodeScanResult>
        get() = barcodeImageAnalyzer.resultFlow

    fun attach(
        owner: LifecycleOwner,
        previewView: PreviewView,
    ) {
        with(cameraController) {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setImageAnalysisAnalyzer(mainExecutor, barcodeImageAnalyzer)
            bindToLifecycle(owner)
        }
        previewView.controller = cameraController
    }

    fun detach() {
        barcodeScanner.close()
        cameraController.unbind()
    }
}
