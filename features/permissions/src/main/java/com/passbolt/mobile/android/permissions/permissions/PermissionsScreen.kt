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

package com.passbolt.mobile.android.permissions.permissions

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.GroupPermissionDetails
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.PermissionRecipients
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.Permissions
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.UserPermissionDetails
import com.passbolt.mobile.android.core.navigation.compose.results.NavigationResultEventBus
import com.passbolt.mobile.android.core.navigation.compose.results.ShareCompleteResult
import com.passbolt.mobile.android.core.ui.compose.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.compose.fab.AddFloatingActionButton
import com.passbolt.mobile.android.core.ui.compose.progressdialog.ProgressDialog
import com.passbolt.mobile.android.core.ui.compose.snackbar.ColoredSnackbarVisuals
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationHandler
import com.passbolt.mobile.android.feature.metadatakeytrust.ui.compose.NewMetadataKeyTrustDialog
import com.passbolt.mobile.android.feature.metadatakeytrust.ui.compose.TrustedMetadataKeyDeletedDialog
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.AddPermission
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.DismissMetadataKeyDeletedDialog
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.DismissMetadataKeyModifiedDialog
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.GoBack
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.MainButtonIntent
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.SeePermission
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.TrustNewMetadataKey
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.TrustedMetadataKeyDeleted
import com.passbolt.mobile.android.permissions.permissions.PermissionsSideEffect.CloseWithShareSuccess
import com.passbolt.mobile.android.permissions.permissions.PermissionsSideEffect.NavigateBack
import com.passbolt.mobile.android.permissions.permissions.PermissionsSideEffect.NavigateToGroupPermissionDetails
import com.passbolt.mobile.android.permissions.permissions.PermissionsSideEffect.NavigateToHome
import com.passbolt.mobile.android.permissions.permissions.PermissionsSideEffect.NavigateToSelectShareRecipients
import com.passbolt.mobile.android.permissions.permissions.PermissionsSideEffect.NavigateToSelfWithMode
import com.passbolt.mobile.android.permissions.permissions.PermissionsSideEffect.NavigateToUserPermissionDetails
import com.passbolt.mobile.android.permissions.permissions.PermissionsSideEffect.ShowContentNotAvailable
import com.passbolt.mobile.android.permissions.permissions.PermissionsSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.permissions.permissions.PermissionsSideEffect.ShowSuccessSnackbar
import com.passbolt.mobile.android.permissions.permissions.ui.EmptyPermissionsState
import com.passbolt.mobile.android.permissions.permissions.ui.PermissionsList
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
fun PermissionsScreen(
    viewModel: PermissionsViewModel,
    modifier: Modifier = Modifier,
    navigator: AppNavigator = koinInject(),
) {
    val context = LocalContext.current
    val resultBus = NavigationResultEventBus.current
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    AuthenticationHandler(
        onAuthenticatedIntent = viewModel::onAuthenticationIntent,
        authenticationSideEffect = viewModel.authenticationSideEffect,
    )

    PermissionsScreen(
        state = state.value,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    SideEffectDispatcher(viewModel.sideEffect) { effect ->
        when (effect) {
            NavigateBack -> navigator.navigateBack()
            is NavigateToGroupPermissionDetails ->
                navigator.navigateToKey(
                    GroupPermissionDetails(
                        permission = effect.permission,
                        mode = effect.mode,
                    ),
                )
            is NavigateToUserPermissionDetails ->
                navigator.navigateToKey(
                    UserPermissionDetails(
                        permission = effect.permission,
                        mode = effect.mode,
                    ),
                )
            is NavigateToSelectShareRecipients ->
                navigator.navigateToKey(
                    PermissionRecipients(
                        userPermissions = effect.users,
                        groupPermissions = effect.groups,
                    ),
                )
            is NavigateToSelfWithMode ->
                navigator.navigateToKey(Permissions(effect.id, effect.mode, effect.permissionsItem))
            CloseWithShareSuccess -> {
                resultBus.sendResult(result = ShareCompleteResult(shared = true))
                navigator.navigateBack()
            }
            NavigateToHome -> navigator.popToKey(navigator.backStack.first())
            ShowContentNotAvailable ->
                Toast
                    .makeText(context, LocalizationR.string.content_not_available, Toast.LENGTH_SHORT)
                    .show()
            is ShowErrorSnackbar ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        ColoredSnackbarVisuals(
                            message = getErrorMessage(context, effect.type),
                            backgroundColor = Color(context.getColor(CoreUiR.color.red)),
                        ),
                    )
                }
            is ShowSuccessSnackbar ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        ColoredSnackbarVisuals(
                            message = getSuccessMessage(context, effect.type),
                            backgroundColor = Color(context.getColor(CoreUiR.color.green)),
                        ),
                    )
                }
        }
    }
}

@Composable
private fun PermissionsScreen(
    state: PermissionsState,
    onIntent: (PermissionsIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TitleAppBar(
                title = stringResource(LocalizationR.string.shared_with),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
            )
        },
        bottomBar = {
            if (state.showEditButton || state.showSaveButton) {
                ActionButtonAppBar(
                    state = state,
                    onIntent = onIntent,
                )
            }
        },
        floatingActionButton = {
            if (state.showAddUserButton) {
                AddFloatingActionButton(onClick = { onIntent(AddPermission) })
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
    ) { paddingValues ->
        if (state.showEmptyState) {
            EmptyPermissionsState(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
            )
        } else {
            PermissionsList(
                permissions = state.permissions,
                onPermissionClick = { onIntent(SeePermission(it)) },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
            )
        }
    }

    ProgressDialog(isVisible = state.showProgress)

    if (state.showMetadataKeyModifiedDialog && state.newMetadataKeyToTrustModel != null) {
        NewMetadataKeyTrustDialog(
            newKeyToTrustModel = state.newMetadataKeyToTrustModel,
            onTrustClick = { onIntent(TrustNewMetadataKey) },
            onDismiss = { onIntent(DismissMetadataKeyModifiedDialog) },
        )
    }

    if (state.showMetadataKeyDeletedDialog && state.trustedKeyDeletedModel != null) {
        TrustedMetadataKeyDeletedDialog(
            trustedKeyDeletedModel = state.trustedKeyDeletedModel,
            onDismiss = { onIntent(DismissMetadataKeyDeletedDialog) },
            onTrustClick = { onIntent(TrustedMetadataKeyDeleted) },
        )
    }
}

@Composable
private fun ActionButtonAppBar(
    state: PermissionsState,
    onIntent: (PermissionsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomAppBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = colorResource(CoreUiR.color.elevated_background),
    ) {
        PrimaryButton(
            text =
                if (state.showSaveButton) {
                    stringResource(LocalizationR.string.save)
                } else {
                    stringResource(LocalizationR.string.resource_permissions_edit_permissions)
                },
            onClick = { onIntent(MainButtonIntent) },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
