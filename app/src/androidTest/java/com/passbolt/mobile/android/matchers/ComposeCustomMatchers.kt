/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
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

package com.passbolt.mobile.android.matchers

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage

/**
 * Asserts that the node is currently displayed and renders a bitmap with non-zero dimensions.
 *
 * **Why use this?**
 * Verifies the node with bitmap renders actual pixels, not just that it is semantically "visible."
 * Use this to catch layout bugs where a displayed view with bitmap has accidentally collapsed to 0 width or height.
 *
 * @throws AssertionError if the node is not displayed or if the captured bitmap has 0 width or height.
 */
fun SemanticsNodeInteraction.assertHasBitmapContent() {
    assertIsDisplayed()
    val bitmap = captureToImage()
    if (bitmap.width == 0 || bitmap.height == 0) {
        throw AssertionError("Image has 0 dimensions")
    }
}
