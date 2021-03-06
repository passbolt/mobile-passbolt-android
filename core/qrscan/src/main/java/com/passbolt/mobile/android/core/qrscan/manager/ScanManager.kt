package com.passbolt.mobile.android.core.qrscan.manager

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.passbolt.mobile.android.core.qrscan.analyzer.BarcodeScanResult
import com.passbolt.mobile.android.core.qrscan.analyzer.CameraBarcodeAnalyzer
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executor

class ScanManager constructor(
    private val cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
    private val previewUseCase: Preview,
    private val cameraSelector: CameraSelector,
    private val cameraBarcodeAnalyzer: CameraBarcodeAnalyzer,
    private val imageAnalysisUseCase: ImageAnalysis,
    private val mainExecutor: Executor
) {

    val barcodeScanPublisher: StateFlow<BarcodeScanResult>
        get() = cameraBarcodeAnalyzer.resultFlow

    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    fun attach(owner: LifecycleOwner, cameraPreview: PreviewView) {
        cameraProviderFuture.addListener({
            imageAnalysisUseCase.setAnalyzer(mainExecutor, cameraBarcodeAnalyzer)
            previewUseCase.setSurfaceProvider(cameraPreview.surfaceProvider)
            bindCameraUseCases(owner)
        }, mainExecutor)
    }

    fun detach() {
        previewUseCase.setSurfaceProvider(null)
        cameraProviderFuture.get().unbind(imageAnalysisUseCase, previewUseCase)
    }

    @Throws(IllegalStateException::class, java.lang.IllegalArgumentException::class)
    private fun bindCameraUseCases(owner: LifecycleOwner) {
        with(cameraProviderFuture.get()) {
            unbindAll()
            bindToLifecycle(
                owner,
                cameraSelector,
                previewUseCase, imageAnalysisUseCase
            )
        }
    }
}
