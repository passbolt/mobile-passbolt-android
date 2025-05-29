package com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccountonboarding

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.passbolt.mobile.android.common.extension.fromHtml
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.ui.circlestepsview.CircleStepItemModel
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.databinding.FragmentTransferAccountOnboardingBinding
import org.koin.android.ext.android.inject
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

class TransferAccountOnboardingFragment :
    BindingScopedFragment<FragmentTransferAccountOnboardingBinding>(
        FragmentTransferAccountOnboardingBinding::inflate,
    ),
    TransferAccountOnboardingContract.View {
    private val presenter: TransferAccountOnboardingContract.Presenter by inject()
    private val authenticationResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                presenter.authenticationSucceeded()
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        presenter.attach(this)
        initDefaultToolbar(requiredBinding.toolbar)
        setListeners()
        addSteps()
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    override fun navigateToTransferAccount() {
        findNavController().navigate(
            TransferAccountOnboardingFragmentDirections.actionTransferAccountOnboardingFragmentToTransferAccount(),
        )
    }

    override fun navigateToRefreshPassphrase() {
        authenticationResult.launch(
            ActivityIntents.authentication(
                requireContext(),
                ActivityIntents.AuthConfig.RefreshSession,
            ),
        )
    }

    private fun setListeners() {
        requiredBinding.startTransferButton.setDebouncingOnClick {
            presenter.startTransferButtonClick()
        }
        requiredBinding.toolbar.setNavigationOnClickListener { requireActivity().finish() }
    }

    private fun addSteps() {
        requiredBinding.steps.addList(
            requireContext()
                .resources
                .getStringArray(LocalizationR.array.transfer_account_onboarding_steps_array)
                .map { CircleStepItemModel(it.fromHtml()) },
        )
    }
}
