/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
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

package com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris

import PassboltTheme
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.passbolt.mobile.android.core.ui.compose.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.compose.button.SecondaryButton
import com.passbolt.mobile.android.core.ui.compose.button.SecondaryIconButton
import com.passbolt.mobile.android.core.ui.compose.snackbar.ColoredSnackbarVisuals
import com.passbolt.mobile.android.core.ui.compose.text.TextInput
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Default
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Error
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormIntent.AddAdditionalUri
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormIntent.AdditionalUriChanged
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormIntent.ApplyChanges
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormIntent.GoBack
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormIntent.MainUriChanged
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormIntent.RemoveAdditionalUri
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormSideEffect.ApplyAndGoBack
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormSideEffect.NavigateUp
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormSideEffect.ScrollToItem
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.ResourceFormMode.Create
import com.passbolt.mobile.android.ui.ResourceFormMode.Edit
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.util.UUID
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun AdditionalUrisFormScreen(
    navigation: AdditionalUrisFormNavigation,
    modifier: Modifier = Modifier,
    viewModel: AdditionalUrisFormViewModel = koinViewModel(),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    AdditionalUrisFormScreen(
        modifier = modifier,
        state = state.value,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        lazyListState = lazyListState,
    )

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is ApplyAndGoBack -> navigation.navigateBackWithResult(sideEffect.model)
            NavigateUp -> navigation.navigateUp()
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
            is ScrollToItem -> {
                coroutineScope.launch {
                    val lazyIndex =
                        if (sideEffect.index == 0 && state.value.mainUriError != null) {
                            SECTION_HEADER_INDEX
                        } else {
                            FIRST_URI_ITEM_INDEX + sideEffect.index
                        }
                    lazyListState.animateScrollToItem(lazyIndex)
                }
            }
        }
    }
}

@Composable
private fun AdditionalUrisFormScreen(
    state: AdditionalUrisFormState,
    onIntent: (AdditionalUrisFormIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val sectionColor = colorResource(CoreUiR.color.section_background)
    val additionalUriEntries = state.additionalUris.entries.toList()

    Scaffold(
        modifier = modifier,
        topBar = {
            TitleAppBar(
                title = getScreenTitle(LocalContext.current, state.resourceFormMode),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                PrimaryButton(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(LocalizationR.string.apply),
                    onClick = { onIntent(ApplyChanges) },
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
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            state = lazyListState,
        ) {
            item(key = TITLE_KEY) {
                Text(
                    text = stringResource(LocalizationR.string.resource_form_additional_uris),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // TODO use Section composable after resource details PR that adds it is merged
            item(key = SECTION_HEADER_KEY) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(sectionColor)
                            .padding(start = 12.dp, end = 12.dp, top = 12.dp),
                ) {
                    TextInput(
                        title = stringResource(LocalizationR.string.resource_form_main_uri),
                        hint = stringResource(LocalizationR.string.resource_form_enter_uri),
                        text = state.mainUri,
                        onTextChange = { onIntent(MainUriChanged(it)) },
                        state =
                            state.mainUriError?.let {
                                Error(stringResource(LocalizationR.string.validation_max_length, it.toInt()))
                            } ?: Default,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                        text = stringResource(LocalizationR.string.resource_form_additional_uris),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            itemsIndexed(
                items = additionalUriEntries,
                key = { _, (uriId, _) -> uriId },
            ) { index, (uriId, itemState) ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(sectionColor)
                            .padding(horizontal = 12.dp),
                ) {
                    AdditionalUriRow(
                        itemState = itemState,
                        onUriChange = { uri -> onIntent(AdditionalUriChanged(uriId, uri)) },
                        onRemoveClick = { onIntent(RemoveAdditionalUri(uriId)) },
                    )
                    if (index < additionalUriEntries.size - 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            item(key = SECTION_FOOTER_KEY) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                            .background(sectionColor)
                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    SecondaryButton(
                        onClick = { onIntent(AddAdditionalUri) },
                        text = stringResource(LocalizationR.string.resource_form_add_uri),
                        icon = painterResource(CoreUiR.drawable.ic_plus),
                    )
                }
            }
        }
    }
}

@Composable
private fun AdditionalUriRow(
    itemState: AdditionalUriItemState,
    onUriChange: (String) -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        TextInput(
            title = "",
            hint = stringResource(LocalizationR.string.resource_form_enter_uri),
            text = itemState.uri,
            onTextChange = onUriChange,
            state =
                itemState.error?.let {
                    Error(stringResource(LocalizationR.string.validation_max_length, it.toInt()))
                } ?: Default,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(8.dp))

        SecondaryIconButton(
            onClick = onRemoveClick,
            icon = painterResource(CoreUiR.drawable.ic_trash),
            modifier = Modifier.size(56.dp),
            iconModifier = Modifier.size(22.dp),
        )
    }
}

private fun getScreenTitle(
    context: Context,
    resourceFormMode: ResourceFormMode?,
): String =
    when (resourceFormMode) {
        is Create -> context.getString(LocalizationR.string.resource_form_create_additional_uris)
        is Edit -> context.getString(LocalizationR.string.resource_form_edit_resource, resourceFormMode.resourceName)
        null -> ""
    }

private const val SECTION_HEADER_INDEX = 1
private const val FIRST_URI_ITEM_INDEX = 2
private const val TITLE_KEY = "title"
private const val SECTION_HEADER_KEY = "section_header"
private const val SECTION_FOOTER_KEY = "section_footer"

@Preview(showBackground = true)
@Composable
private fun AdditionalUrisFormScreenPreview() {
    PassboltTheme {
        AdditionalUrisFormScreen(
            onIntent = {},
            snackbarHostState = remember { SnackbarHostState() },
            lazyListState = rememberLazyListState(),
            state =
                AdditionalUrisFormState(
                    resourceFormMode =
                        Create(
                            leadingContentType = LeadingContentType.PASSWORD,
                            parentFolderId = null,
                        ),
                    mainUri = "https://example.com",
                    additionalUris =
                        linkedMapOf(
                            UUID.randomUUID() to AdditionalUriItemState(uri = "https://example.org"),
                            UUID.randomUUID() to AdditionalUriItemState(uri = "https://example1.org"),
                            UUID.randomUUID() to AdditionalUriItemState(uri = "https://example2.org"),
                        ),
                ),
        )
    }
}
