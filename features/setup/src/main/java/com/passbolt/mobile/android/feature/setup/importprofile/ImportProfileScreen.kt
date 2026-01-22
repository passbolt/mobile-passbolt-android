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

package com.passbolt.mobile.android.feature.setup.importprofile

import PassboltTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.keys.SetupNavigationKey.Summary
import com.passbolt.mobile.android.core.ui.compose.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.compose.text.TextInput
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Default
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Error
import com.passbolt.mobile.android.feature.setup.importprofile.ImportProfileIntent.ChangeAccountUrl
import com.passbolt.mobile.android.feature.setup.importprofile.ImportProfileIntent.ChangePrivateKey
import com.passbolt.mobile.android.feature.setup.importprofile.ImportProfileIntent.ChangeUserId
import com.passbolt.mobile.android.feature.setup.importprofile.ImportProfileIntent.GoBack
import com.passbolt.mobile.android.feature.setup.importprofile.ImportProfileIntent.Import
import com.passbolt.mobile.android.feature.setup.importprofile.ImportProfileSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.setup.importprofile.ImportProfileSideEffect.NavigateToSummary
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Composable
internal fun ImportProfileScreen(
    modifier: Modifier = Modifier,
    navigator: AppNavigator = koinInject(),
    viewModel: ImportProfileViewModel = koinViewModel(),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()

    ImportProfileScreen(
        state = state.value,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            NavigateBack -> navigator.navigateBack()
            is NavigateToSummary -> navigator.navigateToKey(Summary(it.status))
        }
    }
}

@Composable
private fun ImportProfileScreen(
    state: ImportProfileState,
    onIntent: (ImportProfileIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        TitleAppBar(
            title = stringResource(LocalizationR.string.import_profile_title),
            navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
        )

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            TextInput(
                title = stringResource(LocalizationR.string.import_profile_user_id),
                hint = stringResource(LocalizationR.string.import_profile_user_id),
                isRequired = true,
                text = state.userId,
                state =
                    if (state.hasUserIdValidationError) {
                        Error(stringResource(LocalizationR.string.import_profile_invalid_uuid))
                    } else {
                        Default
                    },
                onTextChange = { onIntent(ChangeUserId(it)) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            TextInput(
                title = stringResource(LocalizationR.string.import_profile_account_url),
                hint = stringResource(LocalizationR.string.import_profile_account_url),
                isRequired = true,
                text = state.accountUrl,
                state =
                    if (state.hasAccountUrlValidationError) {
                        Error(stringResource(LocalizationR.string.import_profile_invalid_account_url))
                    } else {
                        Default
                    },
                onTextChange = { onIntent(ChangeAccountUrl(it)) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            )

            TextInput(
                title = stringResource(LocalizationR.string.import_profile_private_key),
                hint = stringResource(LocalizationR.string.import_profile_private_key),
                isRequired = true,
                text = state.privateKey,
                state =
                    if (state.hasPrivateKeyValidationError) {
                        Error(stringResource(LocalizationR.string.import_profile_invalid_private_key))
                    } else {
                        Default
                    },
                onTextChange = { onIntent(ChangePrivateKey(it)) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        PrimaryButton(
            text = stringResource(LocalizationR.string.import_profile_import),
            onClick = { onIntent(Import) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun ImportProfileScreenPreview() {
    PassboltTheme {
        ImportProfileScreen(
            state =
                ImportProfileState(
                    userId = "",
                    accountUrl = "",
                    privateKey = "",
                ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ImportProfileScreenWithErrorsPreview() {
    PassboltTheme {
        ImportProfileScreen(
            state =
                ImportProfileState(
                    userId = "invalid-user-id",
                    accountUrl = "invalid-url",
                    privateKey = "invalid-key",
                    hasUserIdValidationError = true,
                    hasAccountUrlValidationError = true,
                    hasPrivateKeyValidationError = true,
                ),
            onIntent = {},
        )
    }
}
