package com.passbolt.mobile.android.core.coil.transformation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import coil3.size.Size
import coil3.transform.Transformation

class AlphaTransformation(
    private val shouldLowerOpacity: Boolean,
) : Transformation() {
    override val cacheKey: String
        get() = javaClass.name + "$transformAlpha"

    private val transformAlpha: Int
        get() = if (shouldLowerOpacity) LOWER_OPACITY_ALPHA else FULL_ALPHA

    private val alphaPaint = Paint().apply { alpha = transformAlpha }

    override suspend fun transform(
        input: Bitmap,
        size: Size,
    ): Bitmap {
        val newBitmap = createBitmap(input.width, input.height)
        Canvas(newBitmap).drawBitmap(input, 0f, 0f, alphaPaint)
        return newBitmap
    }

    private companion object {
        private const val LOWER_OPACITY_ALPHA = (255 / 2f).toInt()
        private const val FULL_ALPHA = 255
    }
}
