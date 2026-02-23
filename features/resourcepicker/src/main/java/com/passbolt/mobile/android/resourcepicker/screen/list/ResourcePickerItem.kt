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

package com.passbolt.mobile.android.resourcepicker.screen.list

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.passbolt.mobile.android.core.resources.resourceicon.ResourceIconProvider
import com.passbolt.mobile.android.ui.ResourcePickerListItem
import com.passbolt.mobile.android.ui.isExpired
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
fun ResourcePickerItem(
    resource: ResourcePickerListItem,
    resourceIconProvider: ResourceIconProvider,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier,
    nonSelectableAlpha: Float = 0.5f,
) {
    val context = LocalContext.current

    var resourceIcon by remember { mutableStateOf<Drawable?>(null) }
    LaunchedEffect(resource) {
        resourceIcon = resourceIconProvider.getResourceIcon(context, resource.resourceModel)
    }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable(onClick = onItemClick)
                .padding(horizontal = 16.dp)
                .alpha(if (resource.isSelectable) 1f else nonSelectableAlpha),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(46.dp, 52.dp),
        ) {
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

            if (resource.resourceModel.isExpired()) {
                Image(
                    painter = painterResource(CoreUiR.drawable.ic_excl_indicator),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(12.dp)
                            .align(Alignment.BottomEnd),
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
        ) {
            Text(
                text =
                    if (resource.resourceModel.isExpired()) {
                        stringResource(
                            LocalizationR.string.name_expired,
                            resource.resourceModel.metadataJsonModel.name,
                        )
                    } else {
                        resource.resourceModel.metadataJsonModel.name
                    },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text =
                    if (resource.resourceModel.metadataJsonModel.username
                            .isNullOrBlank()
                    ) {
                        stringResource(LocalizationR.string.no_username)
                    } else {
                        resource.resourceModel.metadataJsonModel.username!!
                    },
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                fontStyle =
                    if (resource.resourceModel.metadataJsonModel.username
                            .isNullOrBlank()
                    ) {
                        FontStyle.Italic
                    } else {
                        FontStyle.Normal
                    },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (!resource.isSelectable) {
            Image(
                painter = painterResource(CoreUiR.drawable.ic_lock),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(colorResource(CoreUiR.color.icon_tint)),
            )
        } else {
            RadioButton(
                selected = resource.isSelected,
                onClick = null,
            )
        }
    }
}
