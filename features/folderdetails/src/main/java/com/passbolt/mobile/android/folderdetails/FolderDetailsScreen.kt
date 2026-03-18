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

package com.passbolt.mobile.android.folderdetails

import PassboltTheme
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.ui.compose.header.ItemWithHeader
import com.passbolt.mobile.android.core.ui.compose.pulltorefresh.PullToRefreshIndicatorBox
import com.passbolt.mobile.android.core.ui.compose.sharedwith.SharedWithSection
import com.passbolt.mobile.android.core.ui.compose.snackbar.ColoredSnackbarVisuals
import com.passbolt.mobile.android.core.ui.compose.text.SeparatedText
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.folderdetails.FolderDetailsIntent.GoBack
import com.passbolt.mobile.android.folderdetails.FolderDetailsIntent.GoToLocationDetails
import com.passbolt.mobile.android.folderdetails.FolderDetailsIntent.Initialize
import com.passbolt.mobile.android.folderdetails.FolderDetailsIntent.SharedWithClick
import com.passbolt.mobile.android.folderdetails.FolderDetailsSideEffect.NavigateToFolderLocation
import com.passbolt.mobile.android.folderdetails.FolderDetailsSideEffect.NavigateToFolderPermissions
import com.passbolt.mobile.android.folderdetails.FolderDetailsSideEffect.NavigateToHome
import com.passbolt.mobile.android.folderdetails.FolderDetailsSideEffect.NavigateUp
import com.passbolt.mobile.android.folderdetails.FolderDetailsSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.folderdetails.FolderDetailsSideEffect.ShowToast
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun FolderDetailsScreen(
    folderId: String,
    navigation: FolderDetailsNavigation,
    modifier: Modifier = Modifier,
    viewModel: FolderDetailsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(folderId) {
        viewModel.onIntent(Initialize(folderId))
    }

    FolderDetailsScreen(
        state = state.value,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            NavigateUp -> navigation.navigateUp()
            NavigateToHome -> navigation.navigateToHome()
            is NavigateToFolderPermissions -> navigation.navigateToFolderPermissions(sideEffect.folderId, sideEffect.mode)
            is NavigateToFolderLocation -> navigation.navigateToFolderLocation(sideEffect.folderId)
            is ShowErrorSnackbar -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        ColoredSnackbarVisuals(
                            message = getErrorMessage(context, sideEffect.type),
                            backgroundColor = Color(context.getColor(CoreUiR.color.red)),
                        ),
                    )
                }
            }
            is ShowToast ->
                Toast
                    .makeText(
                        context,
                        getToastMessage(context, sideEffect.type),
                        Toast.LENGTH_SHORT,
                    ).show()
        }
    }
}

@Composable
private fun FolderDetailsScreen(
    state: FolderDetailsState,
    onIntent: (FolderDetailsIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TitleAppBar(
                title = stringResource(LocalizationR.string.folder_details_title),
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
            PullToRefreshIndicatorBox(
                isRefreshing = state.isRefreshing,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter =
                            painterResource(
                                if (state.folder?.isShared == true) {
                                    CoreUiR.drawable.ic_filled_shared_folder_with_bg
                                } else {
                                    CoreUiR.drawable.ic_filled_folder_with_bg
                                },
                            ),
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                    )

                    Text(
                        text = state.folder?.name.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp),
                    )

                    ItemWithHeader(
                        headerText = stringResource(LocalizationR.string.location),
                        modifier = Modifier.padding(top = 16.dp),
                        onItemClick = { onIntent(GoToLocationDetails) },
                    ) {
                        SeparatedText(
                            segments = listOf(stringResource(LocalizationR.string.folder_root)) + state.locationPath,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (state.canViewPermissions) {
                        ItemWithHeader(
                            headerText = stringResource(LocalizationR.string.shared_with),
                            modifier = Modifier.padding(top = 16.dp),
                            onItemClick = { onIntent(SharedWithClick) },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SharedWithSection(
                                    permissions = state.permissions,
                                    modifier = Modifier.weight(1f),
                                )

                                Image(
                                    painter = painterResource(CoreUiR.drawable.ic_chevron_right),
                                    contentDescription = null,
                                    modifier =
                                        Modifier
                                            .size(24.dp)
                                            .padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun FolderDetailsPreview() {
    PassboltTheme {
        FolderDetailsScreen(
            state =
                FolderDetailsState(
                    locationPath = listOf("Projects", "Mobile Apps"),
                    canViewPermissions = true,
                    permissions = emptyList(),
                ),
            onIntent = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
