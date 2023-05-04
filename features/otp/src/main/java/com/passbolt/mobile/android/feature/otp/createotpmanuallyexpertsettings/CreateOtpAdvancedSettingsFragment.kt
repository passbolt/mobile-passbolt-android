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

package com.passbolt.mobile.android.feature.otp.createotpmanuallyexpertsettings

import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput
import com.passbolt.mobile.android.feature.otp.R
import com.passbolt.mobile.android.feature.otp.databinding.FragmentCreateOtpAdvancedSettingsBinding
import com.passbolt.mobile.android.feature.otp.scanotp.parser.OtpParseResult
import com.passbolt.mobile.android.feature.otp.scanotp.parser.OtpParseResult.OtpQr.TotpQr.Companion.DEFAULT_DIGITS
import com.passbolt.mobile.android.ui.OtpAdvancedSettingsModel
import org.koin.android.ext.android.inject

class CreateOtpAdvancedSettingsFragment :
    BindingScopedFragment<FragmentCreateOtpAdvancedSettingsBinding>(FragmentCreateOtpAdvancedSettingsBinding::inflate),
    CreateOtpAdvancedSettingsContract.View {

    private val presenter: CreateOtpAdvancedSettingsContract.Presenter by inject()
    private val args: CreateOtpAdvancedSettingsFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(binding.toolbar)
        setupDropdowns()
        setupListeners()
        presenter.attach(this)
        presenter.bundleRetrieved(args.advancedSettingsModel)
    }

    private fun setupDropdowns() {
        with(binding) {
            totpPeriodInput.setInputType(InputType.TYPE_CLASS_NUMBER)
            algorithmDropdown.items = OtpParseResult.OtpQr.Algorithm.values().map { it.toString() }
            digitsDropdown.items = OtpParseResult.OtpQr.digitsRange.map { it.toString() }
        }
    }

    private fun setupListeners() {
        with(binding) {
            totpPeriodInput.setTextChangeListener {
                presenter.totpPeriodChanged(it)
            }
            algorithmDropdown.selectedItemChangedListener = {
                presenter.totpAlgorithmChanged(it)
            }
            digitsDropdown.selectedItemChangedListener = {
                presenter.totpDigitsChanged(it)
            }
            applyButton.setDebouncingOnClick {
                presenter.applyClick()
            }
        }
    }

    override fun setValues(values: OtpAdvancedSettingsModel) {
        with(binding) {
            totpPeriodInput.text = values.period.toString()
            algorithmDropdown.setItem(values.algorithm)
            digitsDropdown.setItem(DEFAULT_DIGITS.toString())
        }
    }

    override fun applyChangesAndGoBack(model: OtpAdvancedSettingsModel) {
        setFragmentResult(
            REQUEST_MODIFY_TOTP_SETTINGS, bundleOf(
                EXTRA_ALGORITHM to model.algorithm,
                EXTRA_PERIOD to model.period,
                EXTRA_DIGITS to model.digits
            )
        )
        findNavController().popBackStack()
    }

    override fun showTotpPeriodError() {
        binding.totpPeriodInput.setState(
            StatefulInput.State.Error(
                getString(R.string.validation_required_integer)
            )
        )
    }

    companion object {
        const val REQUEST_MODIFY_TOTP_SETTINGS = "MODIFY_TOTP_ADVANCED_SETTINGS"
        const val EXTRA_ALGORITHM = "ALGORITHM"
        const val EXTRA_PERIOD = "PERIOD"
        const val EXTRA_DIGITS = "DIGITS"
    }
}
