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

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.passbolt.mobile.android.common.extension.isInFuture
import com.passbolt.mobile.android.core.resources.resourceicon.ResourceIconProvider
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.isExpired
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Composable
fun ResourceItem(
    resource: ResourceModel,
    resourceIconProvider: ResourceIconProvider,
    onItemClick: (ResourceModel) -> Unit,
    onMoreClick: (ResourceModel) -> Unit,
    modifier: Modifier = Modifier,
    showMoreMenu: Boolean = true,
) {
    val context = LocalContext.current

    var resourceIcon by remember { mutableStateOf<Drawable?>(null) }
    LaunchedEffect(resource) {
        resourceIcon = resourceIconProvider.getResourceIcon(context, resource)
    }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable { onItemClick(resource) }
                .padding(horizontal = 16.dp)
                .testTag("home_resource_row"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(46.dp, 52.dp)) {
            if (resourceIcon != null) {
                Image(
                    painter = rememberDrawablePainter(resourceIcon),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(40.dp)
                            .align(Alignment.CenterStart),
                )
            }

            if (resource.isExpired()) {
                Image(
                    painter = painterResource(R.drawable.ic_excl_indicator),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(12.dp)
                            .align(Alignment.BottomEnd),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            val resourceName = resource.metadataJsonModel.name
            val titleText =
                resource.expiry?.let { expiry ->
                    if (!expiry.isInFuture()) {
                        stringResource(LocalizationR.string.name_expired, resourceName)
                    } else {
                        resourceName
                    }
                } ?: resourceName

            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(4.dp))
            val username = resource.metadataJsonModel.username
            val isUsernameEmpty = username.isNullOrBlank()

            Text(
                text = if (!isUsernameEmpty) username else stringResource(LocalizationR.string.no_username),
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = if (isUsernameEmpty) FontStyle.Italic else FontStyle.Normal,
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        if (showMoreMenu) {
            IconButton(
                onClick = { onMoreClick(resource) },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = null)
            }
        }
    }
}
