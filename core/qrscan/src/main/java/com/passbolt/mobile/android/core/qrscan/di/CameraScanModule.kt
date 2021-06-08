package com.passbolt.mobile.android.core.qrscan.di

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

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
val cameraScanModule = module {
    factory { provideImageAnalysis() }
    factory { providePreview() }
    factory { provideCameraProviderFuture(androidApplication()) }
    factory { provideCameraSelector() }
}

// https://firebase.google.com/docs/ml-kit/android/read-barcodes?hl=en#input-image-guidelines
private const val RECOMMENDED_INPUT_IMAGE_WIDTH = 1280
private const val RECOMMENDED_INPUT_IMAGE_HEIGHT = 720

private fun provideImageAnalysis() = ImageAnalysis.Builder()
    .setTargetResolution(Size(RECOMMENDED_INPUT_IMAGE_WIDTH, RECOMMENDED_INPUT_IMAGE_HEIGHT))
    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
    .build()

private fun provideCameraProviderFuture(context: Context) =
    ProcessCameraProvider.getInstance(context)

private fun providePreview() =
    Preview.Builder().build()

private fun provideCameraSelector() =
    CameraSelector.DEFAULT_BACK_CAMERA
