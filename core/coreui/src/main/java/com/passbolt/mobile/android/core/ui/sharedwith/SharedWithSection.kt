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

package com.passbolt.mobile.android.core.ui.sharedwith

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.circularimage.CircularProfileImage
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
fun SharedWithSection(
    permissions: List<PermissionModelUi>,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 40.dp,
) {
    val avatarSizePx = with(LocalDensity.current) { avatarSize.toPx() }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .height(avatarSize),
    ) {
        val containerWidth = constraints.maxWidth
        val permissionsDataset =
            remember(permissions, containerWidth) {
                PermissionsDatasetCreator(
                    permissionsListWidth = containerWidth,
                    permissionItemWidth = avatarSizePx,
                ).prepareDataset(permissions)
            }
        val usersCount = permissionsDataset.userPermissions.size
        val groupCount = permissionsDataset.groupPermissions.size

        // groups
        permissionsDataset.groupPermissions.forEachIndexed { index, _ ->
            Image(
                painter = painterResource(CoreUiR.drawable.ic_group_avatar),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(avatarSize)
                        .offset { IntOffset(xFor(index, avatarSizePx, permissionsDataset.overlap), 0) },
            )
        }

        // users
        permissionsDataset.userPermissions.forEachIndexed { index, userPermission ->
            CircularProfileImage(
                userPermission.user.avatarUrl,
                width = avatarSize,
                height = avatarSize,
                modifier =
                    Modifier
                        .offset { IntOffset(xFor(groupCount + index, avatarSizePx, permissionsDataset.overlap), 0) },
            )
        }

        // counter
        permissionsDataset.counterValue.firstOrNull()?.let { counter ->
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(avatarSize)
                        .offset { IntOffset(xFor(usersCount + groupCount - 1, avatarSizePx, permissionsDataset.overlap), 0) }
                        .background(
                            color = Color.White,
                            shape = CircleShape,
                        ).border(1.dp, colorResource(CoreUiR.color.divider), CircleShape),
            ) {
                Text(
                    text = counter,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

// overlap is negative
fun xFor(
    position: Int,
    itemWidthPx: Float,
    overlap: Int,
): Int = (position * (itemWidthPx + overlap)).toInt()
