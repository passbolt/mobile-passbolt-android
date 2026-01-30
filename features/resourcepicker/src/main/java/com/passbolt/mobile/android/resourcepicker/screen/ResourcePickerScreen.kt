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

package com.passbolt.mobile.android.resourcepicker.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.fulldatarefresh.service.DataRefreshService
import com.passbolt.mobile.android.core.ui.compose.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.compose.dialogs.ConfirmAlertDialog
import com.passbolt.mobile.android.core.ui.compose.scaffold.HomeScaffold
import com.passbolt.mobile.android.core.ui.compose.search.SearchInput
import com.passbolt.mobile.android.core.ui.compose.snackbar.ColoredSnackbarVisuals
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationHandler
import com.passbolt.mobile.android.resourcepicker.ResourcePickerNavigation
import com.passbolt.mobile.android.resourcepicker.model.ConfirmationModelFactory
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerIntent.CloseConfirmationDialog
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerIntent.ConfirmOtpLink
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerIntent.GoBack
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerIntent.Initialize
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerIntent.Search
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerIntent.SearchEndIconAction
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerSideEffect.NavigateBackWithResult
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerSideEffect.NavigateUp
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.resourcepicker.screen.list.ResourcePickerList
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun ResourcePickerScreen(
    suggestionUri: String?,
    navigation: ResourcePickerNavigation,
    modifier: Modifier = Modifier,
    viewModel: ResourcePickerViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(suggestionUri) {
        viewModel.onIntent(Initialize(suggestionUri))
    }

    ResourcePickerScreen(
        state = state.value,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    AuthenticationHandler(
        onAuthenticatedIntent = viewModel::onAuthenticationIntent,
        authenticationSideEffect = viewModel.authenticationSideEffect,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            is ShowErrorSnackbar ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        ColoredSnackbarVisuals(
                            message = getErrorMessage(context, it.type),
                            backgroundColor = Color(context.getColor(CoreUiR.color.red)),
                        ),
                    )
                }
            is NavigateBackWithResult -> navigation.navigateBackWithResult(it.pickAction, it.resourceModel)
            NavigateUp -> navigation.navigateUp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResourcePickerScreen(
    state: ResourcePickerState,
    onIntent: (ResourcePickerIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    confirmationModelFactory: ConfirmationModelFactory = koinInject(),
) {
    val context = LocalContext.current

    HomeScaffold(
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        appBarTitle = stringResource(LocalizationR.string.otp_create_totp_link_to_password),
        shouldShowBackIcon = true,
        onBackClick = { onIntent(GoBack) },
        appBarSearchInput = {
            SearchInput(
                onValueChange = { onIntent(Search(it)) },
                placeholder = stringResource(LocalizationR.string.default_home_search_hint),
                endIconMode = state.searchInputEndIconMode,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp),
                onEndIconClick = { onIntent(SearchEndIconAction) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            )
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = colorResource(CoreUiR.color.elevated_background),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                ) {
                    PrimaryButton(
                        text = stringResource(LocalizationR.string.apply),
                        isEnabled = state.isApplyButtonEnabled,
                        onClick = { onIntent(ResourcePickerIntent.ApplyClick) },
                    )
                }
            }
        },
        content = { paddingValues ->
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { DataRefreshService.start(context) },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
            ) {
                ResourcePickerList(state, onIntent)
            }

            if (state.showConfirmationDialog && state.confirmationType != null && state.pickAction != null) {
                val confirmationModel = confirmationModelFactory.create(state.confirmationType)
                ConfirmAlertDialog(
                    titleResId = confirmationModel.titleResId,
                    messageResId = confirmationModel.messageResId,
                    positiveButtonResId = confirmationModel.positiveButtonResId,
                    onConfirm = { onIntent(ConfirmOtpLink(state.pickAction)) },
                    onDismiss = { onIntent(CloseConfirmationDialog) },
                )
            }
        },
    )
}
