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

package com.passbolt.mobile.android.locationdetails

import PassboltTheme
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.common.extension.toSingleLine
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.resources.resourceicon.ResourceIconProvider
import com.passbolt.mobile.android.core.ui.pulltorefresh.PullToRefreshIndicatorBox
import com.passbolt.mobile.android.core.ui.snackbar.ColoredSnackbarVisuals
import com.passbolt.mobile.android.core.ui.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationHandler
import com.passbolt.mobile.android.locationdetails.LocationDetailsIntent.GoBack
import com.passbolt.mobile.android.locationdetails.LocationDetailsIntent.Initialize
import com.passbolt.mobile.android.locationdetails.LocationDetailsIntent.ToggleExpanded
import com.passbolt.mobile.android.locationdetails.LocationDetailsSideEffect.NavigateToHome
import com.passbolt.mobile.android.locationdetails.LocationDetailsSideEffect.NavigateUp
import com.passbolt.mobile.android.locationdetails.LocationDetailsSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.locationdetails.LocationDetailsSideEffect.ShowToast
import com.passbolt.mobile.android.locationdetails.data.ExpandableFolderTreeCreator
import com.passbolt.mobile.android.locationdetails.data.flattenTree
import com.passbolt.mobile.android.locationdetails.ui.ExpandableFolderItem
import com.passbolt.mobile.android.locationdetails.ui.LocationItem
import com.passbolt.mobile.android.ui.FolderModel
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun LocationDetailsScreen(
    locationItem: LocationItem,
    itemId: String,
    modifier: Modifier = Modifier,
    viewModel: LocationDetailsViewModel = koinViewModel(),
    navigator: AppNavigator = koinInject(),
) {
    val context = LocalContext.current
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(locationItem, itemId) {
        viewModel.onIntent(Initialize(locationItem, itemId))
    }

    AuthenticationHandler(
        onAuthenticatedIntent = viewModel::onAuthenticationIntent,
        authenticationSideEffect = viewModel.authenticationSideEffect,
    )

    LocationDetailsContent(
        state = state.value,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            NavigateUp -> navigator.navigateBack()
            NavigateToHome -> navigator.popToRoot()
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
                        getToastMessage(context, ToastType.CONTENT_NOT_AVAILABLE),
                        Toast.LENGTH_SHORT,
                    ).show()
        }
    }
}

@Composable
private fun LocationDetailsContent(
    state: LocationDetailsState,
    onIntent: (LocationDetailsIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    resourceIconProvider: ResourceIconProvider = koinInject(),
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TitleAppBar(
                title = stringResource(LocalizationR.string.location),
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
                    val context = LocalContext.current
                    var itemIcon by remember { mutableStateOf<Drawable?>(null) }

                    LaunchedEffect(state.itemName) {
                        itemIcon =
                            if (state.resource != null) {
                                resourceIconProvider.getResourceIcon(context, state.resource)
                            } else {
                                ContextCompat.getDrawable(
                                    context,
                                    if (state.isSharedFolder) {
                                        CoreUiR.drawable.ic_filled_shared_folder_with_bg
                                    } else {
                                        CoreUiR.drawable.ic_filled_folder_with_bg
                                    },
                                )
                            }
                    }

                    itemIcon?.let { drawable ->
                        Image(
                            painter = BitmapPainter(drawable.toBitmap().asImageBitmap()),
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                        )
                    }

                    Text(
                        text = state.itemName.toSingleLine(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp),
                    )

                    if (state.folderTree != null && state.folderTree.rootNodes.isNotEmpty()) {
                        Text(
                            text = stringResource(LocalizationR.string.location),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp, start = 16.dp),
                        )

                        LazyColumn(modifier = Modifier.padding(top = 24.dp)) {
                            items(
                                items = flattenTree(state.folderTree.rootNodes, state.expandedItemIds),
                                key = { it.id },
                            ) { node ->
                                ExpandableFolderItem(
                                    node = node,
                                    isExpanded = state.expandedItemIds.contains(node.id),
                                    onToggleExpansion = { onIntent(ToggleExpanded(node.id)) },
                                    modifier = Modifier.fillMaxWidth(),
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
private fun LocationDetailsPreview(expandableFolderTreeCreator: ExpandableFolderTreeCreator = koinInject()) {
    PassboltTheme {
        val folderTree =
            expandableFolderTreeCreator.create(
                listOf(
                    FolderModel(
                        folderId = "1",
                        parentFolderId = null,
                        name = "Projects",
                        isShared = false,
                        permission = ResourcePermission.OWNER,
                    ),
                    FolderModel(
                        folderId = "2",
                        parentFolderId = "1",
                        name = "Mobile Apps",
                        isShared = true,
                        permission = ResourcePermission.OWNER,
                    ),
                ),
            )

        LocationDetailsContent(
            state =
                LocationDetailsState(
                    itemName = "Item",
                    isSharedFolder = false,
                    resource = null,
                    parentFolders =
                        listOf(
                            FolderModel(
                                folderId = "1",
                                parentFolderId = null,
                                name = "Projects",
                                isShared = false,
                                permission = ResourcePermission.OWNER,
                            ),
                            FolderModel(
                                folderId = "2",
                                parentFolderId = "1",
                                name = "Mobile Apps",
                                isShared = true,
                                permission = ResourcePermission.OWNER,
                            ),
                        ),
                    folderTree = folderTree,
                    expandedItemIds = setOf("root_folder_id", "1"),
                ),
            onIntent = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
