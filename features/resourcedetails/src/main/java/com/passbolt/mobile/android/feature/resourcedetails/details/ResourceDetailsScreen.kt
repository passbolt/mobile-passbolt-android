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

package com.passbolt.mobile.android.feature.resourcedetails.details

import PassboltTheme
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.clipboard.ClipboardAccess
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.keys.LocationDetailsNavigationKey.LocationDetails
import com.passbolt.mobile.android.core.navigation.compose.keys.LocationDetailsNavigationKey.LocationItem
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.Permissions
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.MainResourceForm
import com.passbolt.mobile.android.core.navigation.compose.keys.TagsDetailsNavigationKey.ResourceTags
import com.passbolt.mobile.android.core.navigation.compose.results.NavigationResultEventBus
import com.passbolt.mobile.android.core.navigation.compose.results.ResourceDetailsCompleteResult
import com.passbolt.mobile.android.core.resources.resourceicon.ResourceIconProvider
import com.passbolt.mobile.android.core.ui.dialogs.ConfirmResourceDeleteAlertDialog
import com.passbolt.mobile.android.core.ui.progressdialog.ProgressDialog
import com.passbolt.mobile.android.core.ui.pulltorefresh.PullToRefreshIndicatorBox
import com.passbolt.mobile.android.core.ui.snackbar.ColoredSnackbarVisuals
import com.passbolt.mobile.android.core.ui.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationHandler
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.CloseDeleteConfirmationDialog
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.CloseMoreMenu
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.ConfirmDeleteResource
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.CopyMetadataDescription
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.CopyNote
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.CopyPassword
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.CopyUrl
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.CopyUsername
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.DeleteClick
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.Dispose
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.Edit
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.EditPermissions
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.GoBack
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.Initialize
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.LaunchWebsite
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.OpenMoreMenu
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.ToggleFavourite
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.ViewPermissions
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsSideEffect.AddToClipboard
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsSideEffect.CloseWithDeleteSuccess
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsSideEffect.NavigateToEditResource
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsSideEffect.NavigateToResourceLocation
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsSideEffect.NavigateToResourcePermissions
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsSideEffect.NavigateToResourceTags
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsSideEffect.OpenWebsite
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsSideEffect.SetResourceEditedResult
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsSideEffect.ShowSuccessSnackbar
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsSideEffect.ShowToast
import com.passbolt.mobile.android.feature.resourcedetails.details.ui.CustomFieldsSection
import com.passbolt.mobile.android.feature.resourcedetails.details.ui.MetadataSection
import com.passbolt.mobile.android.feature.resourcedetails.details.ui.NoteSection
import com.passbolt.mobile.android.feature.resourcedetails.details.ui.PasswordSection
import com.passbolt.mobile.android.feature.resourcedetails.details.ui.ResourceHeader
import com.passbolt.mobile.android.feature.resourcedetails.details.ui.SharedWithSection
import com.passbolt.mobile.android.feature.resourcedetails.details.ui.TotpSection
import com.passbolt.mobile.android.resourcemoremenu.ResourceMoreMenuBottomSheet
import com.passbolt.mobile.android.ui.PermissionsItem
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.isExpired
import com.passbolt.mobile.android.ui.isFavourite
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
fun ResourceDetailsScreen(
    resourceModel: ResourceModel,
    modifier: Modifier = Modifier,
    viewModel: ResourceDetailsViewModel = koinViewModel(),
    clipboardAccess: ClipboardAccess = koinInject(),
    navigator: AppNavigator = koinInject(),
    resourceIconProvider: ResourceIconProvider = koinInject(),
) {
    val context = LocalContext.current
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val resultBus = NavigationResultEventBus.current

    var resourceIcon by remember { mutableStateOf<Drawable?>(null) }
    val currentResourceModel = state.value.resourceData.resourceModel ?: resourceModel
    LaunchedEffect(currentResourceModel) {
        resourceIcon = resourceIconProvider.getResourceIcon(context, currentResourceModel)
    }

    LaunchedEffect(resourceModel.resourceId) {
        viewModel.onIntent(Initialize(resourceModel))
    }

    AuthenticationHandler(
        onAuthenticatedIntent = viewModel::onAuthenticationIntent,
        authenticationSideEffect = viewModel.authenticationSideEffect,
    )

    ResourceDetailsScreen(
        state = state.value,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        resourceIcon = resourceIcon,
        modifier = modifier,
    )

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onIntent(Dispose)
        }
    }

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            NavigateBack -> navigator.navigateBack()
            is NavigateToEditResource ->
                navigator.navigateToKey(
                    MainResourceForm(
                        ResourceFormMode.Edit(
                            resourceId = sideEffect.resourceModel.resourceId,
                            resourceName = sideEffect.resourceModel.metadataJsonModel.name,
                        ),
                    ),
                )
            is NavigateToResourcePermissions ->
                navigator.navigateToKey(
                    Permissions(sideEffect.resourceId, sideEffect.mode, PermissionsItem.RESOURCE),
                )
            is NavigateToResourceTags -> navigator.navigateToKey(ResourceTags(sideEffect.resourceId))
            is NavigateToResourceLocation ->
                navigator.navigateToKey(
                    LocationDetails(LocationItem.RESOURCE, sideEffect.resourceId),
                )
            is OpenWebsite -> navigator.openExternalWebsite(context, sideEffect.url)
            is AddToClipboard ->
                clipboardAccess.setPrimaryClip(
                    context = context,
                    label = sideEffect.label,
                    value = sideEffect.value,
                    isSensitive = sideEffect.isSecret,
                )
            is CloseWithDeleteSuccess -> {
                resultBus.sendResult(
                    result =
                        ResourceDetailsCompleteResult(
                            resourceEdited = false,
                            resourceDeleted = true,
                            resourceName = sideEffect.resourceName,
                        ),
                )
                navigator.navigateBack()
            }
            is SetResourceEditedResult -> {
                resultBus.sendResult(
                    result =
                        ResourceDetailsCompleteResult(
                            resourceEdited = true,
                            resourceDeleted = false,
                            resourceName = sideEffect.resourceName,
                        ),
                )
            }
            is ShowSuccessSnackbar -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        ColoredSnackbarVisuals(
                            message = getSuccessSnackbarMessage(context, sideEffect.type),
                            backgroundColor = Color(context.getColor(CoreUiR.color.green)),
                        ),
                    )
                }
            }
            is ShowErrorSnackbar -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        ColoredSnackbarVisuals(
                            message = getErrorSnackbarMessage(context, sideEffect.type),
                            backgroundColor = Color(context.getColor(CoreUiR.color.red)),
                        ),
                    )
                }
            }
            is ShowToast -> {
                Toast
                    .makeText(
                        context,
                        getToastMessage(context, sideEffect.type),
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        }
    }
}

