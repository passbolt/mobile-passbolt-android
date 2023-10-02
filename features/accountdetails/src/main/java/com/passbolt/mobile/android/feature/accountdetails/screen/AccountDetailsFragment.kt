package com.passbolt.mobile.android.feature.accountdetails.screen

import android.content.Intent
import android.os.Bundle
import android.view.View
import coil.load
import coil.transform.CircleCropTransformation
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput
import com.passbolt.mobile.android.feature.accountdetails.databinding.FragmentAccountDetailsBinding
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.TransferAccountToAnotherDeviceActivity
import org.koin.android.ext.android.inject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

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

class AccountDetailsFragment :
    BindingScopedAuthenticatedFragment<FragmentAccountDetailsBinding, AccountDetailsContract.View>(
        FragmentAccountDetailsBinding::inflate
    ), AccountDetailsContract.View {

    override val presenter: AccountDetailsContract.Presenter by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        presenter.attach(this)
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    private fun setListeners() {
        with(binding) {
            initDefaultToolbar(toolbar)
            toolbar.setNavigationOnClickListener { requireActivity().finish() }
            labelInput.setTextChangeListener {
                presenter.labelInputChanged(it)
            }
            saveButton.setDebouncingOnClick {
                presenter.saveClick()
            }
            transferAccountButton.setDebouncingOnClick {
                presenter.transferAccountClick()
            }
        }
    }

    override fun showEmail(email: String) {
        binding.emailLabel.text = email
    }

    override fun showName(name: String) {
        binding.nameLabel.text = name
    }

    override fun showOrgUrl(orgUrl: String) {
        binding.orgUrlLabel.text = orgUrl
    }

    override fun showAvatar(avatarUrl: String?) {
        binding.avatarImage.load(avatarUrl) {
            error(CoreUiR.drawable.ic_avatar_placeholder)
            transformations(CircleCropTransformation())
            placeholder(CoreUiR.drawable.ic_avatar_placeholder)
        }
    }

    override fun showLabel(label: String) {
        binding.labelInput.text = label
    }

    override fun clearValidationErrors() {
        binding.labelInput.setState(StatefulInput.State.Default)
    }

    override fun showLabelLengthError(labelMaxLength: Int) {
        binding.labelInput.setState(
            StatefulInput.State.Error(
                getString(LocalizationR.string.validation_required_with_max_length, labelMaxLength)
            )
        )
    }

    override fun showLabelChanged() {
        showSnackbar(
            LocalizationR.string.account_details_label_saved,
            backgroundColor = CoreUiR.color.green
        )
    }

    override fun navigateToTransferAccountOnboarding() {
        startActivity(Intent(requireContext(), TransferAccountToAnotherDeviceActivity::class.java))
    }

    override fun setLabel(label: String) {
        binding.labelInput.text = label
    }
}
