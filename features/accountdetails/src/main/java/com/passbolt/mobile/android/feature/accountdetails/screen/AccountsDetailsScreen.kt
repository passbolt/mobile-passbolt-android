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

package com.passbolt.mobile.android.feature.accountdetails.screen

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.TransferAccount
import com.passbolt.mobile.android.core.ui.compose.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.compose.circularimage.CircularProfileImage
import com.passbolt.mobile.android.core.ui.compose.labelledtext.LabelledText
import com.passbolt.mobile.android.core.ui.compose.text.TextInput
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Default
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Error
import com.passbolt.mobile.android.feature.accountdetails.AccountDetailsActivity
import com.passbolt.mobile.android.feature.accountdetails.screen.AccountDetailsIntent.GoBack
import com.passbolt.mobile.android.feature.accountdetails.screen.AccountDetailsIntent.StartTransferAccount
import com.passbolt.mobile.android.feature.accountdetails.screen.AccountDetailsIntent.UpdateLabel
import com.passbolt.mobile.android.feature.accountdetails.screen.AccountDetailsScreenSideEffect.NavigateToTransferAccount
import com.passbolt.mobile.android.feature.accountdetails.screen.AccountDetailsScreenSideEffect.NavigateUp
import com.passbolt.mobile.android.feature.accountdetails.screen.AccountDetailsValidationError.MaxLengthExceeded
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Composable
internal fun AccountDetailsScreen(
    modifier: Modifier = Modifier,
    navigator: AppNavigator = koinInject(),
    viewModel: AccountDetailsViewModel = koinViewModel(),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    AccountDetailsScreen(
        state = state.value,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            NavigateUp ->
                if (context is AccountDetailsActivity) {
                    // TODO handle after migration switch account flow to compose
                    context.finish()
                } else {
                    navigator.navigateBack()
                }
            NavigateToTransferAccount -> navigator.startNavigationActivity(context, TransferAccount)
        }
    }
}

@Composable
private fun AccountDetailsScreen(
    state: AccountDetailsState,
    onIntent: (AccountDetailsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState()),
    ) {
        TitleAppBar(
            title = stringResource(LocalizationR.string.settings_accounts_account_details),
            navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
        )

        CircularProfileImage(
            state.avatarUrl,
            width = 96.dp,
            height = 96.dp,
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 32.dp),
        )

        TextInput(
            title = stringResource(LocalizationR.string.account_details_hint_label),
            hint = stringResource(LocalizationR.string.account_details_hint_label),
            isRequired = true,
            text = state.label,
            state =
                if (state.labelValidationErrors.isEmpty()) {
                    Default
                } else {
                    Error(
                        getLabelErrorMessage(
                            context,
                            state.labelValidationErrors,
                        ),
                    )
                },
            onTextChange = { onIntent(UpdateLabel(it)) },
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Text(
            text = stringResource(LocalizationR.string.account_details_label_desc),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        LabelledText(
            label = stringResource(LocalizationR.string.account_details_label_name),
            text = state.name,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
        )

        LabelledText(
            label = stringResource(LocalizationR.string.account_details_label_email),
            text = state.email,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
        )

        state.role?.let {
            LabelledText(
                label = stringResource(LocalizationR.string.account_details_label_role),
                text = it,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp),
            )
        }

        LabelledText(
            label = stringResource(LocalizationR.string.account_details_label_org),
            text = state.name,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
        )

        Spacer(
            modifier =
                Modifier
                    .weight(1f, fill = true)
                    .padding(top = 16.dp),
        )

        PrimaryButton(
            text = stringResource(LocalizationR.string.save),
            onClick = { onIntent(AccountDetailsIntent.SaveChanges) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
        )

        Text(
            text = stringResource(LocalizationR.string.settings_accounts_transfer_account),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 64.dp, vertical = 24.dp)
                    .clickable { onIntent(StartTransferAccount) },
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

private fun getLabelErrorMessage(
    context: Context,
    labelValidationErrors: List<AccountDetailsValidationError>,
): String =
    when (val error = labelValidationErrors.first()) {
        is MaxLengthExceeded ->
            context.getString(
                LocalizationR.string.validation_required_with_max_length,
                error.maxLength,
            )
    }

@Preview(showBackground = true)
@Composable
private fun AccountDetailsPreview() {
    AccountDetailsScreen(
        state =
            AccountDetailsState(
                label = "Grace Hopper",
                name = "Grace Hopper",
                email = "grace@passbolt.com",
                role = "user",
                organizationUrl = "https://www.passbolt.com",
            ),
        onIntent = {},
    )
}
