package com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.summary

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.visible
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.databinding.FragmentTransferAccountSummaryBinding
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
class TransferAccountSummaryFragment :
    BindingScopedFragment<FragmentTransferAccountSummaryBinding>(
        FragmentTransferAccountSummaryBinding::inflate,
    ),
    TransferAccountSummaryContract.View {
    private val presenter: TransferAccountSummaryContract.Presenter by inject()
    private val args: TransferAccountSummaryFragmentArgs by navArgs()
    private val backPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                presenter.backClick()
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        presenter.attach(this)
        presenter.argsRetrieved(args.status)
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, backPressedCallback)
    }

    override fun onDestroyView() {
        backPressedCallback.isEnabled = false
        presenter.detach()
        super.onDestroyView()
    }

    private fun setListeners() {
        with(requiredBinding) {
            resultView.setButtonAction { presenter.buttonClick() }
            tryAgainButton.setDebouncingOnClick { presenter.tryAgainClick() }
        }
    }

    override fun setTitle(title: Int) {
        requiredBinding.resultView.setTitle(getString(title))
    }

    override fun setDescription(message: String) {
        requiredBinding.resultView.setDescription(message)
    }

    override fun setButtonLabel(text: Int) {
        requiredBinding.resultView.setButtonLabel(getString(text))
    }

    override fun setIcon(icon: Int) {
        requiredBinding.resultView.setIcon(icon)
    }

    override fun finish() {
        requireActivity().finish()
    }

    override fun showTryAgain() {
        requiredBinding.tryAgainButton.visible()
    }

    override fun navigateToTransferAccountStart() {
        findNavController().navigate(
            TransferAccountSummaryFragmentDirections
                .actionTransferAccountSummaryFragmentToTransferAccountOnboardingFragment(),
        )
    }
}
