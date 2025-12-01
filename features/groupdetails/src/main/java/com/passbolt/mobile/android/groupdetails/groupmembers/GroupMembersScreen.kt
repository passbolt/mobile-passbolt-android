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

package com.passbolt.mobile.android.groupdetails.groupmembers

import PassboltTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.groupdetails.groupmembers.GroupMembersIntent.GoBack
import com.passbolt.mobile.android.groupdetails.groupmembers.GroupMembersIntent.GoToMemberDetails
import com.passbolt.mobile.android.groupdetails.groupmembers.GroupMembersIntent.Initialize
import com.passbolt.mobile.android.groupdetails.groupmembers.GroupMembersSideEffect.NavigateToMemberDetails
import com.passbolt.mobile.android.groupdetails.groupmembers.GroupMembersSideEffect.NavigateUp
import com.passbolt.mobile.android.ui.GpgKeyModel
import com.passbolt.mobile.android.ui.UserModel
import com.passbolt.mobile.android.ui.UserProfileModel
import org.koin.androidx.compose.koinViewModel
import java.time.ZonedDateTime
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun GroupMembersScreen(
    groupId: String,
    navigation: GroupMembersNavigation,
    modifier: Modifier = Modifier,
    viewModel: GroupMembersViewModel = koinViewModel(),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()

    LaunchedEffect(groupId) {
        viewModel.onIntent(Initialize(groupId))
    }

    GroupMembersContent(
        state = state.value,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            NavigateUp -> navigation.navigateUp()
            is NavigateToMemberDetails -> navigation.navigateToMemberDetails(it.userId)
        }
    }
}

@Composable
private fun GroupMembersContent(
    state: GroupMembersState,
    onIntent: (GroupMembersIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        TitleAppBar(
            title = stringResource(LocalizationR.string.group_members_title),
            navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
        )

        Image(
            painter = painterResource(id = CoreUiR.drawable.ic_filled_group_with_bg),
            contentDescription = stringResource(LocalizationR.string.group_members_title),
            modifier =
                Modifier
                    .size(96.dp)
                    .align(Alignment.CenterHorizontally),
        )

        Text(
            text = state.groupName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.CenterHorizontally),
        )

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = 32.dp),
        ) {
            items(
                items = state.members,
                key = { it.id },
            ) { member ->
                GroupMemberItem(
                    member = member,
                    onClick = { onIntent(GoToMemberDetails(member.id)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GroupMembersPreview() {
    PassboltTheme {
        GroupMembersContent(
            state =
                GroupMembersState(
                    groupName = "Development Team",
                    members =
                        listOf(
                            UserModel(
                                id = "1",
                                userName = "grace@passbolt.com",
                                disabled = false,
                                gpgKey =
                                    GpgKeyModel(
                                        id = "1",
                                        armoredKey = "",
                                        fingerprint = "03F60E958F4CB29723ACDF761353B5B15D9B054F",
                                        bits = 2048,
                                        uid = null,
                                        keyId = "12345",
                                        type = "RSA",
                                        keyExpirationDate = null,
                                        keyCreationDate = ZonedDateTime.now(),
                                    ),
                                profile =
                                    UserProfileModel(
                                        username = "grace",
                                        firstName = "Grace",
                                        lastName = "Hopper",
                                        avatarUrl = null,
                                    ),
                            ),
                            UserModel(
                                id = "2",
                                userName = "ada@passbolt.com",
                                disabled = false,
                                gpgKey =
                                    GpgKeyModel(
                                        id = "2",
                                        armoredKey = "",
                                        fingerprint = "03F60E958F4CB29723ACDF761353B5B15D9B054F",
                                        bits = 2048,
                                        uid = null,
                                        keyId = "67890",
                                        type = "RSA",
                                        keyExpirationDate = null,
                                        keyCreationDate = ZonedDateTime.now(),
                                    ),
                                profile =
                                    UserProfileModel(
                                        username = "ada",
                                        firstName = "Ada",
                                        lastName = "Lovelace",
                                        avatarUrl = null,
                                    ),
                            ),
                        ),
                ),
            onIntent = {},
        )
    }
}
