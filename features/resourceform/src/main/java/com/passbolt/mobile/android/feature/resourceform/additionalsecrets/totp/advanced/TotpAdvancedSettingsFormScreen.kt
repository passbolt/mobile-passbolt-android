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

package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced

import PassboltTheme
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.results.NavigationResultEventBus
import com.passbolt.mobile.android.core.ui.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.dropdown.DropdownInput
import com.passbolt.mobile.android.core.ui.text.TextInput
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Default
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Error
import com.passbolt.mobile.android.core.ui.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormIntent.AlgorithmChanged
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormIntent.ApplyChanges
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormIntent.DigitChanged
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormIntent.GoBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormIntent.PeriodChanged
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormSideEffect.ApplyAndGoBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced.TotpPeriodValidationError.MustBePositiveInteger
import com.passbolt.mobile.android.feature.resourceform.navigation.TotpAdvancedSettingsFormResult
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.ResourceFormMode.Create
import com.passbolt.mobile.android.ui.ResourceFormMode.Edit
import com.passbolt.mobile.android.ui.TotpUiModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun TotpAdvancedSettingsFormScreen(
    mode: ResourceFormMode,
    totpUiModel: TotpUiModel,
    modifier: Modifier = Modifier,
    navigator: AppNavigator = koinInject(),
    viewModel: TotpAdvancedSettingsFormViewModel = koinViewModel(parameters = { parametersOf(mode, totpUiModel) }),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val resultBus = NavigationResultEventBus.current

    TotpAdvancedSettingsFormScreen(
        modifier = modifier,
        state = state.value,
        onIntent = viewModel::onIntent,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            is ApplyAndGoBack -> {
                resultBus.sendResult(result = TotpAdvancedSettingsFormResult(it.totpModel))
                navigator.navigateBack()
            }
            NavigateBack -> navigator.navigateBack()
        }
    }
}

@Composable
private fun TotpAdvancedSettingsFormScreen(
    onIntent: (TotpAdvancedSettingsFormIntent) -> Unit,
    state: TotpAdvancedSettingsFormState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sectionColor = colorResource(CoreUiR.color.section_background)

    Scaffold(
        modifier = modifier,
        topBar = {
            TitleAppBar(
                title = getScreenTitle(context, state.resourceFormMode),
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
            Text(
                text = stringResource(LocalizationR.string.resource_form_totp_settings),
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(sectionColor)
                        .padding(12.dp),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(colorResource(CoreUiR.color.yellow))
                            .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(CoreUiR.drawable.ic_alert_triangle),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(LocalizationR.string.otp_create_totp_expert_settings_warning),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextInput(
                        title = stringResource(LocalizationR.string.otp_create_totp_expert_settings_expiry),
                        hint = stringResource(LocalizationR.string.otp_create_totp_expert_settings_expiry_hint),
                        isRequired = true,
                        text = state.expiry,
                        onTextChange = { onIntent(PeriodChanged(it)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        state =
                            if (state.periodValidationErrors.isEmpty()) {
                                Default
                            } else {
                                Error(getPeriodErrorMessage(context, state.periodValidationErrors))
                            },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(LocalizationR.string.otp_create_totp_expert_settings_expiry_seconds_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DropdownInput(
                        title = stringResource(LocalizationR.string.otp_create_totp_expert_settings_length),
                        items = state.digitsOptions,
                        selectedItem = state.length,
                        onItemSelect = { onIntent(DigitChanged(it)) },
                        isRequired = true,
                        modifier = Modifier.width(120.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(LocalizationR.string.otp_create_totp_expert_settings_expiry_length_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                DropdownInput(
                    title = stringResource(LocalizationR.string.otp_create_totp_expert_settings_algorithm),
                    items = state.algorithms,
                    selectedItem = state.algorithm,
                    onItemSelect = { onIntent(AlgorithmChanged(it)) },
                    isRequired = true,
                )
            }
        }
    }
}

private fun getScreenTitle(
    context: Context,
    resourceFormMode: ResourceFormMode?,
): String =
    when (resourceFormMode) {
        is Create -> context.getString(LocalizationR.string.resource_form_create_totp)
        is Edit -> context.getString(LocalizationR.string.resource_form_edit_resource, resourceFormMode.resourceName)
        null -> ""
    }

private fun getPeriodErrorMessage(
    context: Context,
    errors: List<TotpPeriodValidationError>,
): String =
    when (errors.first()) {
        MustBePositiveInteger -> context.getString(LocalizationR.string.validation_required_integer)
    }

@Preview(showBackground = true)
@Composable
private fun TotpAdvancedSettingsFormScreenPreview() {
    PassboltTheme {
        TotpAdvancedSettingsFormScreen(
            onIntent = {},
            state =
                TotpAdvancedSettingsFormState(
                    resourceFormMode =
                        Create(
                            leadingContentType = LeadingContentType.TOTP,
                            parentFolderId = null,
                        ),
                    expiry = "30",
                    length = "6",
                    algorithm = "SHA1",
                ),
        )
    }
}
