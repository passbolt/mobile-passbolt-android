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

package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.customfields

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.ui.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.customfields.CustomFieldsFormIntent.GoBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.customfields.CustomFieldsFormSideEffect.NavigateUp
import com.passbolt.mobile.android.ui.CustomFieldsUiModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun CustomFieldsFormScreen(
    customFieldsUiModel: CustomFieldsUiModel,
    modifier: Modifier = Modifier,
    navigator: AppNavigator = koinInject(),
    viewModel: CustomFieldsFormViewModel = koinViewModel(parameters = { parametersOf(customFieldsUiModel) }),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()

    CustomFieldsFormScreen(
        modifier = modifier,
        state = state.value,
        onIntent = viewModel::onIntent,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            NavigateUp -> navigator.navigateBack()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomFieldsFormScreen(
    onIntent: (CustomFieldsFormIntent) -> Unit,
    state: CustomFieldsFormState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        TitleAppBar(
            title = context.getString(LocalizationR.string.resource_form_create_resource_custom_fields),
            navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.customFields.isNotEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color = colorResource(CoreUiR.color.section_background),
                        ).padding(16.dp),
            ) {
                Column {
                    state.customFields.forEach { customField ->
                        Text(
                            text = customField.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(text = customField.value, color = MaterialTheme.colorScheme.onBackground)

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        } else {
            Text(
                text = stringResource(LocalizationR.string.resource_form_no_custom_fields_set),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(LocalizationR.string.resource_form_custom_fields_edit_not_supported),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomFieldsPreview() {
    CustomFieldsFormScreen(
        onIntent = {},
        state = CustomFieldsFormState(),
        modifier = Modifier,
    )
}