@Composable
private fun ResourceDetailsScreen(
    state: ResourceDetailsState,
    onIntent: (ResourceDetailsIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    resourceIcon: Drawable?,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TitleAppBar(
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
                actions = {
                    IconButton(onClick = { onIntent(OpenMoreMenu) }) {
                        Icon(
                            painter = painterResource(CoreUiR.drawable.ic_more),
                            contentDescription = null,
                        )
                    }
                },
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
                ResourceDetailsContent(
                    state = state,
                    onIntent = onIntent,
                    resourceIcon = resourceIcon,
                )

                ConfirmResourceDeleteAlertDialog(
                    isVisible = state.showDeleteResourceConfirmationDialog,
                    onConfirm = { onIntent(ConfirmDeleteResource) },
                    onDismiss = { onIntent(CloseDeleteConfirmationDialog) },
                )

                ProgressDialog(state.isLoading)

                if (state.showMoreMenu && state.resourceData.resourceModel != null) {
                    val resource = state.resourceData.resourceModel!!
                    ResourceMoreMenuBottomSheet(
                        resourceId = resource.resourceId,
                        resourceName = resource.metadataJsonModel.name,
                        onDismissRequest = { onIntent(CloseMoreMenu) },
                        onCopyPassword = { onIntent(CopyPassword) },
                        onCopyMetadataDescription = { onIntent(CopyMetadataDescription) },
                        onCopyNote = { onIntent(CopyNote) },
                        onCopyUrl = { onIntent(CopyUrl) },
                        onCopyUsername = { onIntent(CopyUsername) },
                        onLaunchWebsite = { onIntent(LaunchWebsite) },
                        onDelete = { onIntent(DeleteClick) },
                        onEdit = { onIntent(Edit) },
                        onShare = { onIntent(EditPermissions) },
                        onToggleFavourite = { onIntent(ToggleFavourite(it)) },
                    )
                }
            }
        },
    )
}

