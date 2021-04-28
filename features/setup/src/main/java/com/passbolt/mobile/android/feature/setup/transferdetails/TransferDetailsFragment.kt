package com.passbolt.mobile.android.feature.setup.transferdetails

import android.os.Bundle
import android.view.View
import com.passbolt.mobile.android.common.extension.fromHtml
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.mvp.viewbinding.BindingFragment
import com.passbolt.mobile.android.feature.setup.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.passbolt.mobile.android.feature.setup.databinding.FragmentTransferDetailsBinding

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

@AndroidEntryPoint
class TransferDetailsFragment : BindingFragment<FragmentTransferDetailsBinding>(
    FragmentTransferDetailsBinding::inflate
), TransferDetailsContract.View {

    @Inject
    lateinit var presenter: TransferDetailsContract.Presenter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.attach(this)
        initToolbar()
        setListeners()
        addSteps()
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    private fun initToolbar() {
        binding.toolbar.setNavigationIcon(R.drawable.ic_back)
        binding.toolbar.setNavigationOnClickListener { requireActivity().onBackPressed() }
    }

    private fun setListeners() {
        binding.scanQrCodesButton.setDebouncingOnClick {
            presenter.scanQrCodesButtonClicked()
        }
    }

    private fun addSteps() {
        binding.steps.addList(
            requireContext().resources.getStringArray(R.array.transfer_details_steps_array)
                .map { it.fromHtml() }
        )
    }
}
