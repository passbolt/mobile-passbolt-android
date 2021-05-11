package com.passbolt.mobile.android.feature.setup.summary

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.passbolt.mobile.android.core.mvp.viewbinding.BindingFragment
import com.passbolt.mobile.android.feature.setup.databinding.FragmentSummaryBinding
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
class SummaryFragment : BindingFragment<FragmentSummaryBinding>(
    FragmentSummaryBinding::inflate
), SummaryContract.View {

    private val presenter: SummaryContract.Presenter by inject()
    private val args: SummaryFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        presenter.attach(this)
        presenter.start(args.status)
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    private fun setListeners() {
        binding.resultView.setButtonAction { presenter.buttonClick() }
    }

    override fun setTitle(title: Int) {
        binding.resultView.setTitle(getString(title))
    }

    override fun setDescription(message: String) {
        binding.resultView.setDescription(message)
    }

    override fun setButtonLabel(text: Int) {
        binding.resultView.setButtonLabel(getString(text))
    }

    override fun setIcon(icon: Int) {
        binding.resultView.setIcon(icon)
    }

    override fun navigateBack() {
        findNavController().popBackStack()
    }
}
