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

import PassboltTheme
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.passbolt.mobile.android.common.ExternalDeeplinkHandler
import com.passbolt.mobile.android.core.ui.dialog.showDialog
import com.passbolt.mobile.android.feature.autofill.enabled.AutofillEnabledDialog
import com.passbolt.mobile.android.feature.autofill.enabled.DialogMode
import com.passbolt.mobile.android.feature.autofill.encourage.accessibility.EncourageAccessibilityAutofillDialog
import com.passbolt.mobile.android.feature.autofill.encourage.autofill.EncourageAutofillServiceDialog
import org.koin.android.ext.android.inject

class AutofillSettingsComposeFragment :
    Fragment(),
    AutofillSettingsNavigation,
    AutofillEnabledDialog.Listener by EmptyAutofillEnabledListener(),
    EncourageAccessibilityAutofillDialog.Listener by EmptyAccessibilityAutofillChangeListener(),
    EncourageAutofillServiceDialog.Listener {
    private val externalDeeplinkHandler: ExternalDeeplinkHandler by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                PassboltTheme {
                    AutofillSettingsScreen(
                        navigation = this@AutofillSettingsComposeFragment,
                    )
                }
            }
        }

    override fun navigateUp() {
        findNavController().popBackStack()
    }

    // TODO refactor to compose dialogs
    override fun navigateToEncourageNativeAutofill() {
        showDialog(
            childFragmentManager,
            EncourageAutofillServiceDialog(),
            EncourageAutofillServiceDialog::class.java.name,
        )
    }

    // TODO refactor to compose dialogs
    override fun navigateToNativeAutofillEnabled() {
        showDialog(
            childFragmentManager,
            AutofillEnabledDialog.newInstance(DialogMode.Settings),
            AutofillEnabledDialog::class.java.name,
        )
    }

    // TODO refactor to compose dialogs
    override fun navigateToEncourageAccessibilityAutofill() {
        showDialog(
            childFragmentManager,
            EncourageAccessibilityAutofillDialog(),
            EncourageAccessibilityAutofillDialog::class.java.name,
        )
    }

    override fun autofillSetupSuccessfully() {
        navigateToNativeAutofillEnabled()
    }

    override fun navigateToChromeNativeAutofillSettings() {
        externalDeeplinkHandler.openChromeNativeAutofillSettings(requireContext())
    }
}
