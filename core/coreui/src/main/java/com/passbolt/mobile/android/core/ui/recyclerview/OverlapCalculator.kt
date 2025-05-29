package com.passbolt.mobile.android.core.ui.recyclerview

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
class OverlapCalculator(
    private val availableWidth: Int,
    private val itemWidth: Float,
    private val itemCount: Int,
    private val minOverlap: Float = (itemWidth / 2),
) {
    fun calculateLeftOverlapOffset(): ItemOverlapResult {
        if (itemCount * itemWidth < availableWidth) {
            return ItemOverlapResult(
                allItemsFit = true,
                visibleItems = itemCount,
                overlap = 0,
            )
        } else {
            var overlap = 0f
            while (!itemsFit(overlap) && overlap < minOverlap) {
                overlap += OVERLAP_INCREASE_STEP
            }
            return ItemOverlapResult(
                allItemsFit = itemsFit(overlap),
                visibleItems =
                    if (itemsFit(overlap)) {
                        itemCount
                    } else {
                        (availableWidth / (itemWidth - overlap)).toInt()
                    },
                overlap = -overlap.roundToInt(),
            )
        }
    }

    /**
     * Checks if items with applied overlap fit inside the available container width
     * The first item is not overlapping
     *
     * @param overlap current overlap
     * @return true if items fit, false otherwise
     */
    private fun itemsFit(overlap: Float) = (itemCount * itemWidth) - ((itemCount - 1) * overlap) < availableWidth

    data class ItemOverlapResult(
        val allItemsFit: Boolean,
        val visibleItems: Int,
        val overlap: Int,
    )

    private companion object {
        private const val OVERLAP_INCREASE_STEP = 1
    }
}
