package com.passbolt.mobile.android.core.ui.span

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.style.ReplacementSpan
import androidx.annotation.ColorInt
import com.passbolt.mobile.android.core.extension.px
import kotlin.math.roundToInt

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

class RoundedBackgroundSpan(
    @ColorInt private val backgroundColor: Int,
    @ColorInt private val textColor: Int
) : ReplacementSpan() {

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        drawBackgroundRoundedRect(x, top, paint, text, start, end, bottom, canvas)
        drawText(paint, canvas, text, start, end, x, y)
    }

    private fun drawText(
        paint: Paint,
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        y: Int
    ) {
        paint.color = textColor
        canvas.drawText(
            text,
            start,
            end,
            x + ADDITIONAL_IN_BOX_PADDING + ADDITIONAL_OUT_BOX_PADDING,
            y.toFloat(),
            paint
        )
    }

    @Suppress("LongParameterList")
    private fun drawBackgroundRoundedRect(
        x: Float,
        top: Int,
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        bottom: Int,
        canvas: Canvas
    ) {
        val bgRect = RectF(
            x + ADDITIONAL_OUT_BOX_PADDING,
            top.toFloat(),
            x + measureText(paint, text, start, end) +
                    2 * ADDITIONAL_IN_BOX_PADDING + ADDITIONAL_OUT_BOX_PADDING,
            bottom.toFloat()
        )
        paint.color = backgroundColor
        canvas.drawRoundRect(bgRect, CORNER_RADIUS.toFloat(), CORNER_RADIUS.toFloat(), paint)
    }

    override fun getSize(paint: Paint, text: CharSequence?, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        val width = paint.measureText(text, start, end).roundToInt() +
                2 * ADDITIONAL_IN_BOX_PADDING + 2 * ADDITIONAL_OUT_BOX_PADDING
        val metrics = paint.fontMetricsInt
        if (fm != null) {
            fm.top = metrics.top
            fm.ascent = metrics.ascent
            fm.descent = metrics.descent
            fm.bottom = metrics.bottom
        }
        return width
    }

    private fun measureText(paint: Paint, text: CharSequence, start: Int, end: Int) =
        paint.measureText(text, start, end)

    companion object {
        private val CORNER_RADIUS = 2.px
        private val ADDITIONAL_IN_BOX_PADDING = 4.px
        private val ADDITIONAL_OUT_BOX_PADDING = 2.px
    }
}
