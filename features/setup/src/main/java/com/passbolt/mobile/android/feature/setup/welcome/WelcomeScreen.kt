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

package com.passbolt.mobile.android.feature.setup.welcome

import PassboltTheme
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.keys.LogsNavigationKey.Logs
import com.passbolt.mobile.android.core.navigation.compose.keys.SetupNavigationKey.ImportProfile
import com.passbolt.mobile.android.core.navigation.compose.keys.SetupNavigationKey.Summary
import com.passbolt.mobile.android.core.navigation.compose.keys.SetupNavigationKey.TransferDetails
import com.passbolt.mobile.android.core.ui.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.dialogs.HowToCreateAccountDialog
import com.passbolt.mobile.android.core.ui.dialogs.RootWarningAlertDialog
import com.passbolt.mobile.android.core.ui.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.AccessLogs
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.AcknowledgeDeviceRooted
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.ConnectToExistingAccount
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.DismissHelpMenu
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.DismissNoAccountExplanation
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.GoUp
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.ImportProfileManually
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.Initialize
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.OpenHelpMenu
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.SeeNoAccountExplanation
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.SelectedAccountKit
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeSideEffect.NavigateToImportProfile
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeSideEffect.NavigateToLogs
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeSideEffect.NavigateToSummary
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeSideEffect.NavigateToTransferDetails
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeSideEffect.NavigateUp
import com.passbolt.mobile.android.helpmenu.HelpMenuBottomSheet
import com.passbolt.mobile.android.testtags.composetags.Setup.APPS_IMAGE
import com.passbolt.mobile.android.testtags.composetags.Setup.HELP_BUTTON
import com.passbolt.mobile.android.testtags.composetags.Setup.LOGO_IMAGE
import com.passbolt.mobile.android.ui.HelpMenuModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun WelcomeScreen(
    modifier: Modifier = Modifier,
    viewModel: WelcomeViewModel = koinViewModel(),
    navigation: AppNavigator = koinInject(),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current

    LaunchedEffect(Unit) {
        viewModel.onIntent(Initialize(isTaskRoot = activity?.isTaskRoot == true))
    }

    WelcomeScreen(
        modifier = modifier,
        state = state.value,
        onIntent = viewModel::onIntent,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            NavigateUp -> navigation.navigateUp(activity)
            NavigateToImportProfile -> navigation.navigateToKey(ImportProfile)
            NavigateToLogs -> navigation.navigateToKey(Logs)
            is NavigateToSummary -> navigation.navigateToKey(Summary(it.status))
            NavigateToTransferDetails -> navigation.navigateToKey(TransferDetails)
        }
    }
}

@Composable
private fun WelcomeScreen(
    state: WelcomeState,
    onIntent: (WelcomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TitleAppBar(
                title = "",
                navigationIcon =
                    if (state.showBackNavigation) {
                        { BackNavigationIcon(onBackClick = { onIntent(GoUp) }) }
                    } else {
                        {}
                    },
                actions = {
                    IconButton(
                        onClick = { onIntent(OpenHelpMenu) },
                        modifier = Modifier.testTag(HELP_BUTTON),
                    ) {
                        Icon(
                            painter = painterResource(CoreUiR.drawable.ic_help),
                            contentDescription = null,
                            tint = colorResource(CoreUiR.color.icon_tint),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(CoreUiR.drawable.logo_text_icon),
                contentDescription = null,
                modifier =
                    Modifier
                        .padding(top = 48.dp)
                        .size(116.dp, 48.dp)
                        .testTag(LOGO_IMAGE),
            )

            Image(
                painter = painterResource(CoreUiR.drawable.apps_list),
                contentDescription = null,
                modifier =
                    Modifier
                        .padding(top = 64.dp)
                        .size(width = 300.dp, height = 160.dp)
                        .testTag(APPS_IMAGE),
            )

            Text(
                text = stringResource(LocalizationR.string.welcome_title),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 56.dp),
            )

            Text(
                text = stringResource(LocalizationR.string.welcome_body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier =
                    Modifier
                        .padding(top = 16.dp)
                        .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            PrimaryButton(
                text = stringResource(LocalizationR.string.welcome_connect_to_existing_account),
                onClick = { onIntent(ConnectToExistingAccount) },
                modifier = Modifier.padding(top = 24.dp),
            )

            TextButton(
                onClick = { onIntent(SeeNoAccountExplanation) },
                modifier = Modifier.padding(top = 24.dp, bottom = 32.dp),
            ) {
                Text(text = stringResource(LocalizationR.string.welcome_no_account), color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }

    HowToCreateAccountDialog(
        isVisible = state.showNoAccountExplanation,
        onDismiss = { onIntent(DismissNoAccountExplanation) },
    )

    RootWarningAlertDialog(
        isVisible = state.showDeviceRooted,
        onAcknowledge = { onIntent(AcknowledgeDeviceRooted) },
    )

    if (state.showHelpMenu) {
        HelpMenuBottomSheet(
            HelpMenuModel(
                shouldShowShowQrCodesHelp = true,
                shouldShowImportProfile = true,
                shouldShowImportAccountKit = true,
            ),
            onDismissRequest = { onIntent(DismissHelpMenu) },
            onImportProfileManually = { onIntent(ImportProfileManually) },
            onAccessLogs = { onIntent(AccessLogs) },
            onAccountKitSelect = { onIntent(SelectedAccountKit(it)) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    PassboltTheme {
        WelcomeScreen(
            state = WelcomeState(showBackNavigation = false),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenWithBackPreview() {
    PassboltTheme {
        WelcomeScreen(
            state = WelcomeState(showBackNavigation = true),
            onIntent = {},
        )
    }
}
