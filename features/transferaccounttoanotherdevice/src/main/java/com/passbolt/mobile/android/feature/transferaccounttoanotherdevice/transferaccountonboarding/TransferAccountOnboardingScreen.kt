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

package com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccountonboarding

import PassboltTheme
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.ActivityIntents.AuthConfig.RefreshPassphrase
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.keys.TransferAccountToAnotherDeviceKey.Transfer
import com.passbolt.mobile.android.core.ui.compose.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.compose.circlestepsview.CircleStepItemModel
import com.passbolt.mobile.android.core.ui.compose.circlestepsview.CircleStepsView
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationHandler
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.TransferAccountNavigation
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccountonboarding.TransferAccountOnboardingIntent.GoBack
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccountonboarding.TransferAccountOnboardingIntent.RefreshedPassphrase
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccountonboarding.TransferAccountOnboardingIntent.StartTransferClick
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccountonboarding.TransferAccountOnboardingScreenSideEffect.NavigateToRefreshPassphrase
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccountonboarding.TransferAccountOnboardingScreenSideEffect.NavigateToTransferAccount
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccountonboarding.TransferAccountOnboardingScreenSideEffect.NavigateUp
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun TransferAccountOnboardingScreen(
    modifier: Modifier = Modifier,
    navigator: AppNavigator = koinInject(),
    viewModel: TransferAccountOnboardingViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val authenticationLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.onIntent(RefreshedPassphrase)
            }
        }

    TransferAccountOnboardingScreen(
        modifier = modifier,
        onIntent = viewModel::onIntent,
    )

    AuthenticationHandler(
        onAuthenticatedIntent = viewModel::onAuthenticationIntent,
        authenticationSideEffect = viewModel.authenticationSideEffect,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            NavigateUp -> {
                if (context is TransferAccountNavigation) {
                    context.navigateBack()
                } else {
                    navigator.navigateBack()
                }
            }
            NavigateToRefreshPassphrase ->
                authenticationLauncher.launch(
                    ActivityIntents.authentication(
                        context,
                        RefreshPassphrase,
                    ),
                )
            NavigateToTransferAccount ->
                if (context is TransferAccountNavigation) {
                    context.navigateToKey(Transfer)
                } else {
                    navigator.navigateToKey(Transfer)
                }
        }
    }
}

@Composable
private fun TransferAccountOnboardingScreen(
    onIntent: (TransferAccountOnboardingIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rawSteps = stringArrayResource(id = LocalizationR.array.transfer_account_onboarding_steps_array)
    val stepModel =
        remember(rawSteps) {
            rawSteps.map {
                CircleStepItemModel(AnnotatedString.fromHtml(it))
            }
        }

    Box(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
        ) {
            TitleAppBar(
                title = stringResource(LocalizationR.string.transfer_account_title),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(LocalizationR.string.transfer_account_onboarding_header),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            CircleStepsView(
                steps = stepModel,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(80.dp))

            Box(
                modifier =
                    Modifier
                        .size(200.dp)
                        .align(Alignment.CenterHorizontally)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(8.dp),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(CoreUiR.drawable.ic_sample_qr_code),
                    contentDescription = null,
                    modifier = Modifier.size(180.dp),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            PrimaryButton(
                text = stringResource(LocalizationR.string.transfer_account_onboarding_transfer_button),
                onClick = { onIntent(StartTransferClick) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("StartTransferButton"), // TODO: move it to :testtags module once MOB-3312 gets resolved
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TransferAccountOnboardingScreenPreview() {
    PassboltTheme {
        TransferAccountOnboardingScreen(
            onIntent = {},
        )
    }
}
