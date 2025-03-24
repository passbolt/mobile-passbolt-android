package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.feature.resourceform.databinding.FragmentTotpAdvancedSettingsFormBinding
import org.koin.android.ext.android.inject
import androidx.navigation.fragment.navArgs
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput
import com.passbolt.mobile.android.ui.TotpUiModel
import com.passbolt.mobile.android.core.localization.R as LocalizationR

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
class TotpAdvancedSettingsFormFragment :
    BindingScopedFragment<FragmentTotpAdvancedSettingsFormBinding>(
        FragmentTotpAdvancedSettingsFormBinding::inflate
    ), TotpAdvancedSettingsFormContract.View {

    private val presenter: TotpAdvancedSettingsFormContract.Presenter by inject()
    private val navArgs: TotpAdvancedSettingsFormFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(binding.toolbar)
        setListeners()
        presenter.attach(this)
        presenter.argsRetrieved(navArgs.mode, navArgs.totpUiModel)
    }

    override fun showCreateTitle() {
        binding.toolbar.toolbarTitle = getString(LocalizationR.string.resource_form_create_totp)
    }

    private fun setListeners() {
        with(binding) {
            totpAdvancedSettingsSubformView.totpPeriodInput.setTextChangeListener {
                presenter.totpPeriodChanged(it)
            }
            totpAdvancedSettingsSubformView.digitsDropdown.selectedItemChangedListener = {
                presenter.totpDigitsChanged(it)
            }
            totpAdvancedSettingsSubformView.algorithmDropdown.selectedItemChangedListener = {
                presenter.totpAlgorithmChanged(it)
            }
            apply.setDebouncingOnClick {
                presenter.applyClick()
            }
        }
    }

    override fun showExpiry(expiry: String) {
        binding.totpAdvancedSettingsSubformView.totpPeriodInput.text = expiry
    }

    override fun showLength(length: String) {
        binding.totpAdvancedSettingsSubformView.digitsDropdown.setItem(length)
    }

    override fun showAlgorithm(algorithm: String) {
        binding.totpAdvancedSettingsSubformView.algorithmDropdown.setItem(algorithm)
    }

    override fun showTotpPeriodError() {
        binding.totpAdvancedSettingsSubformView.totpPeriodInput.setState(
            StatefulInput.State.Error(
                getString(LocalizationR.string.validation_required_integer)
            )
        )
    }

    override fun goBackWithResult(totpModel: TotpUiModel) {
        setFragmentResult(
            REQUEST_TOTP_ADVANCED,
            bundleOf(EXTRA_TOTP_ADVANCED to totpModel)
        )
        findNavController().popBackStack()
    }

    companion object {
        const val REQUEST_TOTP_ADVANCED = "TOTP_ADVANCED"

        const val EXTRA_TOTP_ADVANCED = "totp_advanced"
    }
}
