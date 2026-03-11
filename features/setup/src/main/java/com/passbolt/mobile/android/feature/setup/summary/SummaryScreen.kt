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

package com.passbolt.mobile.android.feature.setup.summary

import PassboltTheme
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.AuthenticationManageAccounts
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.Start
import com.passbolt.mobile.android.core.navigation.compose.keys.LogsNavigationKey.Logs
import com.passbolt.mobile.android.core.navigation.compose.keys.SetupNavigationKey.FingerprintSetup
import com.passbolt.mobile.android.core.navigation.compose.keys.SetupNavigationKey.Welcome
import com.passbolt.mobile.android.core.ui.compose.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.compose.dialogs.LeaveSetupAlertDialog
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.AccessLogs
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.AuthenticationSuccess
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.ConfirmSetupLeave
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.DismissHelpMenu
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.DismissSetupLeave
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.GoBack
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.Initialize
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.OpenHelpMenu
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.PrimaryButtonAction
import com.passbolt.mobile.android.feature.setup.summary.SummarySideEffect.NavigateToAppStart
import com.passbolt.mobile.android.feature.setup.summary.SummarySideEffect.NavigateToFingerprintSetup
import com.passbolt.mobile.android.feature.setup.summary.SummarySideEffect.NavigateToLogs
import com.passbolt.mobile.android.feature.setup.summary.SummarySideEffect.NavigateToManageAccounts
import com.passbolt.mobile.android.feature.setup.summary.SummarySideEffect.NavigateToSignIn
import com.passbolt.mobile.android.feature.setup.summary.SummarySideEffect.NavigateToWelcome
import com.passbolt.mobile.android.helpmenu.HelpMenuBottomSheet
import com.passbolt.mobile.android.ui.HelpMenuModel
import com.passbolt.mobile.android.ui.ResultStatus
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
fun SummaryScreen(
    status: ResultStatus,
    modifier: Modifier = Modifier,
    navigator: AppNavigator = koinInject(),
    viewModel: SummaryViewModel = koinViewModel(),
) {
    val state by viewModel.viewState.collectAsState()
    val context = LocalContext.current

    BackHandler {
        viewModel.onIntent(GoBack)
    }

    LaunchedEffect(status) {
        viewModel.onIntent(Initialize(status))
    }

    val authenticationResult =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                viewModel.onIntent(AuthenticationSuccess)
            }
        }

    SummaryScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            NavigateToWelcome -> navigator.popToKey(Welcome)
            NavigateToFingerprintSetup -> navigator.navigateToKey(FingerprintSetup)
            is NavigateToSignIn ->
                authenticationResult.launch(
                    ActivityIntents.authentication(
                        context,
                        ActivityIntents.AuthConfig.Setup,
                        userId = sideEffect.userId,
                    ),
                )
            NavigateToAppStart -> navigator.startNavigationActivity(context, Start)
            NavigateToManageAccounts ->
                navigator.startNavigationActivity(
                    context,
                    AuthenticationManageAccounts,
                    Intent.FLAG_ACTIVITY_CLEAR_TOP,
                )
            NavigateToLogs -> navigator.navigateToKey(Logs)
        }
    }
}

@Composable
private fun SummaryScreen(
    state: SummaryState,
    onIntent: (SummaryIntent) -> Unit,
    modifier: Modifier = Modifier,
    resultStatusUiFactory: ResultStatusUiFactory = koinInject(),
) {
    val statusUi =
        remember(state.status) {
            state.status?.let {
                resultStatusUiFactory.create(it.resultStatusType)
            }
        }
    Scaffold(
        modifier = modifier,
        topBar = {
            if (state.status is ResultStatus.Failure) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                    contentAlignment = Alignment.TopEnd,
                ) {
                    IconButton(onClick = { onIntent(OpenHelpMenu) }) {
                        Icon(
                            painter = painterResource(CoreUiR.drawable.ic_help),
                            contentDescription = null,
                            tint = colorResource(CoreUiR.color.icon_tint),
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (statusUi != null) {
                        Image(
                            painter = painterResource(statusUi.icon),
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = stringResource(statusUi.title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        )

                        if (state.status is ResultStatus.Failure) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = state.status.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            if (statusUi != null) {
                PrimaryButton(
                    text = stringResource(statusUi.buttonText),
                    onClick = { onIntent(PrimaryButtonAction) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        LeaveSetupAlertDialog(
            isVisible = state.showSetupLeaveConfirmationDialog,
            onLeaveConfirm = { onIntent(ConfirmSetupLeave) },
            onDismiss = { onIntent(DismissSetupLeave) },
        )

        if (state.showHelpMenu) {
            HelpMenuBottomSheet(
                helpMenuModel =
                    HelpMenuModel(
                        shouldShowShowQrCodesHelp = false,
                        shouldShowImportProfile = false,
                        shouldShowImportAccountKit = false,
                    ),
                onDismissRequest = { onIntent(DismissHelpMenu) },
                onAccessLogs = {
                    onIntent(DismissHelpMenu)
                    onIntent(AccessLogs)
                },
            )
        }
    }
}

@Preview
@Composable
private fun SummaryScreenSuccessPreview() {
    PassboltTheme {
        SummaryScreen(
            state =
                SummaryState(
                    status = ResultStatus.Success(userId = "test-user-id"),
                    showSetupLeaveConfirmationDialog = false,
                    showHelpMenu = false,
                ),
            onIntent = {},
            resultStatusUiFactory = ResultStatusUiFactory(),
        )
    }
}

@Preview
@Composable
private fun SummaryScreenFailurePreview() {
    PassboltTheme {
        SummaryScreen(
            state =
                SummaryState(
                    status = ResultStatus.Failure(message = "An error occurred during account setup. Please try again."),
                    showSetupLeaveConfirmationDialog = false,
                    showHelpMenu = false,
                ),
            onIntent = {},
            resultStatusUiFactory = ResultStatusUiFactory(),
        )
    }
}

@Preview
@Composable
private fun SummaryScreenWithLeaveConfirmationDialogPreview() {
    PassboltTheme {
        SummaryScreen(
            state =
                SummaryState(
                    status = ResultStatus.Success(userId = "test-user-id"),
                    showSetupLeaveConfirmationDialog = true,
                    showHelpMenu = false,
                ),
            onIntent = {},
            resultStatusUiFactory = ResultStatusUiFactory(),
        )
    }
}
