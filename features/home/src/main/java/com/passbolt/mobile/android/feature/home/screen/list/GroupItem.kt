package com.passbolt.mobile.android.feature.home.screen.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.ui.GroupWithCount
import com.passbolt.mobile.android.core.ui.R as CoreUiR

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

@Composable
fun GroupItem(
    group: GroupWithCount,
    onClick: (GroupWithCount) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable { onClick(group) }
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = CoreUiR.drawable.ic_filled_group_with_bg),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = group.groupName,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = colorResource(id = CoreUiR.color.text_primary),
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = group.groupItemsCount.toString(),
            modifier = Modifier.padding(end = 8.dp),
            color = colorResource(id = CoreUiR.color.text_primary),
        )

        Icon(
            painter = painterResource(id = CoreUiR.drawable.ic_chevron_right),
            contentDescription = null,
            tint = colorResource(id = CoreUiR.color.icon_tint),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GroupItemPreview() {
    val sampleGroup =
        GroupWithCount(
            groupId = "group-123",
            groupName = "Development Team",
            groupItemsCount = 5,
        )

    MaterialTheme {
        GroupItem(
            group = sampleGroup,
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LongNameGroupItemPreview() {
    val sampleGroup =
        GroupWithCount(
            groupId = "group-456",
            groupName = "Marketing and Communications Team with Very Long Name",
            groupItemsCount = 1245,
        )

    MaterialTheme {
        GroupItem(
            group = sampleGroup,
            onClick = {},
        )
    }
}
