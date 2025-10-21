package com.passbolt.mobile.android.core.ui.compose.menu

import PassboltTheme
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.passbolt.mobile.android.core.ui.R

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
fun MenuItem(
    title: String,
    @DrawableRes iconResId: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    isSelected: Boolean = false,
) {
    val backgroundColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            colorResource(R.color.elevated_background)
        }

    val textColor =
        if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onBackground
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            tint = if (isSelected) textColor else colorResource(R.color.icon_tint),
            modifier =
                Modifier
                    .size(24.dp),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MenuItemPreview() {
    MaterialTheme {
        MenuItem(
            title = "Menu Item",
            iconResId = R.drawable.ic_list,
            isSelected = false,
            onClick = { },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MenuItemSelectedPreview() {
    PassboltTheme {
        MenuItem(
            title = "Selected Menu Item",
            iconResId = R.drawable.ic_list,
            isSelected = true,
        )
    }
}
