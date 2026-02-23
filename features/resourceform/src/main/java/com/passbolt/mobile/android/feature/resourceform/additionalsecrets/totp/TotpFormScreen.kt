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

package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp

import PassboltTheme
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.ui.compose.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.compose.button.SecondaryIconButton
import com.passbolt.mobile.android.core.ui.compose.text.TextInput
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Default
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Error
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.ApplyChanges
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.GoBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.IssuerChanged
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.MoreSettingsClick
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.RemoveTotp
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.ScanTotpClick
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.SecretChanged
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormSideEffect.ApplyAndGoBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormSideEffect.NavigateToAdvancedSettings
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormSideEffect.NavigateToScanTotp
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.ResourceFormMode.Create
import com.passbolt.mobile.android.ui.ResourceFormMode.Edit
import org.koin.androidx.compose.koinViewModel
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun TotpFormScreen(
    navigation: TotpFormNavigation,
    modifier: Modifier = Modifier,
    viewModel: TotpFormViewModel = koinViewModel(),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()

    TotpFormScreen(
        modifier = modifier,
        state = state.value,
        onIntent = viewModel::onIntent,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            is ApplyAndGoBack -> navigation.navigateBackWithResult(it.totpUiModel)
            NavigateBack -> navigation.navigateBack()
            is NavigateToAdvancedSettings -> navigation.navigateToAdvancedSettings(it.mode, it.totpUiModel)
            NavigateToScanTotp -> navigation.navigateToScanTotp()
        }
    }
}

@Composable
private fun TotpFormScreen(
    onIntent: (TotpFormIntent) -> Unit,
    state: TotpFormState,
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
                text = stringResource(LocalizationR.string.resource_form_create_totp),
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
                    verticalAlignment = Alignment.Bottom,
                ) {
                    TextInput(
                        title = stringResource(LocalizationR.string.resource_form_totp_secret),
                        hint = stringResource(LocalizationR.string.resource_form_totp_secret),
                        text = state.secret,
                        onTextChange = { onIntent(SecretChanged(it)) },
                        state =
                            if (state.secretValidationErrors.isEmpty()) {
                                Default
                            } else {
                                Error(getSecretErrorMessage(context, state.secretValidationErrors))
                            },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    SecondaryIconButton(
                        modifier = Modifier.size(56.dp),
                        onClick = { onIntent(ScanTotpClick) },
                        icon = painterResource(CoreUiR.drawable.ic_camera),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextInput(
                    title = stringResource(LocalizationR.string.resource_form_totp_url_issuer),
                    hint = stringResource(LocalizationR.string.resource_form_totp_url),
                    text = state.issuer,
                    onTextChange = { onIntent(IssuerChanged(it)) },
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable { onIntent(MoreSettingsClick) }
                            .padding(
                                horizontal = 16.dp,
                            ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(CoreUiR.drawable.ic_cog),
                        contentDescription = null,
                        tint = colorResource(CoreUiR.color.icon_tint),
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(LocalizationR.string.resource_form_totp_more_settings),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        painter = painterResource(CoreUiR.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = colorResource(CoreUiR.color.icon_tint),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onIntent(RemoveTotp) }
                        .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(CoreUiR.drawable.ic_trash),
                    contentDescription = null,
                    tint = colorResource(CoreUiR.color.icon_tint),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(LocalizationR.string.resource_form_remove_totp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
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
        is Create -> "TOTP"
        is Edit -> context.getString(LocalizationR.string.resource_form_edit_resource, resourceFormMode.resourceName)
        null -> ""
    }

@Preview(showBackground = true)
@Composable
private fun TotpFormScreenPreview() {
    PassboltTheme {
        TotpFormScreen(
            onIntent = {},
            state =
                TotpFormState(
                    resourceFormMode =
                        Create(
                            leadingContentType = LeadingContentType.TOTP,
                            parentFolderId = null,
                        ),
                    secret = "ABCDEFGHIJKJLM",
                    issuer = "passbolt.com",
                ),
        )
    }
}
