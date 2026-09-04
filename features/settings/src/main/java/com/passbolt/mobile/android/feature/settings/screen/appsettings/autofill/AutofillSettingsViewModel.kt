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

package com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill

import com.passbolt.mobile.android.core.autofill.AutofillInformationProvider
import com.passbolt.mobile.android.core.autofill.AutofillInformationProvider.ChromeNativeAutofillStatus.ENABLED
import com.passbolt.mobile.android.core.autofill.AutofillInformationProvider.ChromeNativeAutofillStatus.NOT_SUPPORTED
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.domain.preferences.GlobalPreferencesUpdate
import com.passbolt.mobile.android.domain.preferences.usecase.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.domain.preferences.usecase.UpdateGlobalPreferencesUseCase
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillScreenSideEffect.ErrorSnackbarType.NATIVE_AUTOFILL_NOT_SUPPORTED
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillScreenSideEffect.NavigateToAccessibilityPoliciesConsent
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillScreenSideEffect.NavigateToAutofillEnabled
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillScreenSideEffect.NavigateToChromeNativeAutofill
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillScreenSideEffect.NavigateToEncourageAccessibilityAutofill
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillScreenSideEffect.NavigateToEncourageNativeAutofill
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillScreenSideEffect.NavigateUp
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillScreenSideEffect.ShowErrorSnackBar
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillSettingsIntent.GoBack
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillSettingsIntent.ToggleAccessibilityAutofill
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillSettingsIntent.ToggleCopyTotpOnAutofill
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillSettingsIntent.ToggleChromeNativeAutofill
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillSettingsIntent.ToggleNativeAutofill
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillSettingsIntent.UpdateAutofillState
import timber.log.Timber

internal class AutofillSettingsViewModel(
    private val autofillInformationProvider: AutofillInformationProvider,
    private val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase,
    private val updateGlobalPreferencesUseCase: UpdateGlobalPreferencesUseCase,
) : SideEffectViewModel<AutofillSettingsState, AutofillScreenSideEffect>(AutofillSettingsState()) {
    init {
        loadInitialValues()
    }

    fun onIntent(intent: AutofillSettingsIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateUp)
            ToggleAccessibilityAutofill -> toggleAccessibilityAutofill()
            ToggleChromeNativeAutofill -> emitSideEffect(NavigateToChromeNativeAutofill)
            ToggleNativeAutofill -> toggleNativeAutofill()
            UpdateAutofillState -> loadInitialValues()
            ToggleCopyTotpOnAutofill -> toggleCopyTotpOnAutofill()
        }
    }

    private fun toggleCopyTotpOnAutofill() {
        val enabled = !viewState.value.isCopyTotpOnAutofillChecked
        updateGlobalPreferencesUseCase.execute(GlobalPreferencesUpdate(isCopyTotpOnAutofillEnabled = enabled))
        updateViewState { copy(isCopyTotpOnAutofillChecked = enabled) }
    }

    private fun toggleAccessibilityAutofill() {
        if (getGlobalPreferencesUseCase.execute(Unit).accessibilityPoliciesConsentGiven) {
            emitSideEffect(NavigateToEncourageAccessibilityAutofill)
        } else {
            Timber.d("Accessibility consent shown (no prior consent)")
            emitSideEffect(NavigateToAccessibilityPoliciesConsent)
        }
    }

    private fun toggleNativeAutofill() {
        if (autofillInformationProvider.isAutofillServiceSupported()) {
            if (viewState.value.isNativeAutofillChecked) {
                emitSideEffect(NavigateToAutofillEnabled)
            } else {
                emitSideEffect(NavigateToEncourageNativeAutofill)
            }
        } else {
            emitSideEffect(ShowErrorSnackBar(NATIVE_AUTOFILL_NOT_SUPPORTED))
        }
    }

    private fun loadInitialValues() {
        val isAccessibilityAutofillChecked = autofillInformationProvider.isAccessibilityAutofillSetup()
        val isNativeAutofillChecked =
            autofillInformationProvider.isAutofillServiceSupported() &&
                autofillInformationProvider.isPassboltAutofillServiceSet()
        val chromeNativeAutofillStatus = autofillInformationProvider.getChromeNativeAutofillStatus()
        val isChromeNativeAutofillChecked = chromeNativeAutofillStatus == ENABLED
        val isChromeNativeAutofillEnabled = chromeNativeAutofillStatus != NOT_SUPPORTED
        val isCopyTotpOnAutofillChecked = getGlobalPreferencesUseCase.execute(Unit).isCopyTotpOnAutofillEnabled
        updateViewState {
            copy(
                isCopyTotpOnAutofillChecked = isCopyTotpOnAutofillChecked,
                isNativeAutofillChecked = isNativeAutofillChecked,
                isAccessibilityAutofillChecked = isAccessibilityAutofillChecked,
                isChromeNativeAutofillChecked = isChromeNativeAutofillChecked,
                isChromeNativeAutofillEnabled = isChromeNativeAutofillEnabled,
            )
        }
    }
}
