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

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.TermsAndLicensesSettingsIntent.GoBack
import com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.TermsAndLicensesSettingsIntent.GoToOpenSourceLicenses
import com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.TermsAndLicensesSettingsIntent.GoToPrivacyPolicy
import com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.TermsAndLicensesSettingsIntent.GoToTermsAndLicenses
import com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.TermsAndLicensesSettingsSideEffect.NavigateToOpenSourceLicensesSettings
import com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.TermsAndLicensesSettingsSideEffect.NavigateToPrivacyPolicy
import com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.TermsAndLicensesSettingsSideEffect.NavigateToTermsAndConditionsSettings
import com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.TermsAndLicensesSettingsSideEffect.NavigateUp
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import kotlinx.coroutines.launch

internal class TermsAndLicensesSettingsViewModel(
    private val getFeatureFlagsUseCase: GetFeatureFlagsUseCase,
) : SideEffectViewModel<TermsAndLicensesSettingsState, TermsAndLicensesSettingsSideEffect>(TermsAndLicensesSettingsState()) {
    init {
        loadInitialState()
    }

    fun onIntent(intent: TermsAndLicensesSettingsIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateUp)
            GoToOpenSourceLicenses -> emitSideEffect(NavigateToOpenSourceLicensesSettings)
            GoToPrivacyPolicy -> viewState.value.privacyPolicyUrl?.let { emitSideEffect(NavigateToPrivacyPolicy(it)) }
            GoToTermsAndLicenses -> viewState.value.termsAndConditionsUrl?.let { emitSideEffect(NavigateToTermsAndConditionsSettings(it)) }
        }
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            val featureFlags = getFeatureFlagsUseCase.execute(Unit).featureFlags
            updateViewState {
                TermsAndLicensesSettingsState(
                    privacyPolicyUrl = featureFlags.privacyPolicyUrl,
                    termsAndConditionsUrl = featureFlags.termsAndConditionsUrl,
                    isPrivacyPolicyEnabled = !featureFlags.privacyPolicyUrl.isNullOrBlank(),
                    isTermsAndConditionsEnabled = !featureFlags.termsAndConditionsUrl.isNullOrBlank(),
                )
            }
        }
    }
}
