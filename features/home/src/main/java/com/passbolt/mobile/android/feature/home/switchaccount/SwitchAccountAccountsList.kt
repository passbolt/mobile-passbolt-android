package com.passbolt.mobile.android.feature.home.switchaccount

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.localization.R
import com.passbolt.mobile.android.core.ui.compose.button.SecondaryButton
import com.passbolt.mobile.android.core.ui.compose.circularimage.CircularProfileImage
import com.passbolt.mobile.android.core.ui.compose.menu.OpenableSettingsItem
import com.passbolt.mobile.android.ui.SwitchAccountUiModel
import com.passbolt.mobile.android.core.localization.R as LocalizationR
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
fun SwitchAccountAccountsList(
    accountsList: List<SwitchAccountUiModel>,
    onHeaderSeeDetailsClick: () -> Unit,
    onHeaderSignOutClick: () -> Unit,
    onManageAccountsClick: () -> Unit,
    onAccountClick: (SwitchAccountUiModel.AccountItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(accountsList) { item ->
            when (item) {
                is SwitchAccountUiModel.HeaderItem -> {
                    AccountHeaderItem(
                        item = item,
                        onSeeDetailsClick = onHeaderSeeDetailsClick,
                        onSignOutClick = onHeaderSignOutClick,
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = colorResource(CoreUiR.color.divider),
                    )
                }
                is SwitchAccountUiModel.AccountItem -> {
                    AccountItem(
                        item = item,
                        onClick = { onAccountClick(item) },
                    )
                }
                is SwitchAccountUiModel.ManageAccountsItem -> {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = colorResource(CoreUiR.color.divider),
                    )

                    OpenableSettingsItem(
                        title = stringResource(id = R.string.switch_account_manage_accounts),
                        iconPainter = painterResource(CoreUiR.drawable.ic_manage_accounts),
                        onClick = onManageAccountsClick,
                        opensInternally = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountHeaderItem(
    item: SwitchAccountUiModel.HeaderItem,
    onSeeDetailsClick: () -> Unit,
    onSignOutClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box {
                CircularProfileImage(
                    imageUrl = item.avatarUrl,
                    width = 40.dp,
                    height = 40.dp,
                )

                Box(
                    modifier =
                        Modifier
                            .size(8.dp)
                            .background(color = colorResource(CoreUiR.color.green), shape = CircleShape)
                            .align(Alignment.TopEnd),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = item.email,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            SecondaryButton(
                onClick = onSeeDetailsClick,
                text = stringResource(id = LocalizationR.string.switch_account_see_details),
                icon = painterResource(id = CoreUiR.drawable.ic_permission_owner),
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.width(16.dp))

            SecondaryButton(
                onClick = onSignOutClick,
                text = stringResource(id = LocalizationR.string.switch_account_sign_out),
                icon = painterResource(id = CoreUiR.drawable.ic_sign_out),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AccountItem(
    item: SwitchAccountUiModel.AccountItem,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
    ) {
        CircularProfileImage(
            imageUrl = item.avatarUrl,
            width = 40.dp,
            height = 40.dp,
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = item.email,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
