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

package com.passbolt.mobile.android.locationdetails.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.locationdetails.data.ExpandableFolderNode
import com.passbolt.mobile.android.core.ui.R as CoreUiR

private const val FOLDER_ARROW_ANIMATION_DURATION_MS = 200
private const val EXPANDED_ARROW_ROTATION = 0f
private const val COLLAPSED_ARROW_ROTATION = -90f
private const val INDENT_PADDING = 8

@Composable
internal fun ExpandableFolderItem(
    node: ExpandableFolderNode,
    isExpanded: Boolean,
    onToggleExpansion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasChildren = node.children.isNotEmpty()
    val paddingStart = (INDENT_PADDING * node.depth).dp

    val expandIconRotation by animateFloatAsState(
        targetValue = if (isExpanded) EXPANDED_ARROW_ROTATION else COLLAPSED_ARROW_ROTATION,
        animationSpec = tween(durationMillis = FOLDER_ARROW_ANIMATION_DURATION_MS),
    )

    Row(
        modifier =
            modifier
                .clickable(enabled = hasChildren) { onToggleExpansion() }
                .padding(
                    start = paddingStart + 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(CoreUiR.drawable.ic_arrow_down),
            contentDescription = null,
            modifier =
                Modifier
                    .size(16.dp)
                    .rotate(expandIconRotation),
        )

        Image(
            painter =
                painterResource(
                    if (node.folderModel.isShared) {
                        CoreUiR.drawable.ic_filled_shared_folder_with_bg
                    } else {
                        CoreUiR.drawable.ic_filled_folder_with_bg
                    },
                ),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )

        Text(
            text = node.folderModel.name,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
    }
}
