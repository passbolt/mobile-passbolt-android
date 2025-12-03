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

package com.passbolt.mobile.android.feature.home.screen.list

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.ui.FolderWithCountAndPath
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
fun FolderItem(
    folder: FolderWithCountAndPath,
    onFolderClick: (FolderWithCountAndPath) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable { onFolderClick(folder) }
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(getFolderIconResId(folder)),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = getPathText(context, folder),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = folder.subItemsCount.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(end = 8.dp),
        )

        Icon(
            painter = painterResource(id = CoreUiR.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@DrawableRes
private fun getFolderIconResId(folder: FolderWithCountAndPath): Int =
    if (folder.isShared) {
        CoreUiR.drawable.ic_filled_shared_folder_with_bg
    } else {
        CoreUiR.drawable.ic_filled_folder_with_bg
    }

private fun getPathText(
    context: Context,
    folder: FolderWithCountAndPath,
): String {
    val rootPathPlaceholder = context.getString(LocalizationR.string.folder_root)
    val pathSeparator = context.getString(LocalizationR.string.folder_details_location_separator)

    return folder.path?.let {
        "$rootPathPlaceholder $pathSeparator $it"
    } ?: rootPathPlaceholder
}

@Preview(showBackground = true)
@Composable
private fun FolderItemPreview() {
    val sampleFolder =
        FolderWithCountAndPath(
            folderId = "folder-123",
            name = "Work Documents",
            permission = ResourcePermission.READ,
            parentId = null,
            isShared = false,
            subItemsCount = 7,
            path = null,
            searchCriteria = "Work Documents",
        )

    MaterialTheme {
        FolderItem(
            folder = sampleFolder,
            onFolderClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SharedFolderItemPreview() {
    val sampleSharedFolder =
        FolderWithCountAndPath(
            folderId = "shared-folder-456",
            name = "Shared Team Folder",
            permission = ResourcePermission.READ,
            parentId = "parent-folder-789",
            isShared = true,
            subItemsCount = 12,
            path = "Parent Folder",
            searchCriteria = "Shared Team Folder",
        )

    MaterialTheme {
        FolderItem(
            folder = sampleSharedFolder,
            onFolderClick = {},
        )
    }
}
