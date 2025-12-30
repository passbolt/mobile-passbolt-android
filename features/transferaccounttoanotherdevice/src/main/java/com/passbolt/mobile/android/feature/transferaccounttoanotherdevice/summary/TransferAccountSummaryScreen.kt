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

package com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.summary

import PassboltTheme
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.keys.SettingsNavigationKey.Accounts
import com.passbolt.mobile.android.core.navigation.compose.keys.TransferAccountToAnotherDeviceKey.Onboarding
import com.passbolt.mobile.android.core.ui.compose.button.PrimaryButton
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationHandler
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.TransferAccountNavigation
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.summary.TransferAccountSummaryIntent.GoBack
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.summary.TransferAccountSummaryIntent.Initialize
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.summary.TransferAccountSummaryIntent.PrimaryAction
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.summary.TransferAccountSummaryIntent.TryAgain
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.summary.TransferAccountSummarySideEffect.NavigateToMyAccount
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.summary.TransferAccountSummarySideEffect.NavigateToTransferAccountStart
import com.passbolt.mobile.android.ui.TransferAccountStatusType
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Composable
internal fun TransferAccountSummaryScreen(
    statusType: TransferAccountStatusType,
    modifier: Modifier = Modifier,
    viewModel: TransferAccountSummaryViewModel = koinViewModel(),
    transferStatusFactory: TransferAccountStatusFactory = koinInject(),
    navigator: AppNavigator = koinInject(),
) {
    val state by viewModel.viewState.collectAsState()
    val context = LocalContext.current

    BackHandler {
        viewModel.onIntent(GoBack)
    }

    LaunchedEffect(statusType) {
        viewModel.onIntent(Initialize(statusType))
    }

    val status =
        remember(state.statusType) {
            state.statusType?.let {
                transferStatusFactory.create(it)
            }
        }

    TransferAccountSummaryScreen(
        status = status,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )

    AuthenticationHandler(
        onAuthenticatedIntent = viewModel::onAuthenticationIntent,
        authenticationSideEffect = viewModel.authenticationSideEffect,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            NavigateToMyAccount ->
                if (context is TransferAccountNavigation) {
                    context.close()
                } else {
                    navigator.popToKey(Accounts)
                }
            NavigateToTransferAccountStart ->
                if (context is TransferAccountNavigation) {
                    context.popToKey(Onboarding)
                } else {
                    navigator.popToKey(Onboarding)
                }
        }
    }
}

@Composable
private fun TransferAccountSummaryScreen(
    status: TransferAccountStatus?,
    onIntent: (TransferAccountSummaryIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (status != null) {
                Image(
                    painter = painterResource(status.icon),
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(status.title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Spacer(modifier = Modifier.height(32.dp))

                if (status is TransferAccountStatus.Canceled || status is TransferAccountStatus.Failure) {
                    TextButton(
                        onClick = { onIntent(TryAgain) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(LocalizationR.string.try_again))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                PrimaryButton(
                    text = stringResource(status.buttonText),
                    onClick = { onIntent(PrimaryAction) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TransferAccountSummaryScreenSuccessPreview() {
    PassboltTheme {
        TransferAccountSummaryScreen(statusType = TransferAccountStatusType.SUCCESS)
    }
}

@Preview(showBackground = true)
@Composable
private fun TransferAccountSummaryScreenCanceledPreview() {
    PassboltTheme {
        TransferAccountSummaryScreen(statusType = TransferAccountStatusType.CANCELED)
    }
}

@Preview(showBackground = true)
@Composable
private fun TransferAccountSummaryScreenFailurePreview() {
    PassboltTheme {
        TransferAccountSummaryScreen(statusType = TransferAccountStatusType.FAILURE)
    }
}
