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

package com.passbolt.mobile.android.feature.authentication.accountslist

import PassboltTheme
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.ActivityIntents.AuthConfig
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.Setup
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.Start
import com.passbolt.mobile.android.core.navigation.compose.keys.AuthenticationNavigationKey.Auth
import com.passbolt.mobile.android.core.ui.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.button.SecondaryButton
import com.passbolt.mobile.android.core.ui.dialogs.ConfirmAlertDialog
import com.passbolt.mobile.android.core.ui.progressdialog.ProgressDialog
import com.passbolt.mobile.android.core.ui.snackbar.ColoredSnackbarVisuals
import com.passbolt.mobile.android.core.ui.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListIntent.AddAccount
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListIntent.ConfirmRemoveAccount
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListIntent.DismissRemoveAccountDialog
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListIntent.EnterRemoveAccountMode
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListIntent.ExitRemoveAccountMode
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListIntent.GoBack
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListIntent.RemoveAccount
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListIntent.SelectAccount
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListSideEffect.Finish
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListSideEffect.FinishAffinity
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListSideEffect.NavigateToNewAccountSignIn
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListSideEffect.NavigateToSetup
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListSideEffect.NavigateToSignIn
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListSideEffect.NavigateToStartUp
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListSideEffect.ShowSuccessSnackBar
import com.passbolt.mobile.android.feature.authentication.accountslist.ui.list.AccountItem
import com.passbolt.mobile.android.feature.authentication.accountslist.ui.list.AddNewAccountItem
import com.passbolt.mobile.android.ui.AccountModelUi.AccountModel
import com.passbolt.mobile.android.ui.AccountModelUi.AddNewAccount
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun AccountsListScreen(
    authConfig: AuthConfig,
    viewModel: AccountsListViewModel = koinViewModel(parameters = { parametersOf(authConfig) }),
    navigator: AppNavigator = koinInject(),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    BackHandler { viewModel.onIntent(GoBack) }

    AccountsListScreen(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            is NavigateToSignIn -> navigator.navigateToKey(Auth(it.account.userId))
            is NavigateToNewAccountSignIn ->
                navigator.navigateToKey(Auth(it.account.userId, authConfig = AuthConfig.Startup))
            NavigateToSetup -> navigator.startNavigationActivity(context, Setup)
            NavigateToStartUp -> navigator.startNavigationActivity(context, Start)
            FinishAffinity -> navigator.finishAffinity(activity)
            Finish -> navigator.finishActivity(activity)
            is ShowSuccessSnackBar -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        ColoredSnackbarVisuals(
                            message = getSnackBarMessage(context, it.type),
                            backgroundColor = Color(context.getColor(CoreUiR.color.background_gray_dark)),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountsListScreen(
    state: AccountsListState,
    onIntent: (AccountsListIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        topBar = {
            if (state.showManageAccountsTopBar) {
                TitleAppBar(
                    title = stringResource(LocalizationR.string.accounts_list_manage_accounts),
                    navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
                )
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    val customVisuals = data.visuals as? ColoredSnackbarVisuals
                    if (customVisuals != null) {
                        Snackbar(
                            snackbarData = data,
                            containerColor = customVisuals.backgroundColor,
                            contentColor = customVisuals.contentColor,
                        )
                    } else {
                        Snackbar(snackbarData = data)
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.background,
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                if (state.isRemoveMode) {
                    PrimaryButton(
                        text = stringResource(LocalizationR.string.accounts_list_removing_done),
                        onClick = { onIntent(ExitRemoveAccountMode) },
                    )
                } else {
                    SecondaryButton(
                        onClick = { onIntent(EnterRemoveAccountMode) },
                        text = stringResource(LocalizationR.string.accounts_list_remove),
                        icon = painterResource(CoreUiR.drawable.ic_trash),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.showHeader) {
                Spacer(modifier = Modifier.height(24.dp))
                Image(
                    painter = painterResource(CoreUiR.drawable.logo_text_icon),
                    contentDescription = null,
                    modifier = Modifier.height(24.dp),
                )
                Spacer(modifier = Modifier.height(80.dp))
                Text(
                    text = stringResource(LocalizationR.string.accounts_list_title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(LocalizationR.string.accounts_list_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .border(1.dp, colorResource(CoreUiR.color.divider), RoundedCornerShape(15.dp))
                        .clip(RoundedCornerShape(15.dp)),
            ) {
                state.visibleAccounts.forEachIndexed { index, item ->
                    if (index > 0) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = colorResource(CoreUiR.color.divider),
                        )
                    }
                    when (item) {
                        is AccountModel -> {
                            AccountItem(
                                account = item,
                                isCurrentUser = item.userId == state.currentUserId,
                                isRemoveMode = state.isRemoveMode,
                                onAccountClick = { onIntent(SelectAccount(item)) },
                                onTrashClick = { onIntent(RemoveAccount(item)) },
                            )
                        }
                        is AddNewAccount -> {
                            AddNewAccountItem(
                                onClick = { onIntent(AddAccount) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.showAccountRemovalConfirmation && state.accountToRemove != null) {
        ConfirmAlertDialog(
            titleResId = LocalizationR.string.are_you_sure,
            messageResId = LocalizationR.string.accounts_list_remove_account_message,
            positiveButtonResId = LocalizationR.string.accounts_list_remove_account,
            onConfirm = { onIntent(ConfirmRemoveAccount(state.accountToRemove)) },
            onDismiss = { onIntent(DismissRemoveAccountDialog) },
        )
    }

    ProgressDialog(isVisible = state.showProgress)
}

@Preview(showBackground = true)
@Composable
private fun AccountsListScreenAuthPreview() {
    PassboltTheme {
        AccountsListScreen(
            state =
                AccountsListState(
                    accounts =
                        listOf(
                            AccountModel(
                                userId = "1",
                                title = "Ada Lovelace",
                                email = "ada@passbolt.com",
                                avatar = null,
                                url = "https://passbolt.com",
                            ),
                            AccountModel(
                                userId = "2",
                                title = "Betty",
                                email = "betty@passbolt.com",
                                avatar = null,
                                url = "https://passbolt.com",
                            ),
                            AddNewAccount,
                        ),
                    currentUserId = "1",
                    showManageAccountsTopBar = false,
                    showHeader = true,
                ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountsListScreenManagePreview() {
    PassboltTheme {
        AccountsListScreen(
            state =
                AccountsListState(
                    accounts =
                        listOf(
                            AccountModel(
                                userId = "1",
                                title = "Ada Lovelace",
                                email = "ada@passbolt.com",
                                avatar = null,
                                url = "https://passbolt.com",
                            ),
                        ),
                    showManageAccountsTopBar = true,
                    showHeader = false,
                    isRemoveMode = false,
                ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountsListScreenManageRemoveModePreview() {
    PassboltTheme {
        AccountsListScreen(
            state =
                AccountsListState(
                    accounts =
                        listOf(
                            AccountModel(
                                userId = "1",
                                title = "Ada Lovelace",
                                email = "ada@passbolt.com",
                                avatar = null,
                                url = "https://passbolt.com",
                            ),
                        ),
                    showManageAccountsTopBar = true,
                    showHeader = false,
                    isRemoveMode = true,
                ),
            onIntent = {},
        )
    }
}
