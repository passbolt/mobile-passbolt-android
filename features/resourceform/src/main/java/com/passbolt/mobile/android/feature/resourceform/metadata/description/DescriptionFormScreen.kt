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

package com.passbolt.mobile.android.feature.resourceform.metadata.description

import PassboltTheme
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.results.NavigationResultEventBus
import com.passbolt.mobile.android.core.ui.compose.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.compose.section.Section
import com.passbolt.mobile.android.core.ui.compose.text.TextInput
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.resourceform.metadata.description.DescriptionFormIntent.ApplyChanges
import com.passbolt.mobile.android.feature.resourceform.metadata.description.DescriptionFormIntent.DescriptionChanged
import com.passbolt.mobile.android.feature.resourceform.metadata.description.DescriptionFormIntent.GoBack
import com.passbolt.mobile.android.feature.resourceform.metadata.description.DescriptionFormSideEffect.ApplyAndGoBack
import com.passbolt.mobile.android.feature.resourceform.metadata.description.DescriptionFormSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.resourceform.navigation.DescriptionFormResult
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.ResourceFormMode.Create
import com.passbolt.mobile.android.ui.ResourceFormMode.Edit
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Composable
internal fun DescriptionFormScreen(
    mode: ResourceFormMode,
    metadataDescription: String,
    modifier: Modifier = Modifier,
    navigator: AppNavigator = koinInject(),
    viewModel: DescriptionFormViewModel = koinViewModel(parameters = { parametersOf(mode, metadataDescription) }),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val resultBus = NavigationResultEventBus.current

    DescriptionFormScreen(
        modifier = modifier,
        state = state.value,
        onIntent = viewModel::onIntent,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            is ApplyAndGoBack -> {
                resultBus.sendResult(result = DescriptionFormResult(it.metadataDescription))
                navigator.navigateBack()
            }
            NavigateBack -> navigator.navigateBack()
        }
    }
}

@Composable
private fun DescriptionFormScreen(
    state: DescriptionFormState,
    onIntent: (DescriptionFormIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
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
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
        ) {
            Section(title = stringResource(LocalizationR.string.resource_form_description)) {
                Column {
                    TextInput(
                        title = stringResource(LocalizationR.string.resource_form_metadata_description),
                        hint = stringResource(LocalizationR.string.resource_form_enter_description),
                        text = state.metadataDescription,
                        onTextChange = { onIntent(DescriptionChanged(it)) },
                        minLines = 3,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(LocalizationR.string.resource_form_metadata_description_info),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun getScreenTitle(
    context: Context,
    resourceFormMode: ResourceFormMode,
): String =
    when (resourceFormMode) {
        is Create -> context.getString(LocalizationR.string.resource_form_create_metadata_description)
        is Edit -> context.getString(LocalizationR.string.resource_form_edit_resource, resourceFormMode.resourceName)
    }

@Preview(showBackground = true)
@Composable
private fun DescriptionFormScreenPreview() {
    PassboltTheme {
        DescriptionFormScreen(
            onIntent = {},
            state =
                DescriptionFormState(
                    resourceFormMode =
                        Create(
                            leadingContentType = LeadingContentType.PASSWORD,
                            parentFolderId = null,
                        ),
                    metadataDescription = "Sample description",
                ),
        )
    }
}
