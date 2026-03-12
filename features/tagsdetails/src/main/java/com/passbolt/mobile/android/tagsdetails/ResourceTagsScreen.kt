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

package com.passbolt.mobile.android.tagsdetails

import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.resources.resourceicon.ResourceIconProvider
import com.passbolt.mobile.android.core.ui.pulltorefresh.PullToRefreshIndicatorBox
import com.passbolt.mobile.android.core.ui.snackbar.ColoredSnackbarVisuals
import com.passbolt.mobile.android.core.ui.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationHandler
import com.passbolt.mobile.android.tagsdetails.ResourceTagsIntent.GoBack
import com.passbolt.mobile.android.tagsdetails.ResourceTagsIntent.Initialize
import com.passbolt.mobile.android.tagsdetails.ResourceTagsSideEffect.NavigateBack
import com.passbolt.mobile.android.tagsdetails.ResourceTagsSideEffect.NavigateToHome
import com.passbolt.mobile.android.tagsdetails.ResourceTagsSideEffect.ShowContentNotAvailable
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.isFavourite
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun ResourceTagsScreen(
    resourceId: String,
    modifier: Modifier = Modifier,
    navigator: AppNavigator = koinInject(),
    viewModel: ResourceTagsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(resourceId) {
        viewModel.onIntent(Initialize(resourceId))
    }

    AuthenticationHandler(
        onAuthenticatedIntent = viewModel::onAuthenticationIntent,
        authenticationSideEffect = viewModel.authenticationSideEffect,
    )

    ResourceTagsContent(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            NavigateBack -> navigator.navigateBack()
            NavigateToHome -> navigator.popToRoot()
            ShowContentNotAvailable -> {
                Toast
                    .makeText(
                        context,
                        getToastMessage(context, ToastType.CONTENT_NOT_AVAILABLE),
                        Toast.LENGTH_SHORT,
                    ).show()
            }
            is ResourceTagsSideEffect.ShowErrorSnackbar -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        ColoredSnackbarVisuals(
                            message = getErrorMessage(context, sideEffect.type),
                            backgroundColor = Color(context.getColor(CoreUiR.color.red)),
                        ),
                    )
                }
            }
            is ResourceTagsSideEffect.ShowToast -> {
                Toast.makeText(context, getToastMessage(context, sideEffect.type), Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResourceTagsContent(
    state: ResourceTagsState,
    onIntent: (ResourceTagsIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val pullState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(state.isRefreshing) {
        scope.launch {
            if (state.isRefreshing) {
                pullState.animateToThreshold()
            } else {
                pullState.animateToHidden()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TitleAppBar(
                title = stringResource(LocalizationR.string.resource_tags_title),
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
                Column(modifier = Modifier.fillMaxSize()) {
                    ResourceHeader(
                        resourceModel = state.resourceModel,
                        modifier = Modifier.padding(16.dp),
                    )

                    Text(
                        text = stringResource(LocalizationR.string.resource_tags_section),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(state.tags) { tag ->
                            TagItem(tag = tag)
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun ResourceHeader(
    resourceModel: ResourceModel?,
    modifier: Modifier = Modifier,
    resourceIconProvider: ResourceIconProvider = koinInject(),
) {
    val context = LocalContext.current
    var resourceIcon by remember { mutableStateOf<Drawable?>(null) }

    LaunchedEffect(resourceModel) {
        resourceIcon =
            resourceModel?.let {
                resourceIconProvider.getResourceIcon(context, resourceModel)
            }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(84.dp), contentAlignment = Alignment.Center) {
            if (resourceIcon != null) {
                Image(
                    painter = rememberDrawablePainter(resourceIcon),
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                )
            }

            if (resourceModel?.isFavourite() == true) {
                Image(
                    painter = painterResource(CoreUiR.drawable.ic_favourite_star_filled),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(24.dp)
                            .align(Alignment.TopEnd),
                )
            }
        }

        Text(
            text = resourceModel?.metadataJsonModel?.name.orEmpty(),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
        )
    }
}
