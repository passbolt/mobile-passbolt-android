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

package com.passbolt.mobile.android.groupdetails.groupmemberdetails

import PassboltTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.ui.compose.circularimage.CircularProfileImage
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.core.ui.formatter.FingerprintFormatter
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationHandler
import com.passbolt.mobile.android.groupdetails.groupmemberdetails.GroupMemberDetailsIntent.GoBack
import com.passbolt.mobile.android.groupdetails.groupmemberdetails.GroupMemberDetailsIntent.Initialize
import com.passbolt.mobile.android.groupdetails.groupmemberdetails.GroupMemberDetailsSideEffect.NavigateUp
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Composable
internal fun GroupMemberDetailsScreen(
    userId: String,
    navigation: GroupMemberDetailsNavigation,
    modifier: Modifier = Modifier,
    viewModel: GroupMemberDetailsViewModel = koinViewModel(),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()

    LaunchedEffect(userId) {
        viewModel.onIntent(Initialize(userId))
    }

    AuthenticationHandler(
        onAuthenticatedIntent = viewModel::onAuthenticationIntent,
        authenticationSideEffect = viewModel.authenticationSideEffect,
    )

    GroupMemberDetailsContent(
        state = state.value,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            NavigateUp -> navigation.navigateUp()
        }
    }
}

@Composable
private fun GroupMemberDetailsContent(
    state: GroupMemberDetailsState,
    onIntent: (GroupMemberDetailsIntent) -> Unit,
    modifier: Modifier = Modifier,
    fingerprintFormatter: FingerprintFormatter = koinInject(),
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
    ) {
        TitleAppBar(
            title = stringResource(LocalizationR.string.group_member_details_group_member),
            navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
        )

        CircularProfileImage(
            state.avatarUrl,
            width = 96.dp,
            height = 96.dp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Text(
            text = state.fullName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 32.dp),
        )

        Text(
            text = state.userName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp),
        )

        Text(
            text =
                fingerprintFormatter
                    .format(
                        state.fingerprint,
                        appendMiddleSpacing = true,
                    )?.uppercase()
                    .orEmpty(),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GroupMemberDetailsPreview() {
    PassboltTheme {
        GroupMemberDetailsContent(
            state =
                GroupMemberDetailsState(
                    userName = "passbolt@passbolt.com",
                    firstName = "Grace",
                    lastName = "Hopper",
                    avatarUrl = null,
                    fingerprint = "0C1D1761110D1E33C9006D1A5B1B332ED06426D3",
                ),
            onIntent = {},
        )
    }
}
