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

package com.passbolt.mobile.android.feature.settings.screen.termsandlicenses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.compose.menu.OpenableSettingsItem
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.TermsAndLicensesSettingsSideEffect.NavigateToOpenSourceLicensesSettings
import com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.TermsAndLicensesSettingsSideEffect.NavigateToPrivacyPolicy
import com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.TermsAndLicensesSettingsSideEffect.NavigateToTermsAndConditionsSettings
import com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.TermsAndLicensesSettingsSideEffect.NavigateUp
import org.koin.androidx.compose.koinViewModel
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Composable
internal fun TermsAndLicensesScreen(
    navigation: TermsAndLicensesSettingsNavigation,
    modifier: Modifier = Modifier,
    viewModel: TermsAndLicensesSettingsViewModel = koinViewModel(),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()

    TermsAndLicensesScreen(
        modifier = modifier,
        state = state.value,
        onIntent = viewModel::onIntent,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            NavigateToOpenSourceLicensesSettings -> navigation.navigateToOpenSourceLicenses()
            is NavigateToPrivacyPolicy -> navigation.navigateToPrivacyPolicy(it.privacyPolicyUrl)
            is NavigateToTermsAndConditionsSettings -> navigation.navigateToTermsAndConditions(it.termsAndConditionsUrl)
            NavigateUp -> navigation.navigateUp()
        }
    }
}

@Composable
private fun TermsAndLicensesScreen(
    onIntent: (TermsAndLicensesSettingsIntent) -> Unit,
    state: TermsAndLicensesSettingsState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
    ) {
        TitleAppBar(
            title = stringResource(LocalizationR.string.settings_terms_and_licenses),
            onBackClick = { onIntent(TermsAndLicensesSettingsIntent.GoBack) },
        )

        OpenableSettingsItem(
            iconPainter = painterResource(R.drawable.ic_terms),
            title = stringResource(LocalizationR.string.settings_terms_and_licenses_terms),
            onClick = { onIntent(TermsAndLicensesSettingsIntent.GoToTermsAndLicenses) },
            opensInternally = false,
            isEnabled = state.isTermsAndConditionsEnabled,
        )

        OpenableSettingsItem(
            iconPainter = painterResource(R.drawable.ic_lock),
            title = stringResource(LocalizationR.string.settings_terms_and_licenses_privacy_policy),
            onClick = { onIntent(TermsAndLicensesSettingsIntent.GoToPrivacyPolicy) },
            opensInternally = false,
            isEnabled = state.isPrivacyPolicyEnabled,
        )

        OpenableSettingsItem(
            iconPainter = painterResource(R.drawable.ic_licenses),
            title = stringResource(LocalizationR.string.settings_terms_and_licenses_licenses),
            onClick = { onIntent(TermsAndLicensesSettingsIntent.GoToOpenSourceLicenses) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TermsAndLicensesPreview() {
    TermsAndLicensesScreen(
        onIntent = {},
        state = TermsAndLicensesSettingsState(),
        modifier = Modifier,
    )
}
