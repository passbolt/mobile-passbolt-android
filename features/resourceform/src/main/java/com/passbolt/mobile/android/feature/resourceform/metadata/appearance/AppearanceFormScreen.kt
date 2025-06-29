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

package com.passbolt.mobile.android.feature.resourceform.metadata.appearance

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.localization.R
import com.passbolt.mobile.android.core.resources.resourceicon.BackgroundColorIconProvider
import com.passbolt.mobile.android.core.resources.resourceicon.ResourceIconProvider
import com.passbolt.mobile.android.core.ui.compose.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.compose.switch.TextSwitch
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.resourceform.metadata.appearance.AppearanceFormIntent.ApplyChanges
import com.passbolt.mobile.android.feature.resourceform.metadata.appearance.AppearanceFormIntent.GoBack
import com.passbolt.mobile.android.feature.resourceform.metadata.appearance.AppearanceFormIntent.SetCustomIconBackgroundColor
import com.passbolt.mobile.android.feature.resourceform.metadata.appearance.AppearanceFormIntent.SetKeepassIcon
import com.passbolt.mobile.android.feature.resourceform.metadata.appearance.AppearanceFormSideEffect.ApplyAndGoBack
import com.passbolt.mobile.android.feature.resourceform.metadata.appearance.AppearanceFormSideEffect.NavigateUp
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.ResourceAppearanceModel.Companion.DEFAULT_BACKGROUND_COLOR_HEX_STRING
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.ResourceFormMode.Create
import com.passbolt.mobile.android.ui.ResourceFormMode.Edit
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Composable
internal fun AppearanceFormScreen(
    navigation: AppearanceFormNavigation,
    modifier: Modifier = Modifier,
    viewModel: AppearanceFormViewModel = koinViewModel(),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()

    AppearanceFormScreen(
        modifier = modifier,
        state = state.value,
        onIntent = viewModel::onIntent,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            is ApplyAndGoBack -> navigation.navigateBackWithResult(it.model)
            NavigateUp -> navigation.navigateUp()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceFormScreen(
    onIntent: (AppearanceFormIntent) -> Unit,
    state: AppearanceFormState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(96.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    PrimaryButton(
                        text = stringResource(LocalizationR.string.apply),
                        onClick = { onIntent(ApplyChanges) },
                    )
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
        ) {
            TitleAppBar(
                title = getScreenTitle(context, state.resourceFormMode),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(LocalizationR.string.resource_form_appearance), style = MaterialTheme.typography.titleMedium)

            SelectColorSection(state, onIntent)

            SelectIconSection(state, onIntent)
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SelectIconSection(
    state: AppearanceFormState,
    onIntent: (AppearanceFormIntent) -> Unit,
    resourceIconProvider: ResourceIconProvider = koinInject(),
) {
    val context = LocalContext.current

    Column {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(LocalizationR.string.resource_form_appearance_select_icon),
            style = MaterialTheme.typography.titleSmall,
        )

        Spacer(modifier = Modifier.height(16.dp))
        TextSwitch(
            stringResource(R.string.resource_form_appearance_default_icon),
            isChecked = state.isDefaultIconChecked,
            onCheckedChange = { onIntent(AppearanceFormIntent.ToggleDefaultIcon) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            state.keepassIconValues.forEach { keepassIconValue ->
                val isSelected = state.keepassIconValue == keepassIconValue
                val drawable =
                    remember(keepassIconValue, isSelected) {
                        resourceIconProvider.getKeypassIcon(
                            context = context,
                            keepassIconValue = keepassIconValue,
                            backgroundHexString = DEFAULT_BACKGROUND_COLOR_HEX_STRING,
                            withSelectedBorder = isSelected,
                        )
                    }
                val painter = rememberDrawablePainter(drawable)

                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable {
                                onIntent(SetKeepassIcon(keepassIconValue))
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SelectColorSection(
    state: AppearanceFormState,
    onIntent: (AppearanceFormIntent) -> Unit,
    backgroundIconProvider: BackgroundColorIconProvider = koinInject(),
) {
    val context = LocalContext.current

    Column {
        Spacer(modifier = Modifier.height(24.dp))
        Text(stringResource(LocalizationR.string.resource_form_appearance_select_color), style = MaterialTheme.typography.titleSmall)

        Spacer(modifier = Modifier.height(16.dp))
        TextSwitch(
            stringResource(R.string.resource_form_appearance_default_color),
            isChecked = state.isDefaultColorChecked,
            onCheckedChange = { onIntent(AppearanceFormIntent.ToggleDefaultColor) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            state.backgroundColorsHexList.forEach { colorHex ->
                val isSelected = state.iconBackgroundColorHex == colorHex
                val drawable =
                    remember(colorHex, isSelected) {
                        backgroundIconProvider.getBackgroundColorIcon(
                            context = context,
                            backgroundColorHex = colorHex,
                            isSelected = isSelected,
                        )
                    }
                val painter = rememberDrawablePainter(drawable)

                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable {
                                onIntent(SetCustomIconBackgroundColor(colorHex))
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(40.dp),
                    )
                }
            }
        }
    }
}

private fun getScreenTitle(
    context: Context,
    resourceFormMode: ResourceFormMode?,
): String =
    when (resourceFormMode) {
        is Create -> context.getString(LocalizationR.string.resource_form_create_resource_appearance)
        is Edit -> context.getString(LocalizationR.string.resource_form_edit_resource, resourceFormMode.resourceName)
        null -> "" // Until navigation data is processed use empty title
    }

@Preview(showBackground = true)
@Composable
private fun TermsAndLicensesPreview() {
    AppearanceFormScreen(
        onIntent = {},
        state =
            AppearanceFormState(
                Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                ),
            ),
        modifier = Modifier,
    )
}
