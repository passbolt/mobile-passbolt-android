package com.passbolt.mobile.android.feature.home.screen.list

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.ui.TagWithCount
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
fun TagItem(
    tag: TagWithCount,
    onClick: (TagWithCount) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable { onClick(tag) }
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(getTagIconResId(tag)),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )

        Text(
            text = tag.slug,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = tag.taggedResourcesCount.toString(),
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        Image(
            painter = painterResource(id = CoreUiR.drawable.ic_chevron_right),
            contentDescription = null,
        )
    }
}

@DrawableRes
private fun getTagIconResId(tag: TagWithCount): Int =
    if (tag.isShared) {
        CoreUiR.drawable.ic_filled_shared_tag_with_bg
    } else {
        CoreUiR.drawable.ic_filled_tag_with_bg
    }

@Preview(showBackground = true)
@Composable
private fun TagWithCountItemPreview() {
    TagItem(
        tag =
            TagWithCount(
                id = "1",
                slug = "Personal",
                isShared = false,
                taggedResourcesCount = 5,
                searchCriteria = "Personal",
            ),
        onClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun SharedTagWithCountItemPreview() {
    TagItem(
        tag =
            TagWithCount(
                id = "2",
                slug = "Shared Tag with a very long name that should be truncated",
                isShared = true,
                taggedResourcesCount = 25,
                searchCriteria = "Shared Tag",
            ),
        onClick = {},
    )
}