@Composable
private fun ResourceDetailsContent(
    state: ResourceDetailsState,
    onIntent: (ResourceDetailsIntent) -> Unit,
    resourceIcon: Drawable?,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ResourceHeader(
            title =
                state.resourceData.resourceModel
                    ?.metadataJsonModel
                    ?.name
                    .orEmpty(),
            isExpired = state.resourceData.resourceModel?.isExpired() == true,
            isFavourite = state.resourceData.resourceModel?.isFavourite() == true,
            resourceIcon = resourceIcon,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
        )

        if (state.showPasswordSection) {
            PasswordSection(
                username =
                    state.resourceData.resourceModel
                        ?.metadataJsonModel
                        ?.username
                        .orEmpty(),
                mainUri = state.metadataData.mainUri,
                password = state.passwordData.password,
                isPasswordUnmasked = state.passwordData.isPasswordVisible,
                showPassword = state.passwordData.showPasswordItem,
                showPasswordEyeIcon = state.passwordData.showPasswordEyeIcon,
                onIntent = onIntent,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        if (state.totpData.showTotpSection) {
            TotpSection(
                otpModel = state.totpData.totpModel,
                onIntent = onIntent,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        if (state.customFieldsData.showCustomFieldsSection) {
            CustomFieldsSection(
                customFields = state.customFieldsData.customFields,
                visibleCustomFields = state.customFieldsData.visibleCustomFields,
                onIntent = onIntent,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        if (state.noteData.showNoteSection) {
            NoteSection(
                note = state.noteData.note,
                isNoteVisible = state.noteData.isNoteVisible,
                onIntent = onIntent,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        if (state.showMetadataSection) {
            MetadataSection(
                showMetadataDescriptionSection = state.metadataData.showMetadataDescriptionItem,
                canViewLocation = state.metadataData.canViewLocation,
                canViewTags = state.metadataData.canViewTags,
                metadataDescription =
                    state.resourceData.resourceModel
                        ?.metadataJsonModel
                        ?.description
                        .orEmpty(),
                additionalUris = state.metadataData.additionalUris,
                tags = state.metadataData.tags,
                locationPath = state.metadataData.locationPath,
                expiry = state.resourceData.resourceModel?.expiry,
                onIntent = onIntent,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        if (state.sharedWithData.canViewPermissions) {
            SharedWithSection(
                permissions = state.sharedWithData.permissions,
                onShareWithClick = { onIntent(ViewPermissions) },
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResourceDetailsScreenPreview() {
    PassboltTheme {
        ResourceDetailsScreen(
            state =
                ResourceDetailsState(
                    passwordData =
                        PasswordData(
                            showPasswordItem = true,
                            showPasswordEyeIcon = true,
                            password = "secretPassword123",
                        ),
                    metadataData =
                        MetadataData(
                            mainUri = "https://example.com",
                            canViewTags = true,
                            tags = listOf("work", "important"),
                            canViewLocation = true,
                            locationPath = listOf("Projects", "Mobile"),
                        ),
                    sharedWithData =
                        SharedWithData(
                            canViewPermissions = true,
                        ),
                ),
            onIntent = {},
            snackbarHostState = remember { SnackbarHostState() },
            resourceIcon = null,
        )
    }
}
