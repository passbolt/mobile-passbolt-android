package com.passbolt.mobile.android.feature.settings.screen.autofill

import android.os.Bundle
import android.view.View
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.core.ui.dialog.showDialog
import com.passbolt.mobile.android.feature.autofill.enabled.AutofillEnabledDialog
import com.passbolt.mobile.android.feature.autofill.enabled.DialogMode
import com.passbolt.mobile.android.feature.autofill.encourage.accessibility.EncourageAccessibilityAutofillDialog
import com.passbolt.mobile.android.feature.autofill.encourage.autofill.EncourageAutofillServiceDialog
import com.passbolt.mobile.android.feature.settings.R
import com.passbolt.mobile.android.feature.settings.databinding.FragmentSettingsAutofillBinding
import org.koin.android.ext.android.inject

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
class SettingsAutofillFragment :
    BindingScopedFragment<FragmentSettingsAutofillBinding>(FragmentSettingsAutofillBinding::inflate),
    SettingsAutofillContract.View, EncourageAutofillServiceDialog.Listener,
    EncourageAccessibilityAutofillDialog.Listener, AutofillEnabledDialog.Listener {

    private val presenter: SettingsAutofillContract.Presenter by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
    }

    override fun onResume() {
        super.onResume()
        presenter.attach(this)
    }

    private fun setListeners() {
        with(binding) {
            autofillServiceSwitchContainer.setOnClickListener {
                presenter.autofillServiceSwitchClick()
            }
            accessibilityServiceSwitchContainer.setOnClickListener {
                presenter.accessibilityServiceSwitchClick()
            }
            initDefaultToolbar(toolbar)
        }
    }

    override fun setAccessibilitySwitchOn() {
        binding.accessibilityServiceSwitch.isChecked = true
    }

    override fun setAccessibilitySwitchOff() {
        binding.accessibilityServiceSwitch.isChecked = false
    }

    override fun showAutofillServiceNotSupported() {
        showSnackbar(
            R.string.settings_autofill_autofill_service_not_supported,
            backgroundColor = R.color.red
        )
    }

    override fun setAutofillSwitchOff() {
        binding.autofillServiceSwitch.isChecked = false
    }

    override fun setAutofillSwitchOn() {
        binding.autofillServiceSwitch.isChecked = true
    }

    override fun showEncourageAutofillService() {
        showDialog(
            childFragmentManager,
            EncourageAutofillServiceDialog(),
            EncourageAutofillServiceDialog::class.java.name
        )
    }

    override fun autofillSetupSuccessfully() {
        presenter.autofillSetupSuccessfully()
    }

    override fun autofillSetupClosed() {
        // ignore
    }

    override fun showEncourageAccessibilityService() {
        showDialog(
            childFragmentManager,
            EncourageAccessibilityAutofillDialog(),
            EncourageAccessibilityAutofillDialog::class.java.name
        )
    }

    override fun accessibilitySettingsPossibleChange() {
        presenter.viewResumed()
    }

    override fun setupAccessibilityLaterClick() {
        // ignore
    }

    override fun showAutofillFeatureEnabledSuccess() {
        AutofillEnabledDialog.newInstance(DialogMode.Settings)
            .show(childFragmentManager, AutofillEnabledDialog::class.java.name)
    }

    override fun goToAppClick() {
        // ignore
    }

    override fun autofillEnabledDialogDismissed() {
        // ignore
    }
}
