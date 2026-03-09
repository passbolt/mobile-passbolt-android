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

package com.passbolt.mobile.android.feature.authentication.accountslist.ui.list

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.compose.circularimage.CircularProfileImage
import com.passbolt.mobile.android.ui.AccountModelUi.AccountModel

@Composable
internal fun AccountItem(
    account: AccountModel,
    isCurrentUser: Boolean,
    isRemoveMode: Boolean,
    onAccountClick: () -> Unit,
    onTrashClick: () -> Unit,
) {
    Row(
        verticalAlignment =
            Alignment
                .CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable(onClick = onAccountClick)
                .padding(horizontal = 12.dp),
    ) {
        Box {
            CircularProfileImage(
                imageUrl = account.avatar,
                width = 40.dp,
                height = 40.dp,
            )
            if (isCurrentUser) {
                Box(
                    modifier =
                        Modifier
                            .size(9.dp)
                            .background(color = colorResource(R.color.green), shape = CircleShape)
                            .align(Alignment.TopEnd),
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            account.email?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (isRemoveMode) {
            IconButton(onClick = onTrashClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_trash),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountItemPreview() {
    AccountItem(
        account =
            AccountModel(
                userId = "1",
                title = "Ada Lovelace",
                email = "ada@passbolt.com",
                avatar = null,
                url = "https://passbolt.com",
            ),
        isCurrentUser = true,
        isRemoveMode = false,
        onAccountClick = {},
        onTrashClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun AccountItemRemoveModePreview() {
    AccountItem(
        account =
            AccountModel(
                userId = "1",
                title = "Ada Lovelace",
                email = "ada@passbolt.com",
                avatar = null,
                url = "https://passbolt.com",
            ),
        isCurrentUser = false,
        isRemoveMode = true,
        onAccountClick = {},
        onTrashClick = {},
    )
}
