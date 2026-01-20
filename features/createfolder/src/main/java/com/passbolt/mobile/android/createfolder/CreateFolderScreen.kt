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

package com.passbolt.mobile.android.createfolder

import PassboltTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.ui.compose.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.compose.header.ItemWithHeader
import com.passbolt.mobile.android.core.ui.compose.progressdialog.ProgressDialog
import com.passbolt.mobile.android.core.ui.compose.sharedwith.SharedWithSection
import com.passbolt.mobile.android.core.ui.compose.snackbar.ColoredSnackbarVisuals
import com.passbolt.mobile.android.core.ui.compose.text.SeparatedText
import com.passbolt.mobile.android.core.ui.compose.text.TextInput
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Default
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Error
import com.passbolt.mobile.android.createfolder.CreateFolderIntent.FolderNameChanged
import com.passbolt.mobile.android.createfolder.CreateFolderIntent.GoBack
import com.passbolt.mobile.android.createfolder.CreateFolderIntent.Initialize
import com.passbolt.mobile.android.createfolder.CreateFolderIntent.Save
import com.passbolt.mobile.android.createfolder.CreateFolderSideEffect.FolderCreated
import com.passbolt.mobile.android.createfolder.CreateFolderSideEffect.NavigateUp
import com.passbolt.mobile.android.createfolder.CreateFolderSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationHandler
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun CreateFolderScreen(
    parentFolderId: String?,
    navigation: CreateFolderNavigation,
    modifier: Modifier = Modifier,
    viewModel: CreateFolderViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(parentFolderId) {
        viewModel.onIntent(Initialize(parentFolderId))
    }

    AuthenticationHandler(
        onAuthenticatedIntent = viewModel::onAuthenticationIntent,
        authenticationSideEffect = viewModel.authenticationSideEffect,
    )

    CreateFolderScreen(
        state = state.value,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            NavigateUp -> navigation.navigateUp()
            is FolderCreated -> navigation.folderCreated(sideEffect.folderName)
            is ShowErrorSnackbar -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        ColoredSnackbarVisuals(
                            message = getErrorMessage(context, sideEffect.type, sideEffect.message),
                            backgroundColor = Color(context.getColor(CoreUiR.color.red)),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateFolderScreen(
    state: CreateFolderState,
    onIntent: (CreateFolderIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TitleAppBar(
                title = stringResource(LocalizationR.string.create_folder_title),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
            )
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
        content = { paddingValues ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                ) {
                    TextInput(
                        title = stringResource(LocalizationR.string.create_folder_name),
                        text = state.folderName,
                        state =
                            if (state.folderNameValidationErrors.isEmpty()) {
                                Default
                            } else {
                                Error(
                                    getFolderNameErrorMessage(
                                        LocalContext.current,
                                        state.folderNameValidationErrors,
                                    ),
                                )
                            },
                        onTextChange = { onIntent(FolderNameChanged(it)) },
                        isRequired = true,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = CreateFolderScreenTestTags.NAME_INPUT,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ItemWithHeader(
                        headerText = stringResource(LocalizationR.string.location),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SeparatedText(
                            segments = listOf(stringResource(LocalizationR.string.folder_root)) + state.locationPath,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ItemWithHeader(
                        headerText = stringResource(LocalizationR.string.shared_with),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SharedWithSection(
                                permissions = state.permissions,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    PrimaryButton(
                        text = stringResource(LocalizationR.string.save),
                        onClick = { onIntent(Save) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                ProgressDialog(state.isLoading)
            }
        },
    )
}

object CreateFolderScreenTestTags {
    const val NAME_INPUT: String = "create_folder_name_input"
}

@Preview(showBackground = true)
@Composable
private fun CreateFolderScreenPreview() {
    PassboltTheme {
        CreateFolderScreen(
            state =
                CreateFolderState(
                    folderName = "My Folder",
                    locationPath = listOf("Projects", "Mobile Apps"),
                    permissions = emptyList(),
                ),
            onIntent = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
