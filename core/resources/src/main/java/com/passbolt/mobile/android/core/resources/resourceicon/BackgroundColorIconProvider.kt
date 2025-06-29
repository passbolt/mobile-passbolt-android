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

package com.passbolt.mobile.android.core.resources.resourceicon

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.view.Gravity
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.passbolt.mobile.android.core.ui.R as CoreUiR

class BackgroundColorIconProvider {
    fun getBackgroundColorIcon(
        context: Context,
        backgroundColorHex: String,
        backgroundCircleSizeDp: Float = 40f,
        selectedIconToBackgroundPercentage: Float = 0.7f,
        isSelected: Boolean = false,
    ): Drawable {
        val backgroundColor = backgroundColorHex.toColorInt()
        val density = context.resources.displayMetrics.density

        val backgroundShape =
            ShapeDrawable(OvalShape()).apply {
                intrinsicWidth = (backgroundCircleSizeDp * density).toInt()
                intrinsicHeight = (backgroundCircleSizeDp * density).toInt()
                paint.color = backgroundColor
            }

        if (isSelected) {
            val selectedTickIcon =
                ContextCompat
                    .getDrawable(
                        context,
                        CoreUiR.drawable.selected_icon,
                    )?.mutate()

            val iconIntrinsicWidth = selectedTickIcon?.intrinsicWidth ?: 1
            val iconIntrinsicHeight = selectedTickIcon?.intrinsicHeight ?: 1

            val targetIconWidthPixels = (backgroundCircleSizeDp * selectedIconToBackgroundPercentage * density).toInt()

            val targetIconHeightPixels = (targetIconWidthPixels * iconIntrinsicHeight.toFloat() / iconIntrinsicWidth).toInt()

            selectedTickIcon?.setBounds(
                0,
                0,
                targetIconWidthPixels,
                targetIconHeightPixels,
            )

            return LayerDrawable(arrayOf(backgroundShape, selectedTickIcon)).apply {
                setLayerGravity(1, Gravity.CENTER)
                setLayerSize(1, targetIconWidthPixels, targetIconHeightPixels)
            }
        } else {
            return backgroundShape
        }
    }
}
