package com.passbolt.mobile.android.feature.setup.welcome

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.mvp.viewbinding.BindingFragment
import com.passbolt.mobile.android.feature.setup.R
import com.passbolt.mobile.android.feature.setup.databinding.FragmentWelcomeBinding
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
class WelcomeFragment : BindingFragment<FragmentWelcomeBinding>(FragmentWelcomeBinding::inflate), WelcomeContract.View {

    private val presenter: WelcomeContract.Presenter by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpListeners()
        presenter.attach(this)
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    private fun setUpListeners() {
        binding.noAccountButton.setDebouncingOnClick { presenter.noAccountButtonClick() }
        binding.connectToAccountButton.setDebouncingOnClick { presenter.connectToAccountClick() }
    }

    override fun showAccountCreationInfoDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.welcome_create_account_dialog_title)
            .setMessage(R.string.welcome_create_account_dialog_message)
            .setPositiveButton(R.string.got_it) { _, _ -> }
            .show()
    }

    override fun navigateToTransferDetails() {
        findNavController().navigate(
            WelcomeFragmentDirections.actionWelcomeFragmentToTransferDetailsFragment()
        )
    }
}
