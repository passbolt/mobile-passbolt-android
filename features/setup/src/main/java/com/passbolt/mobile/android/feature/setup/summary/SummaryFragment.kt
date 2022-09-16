package com.passbolt.mobile.android.feature.setup.summary

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.common.extension.visible
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.feature.authentication.R
import com.passbolt.mobile.android.feature.setup.databinding.FragmentSummaryBinding
import com.passbolt.mobile.android.helpmenu.HelpMenuFragment
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
class SummaryFragment : BindingScopedFragment<FragmentSummaryBinding>(
    FragmentSummaryBinding::inflate
), SummaryContract.View, HelpMenuFragment.Listener {

    private val presenter: SummaryContract.Presenter by inject()
    private val args: SummaryFragmentArgs by navArgs()
    private val authenticationResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            presenter.authenticationSucceeded()
        }
    }
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            presenter.backClick()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        presenter.attach(this)
        presenter.start(args.status)
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, backPressedCallback)
    }

    override fun onDestroyView() {
        backPressedCallback.isEnabled = false
        presenter.detach()
        super.onDestroyView()
    }

    private fun setListeners() {
        with(binding) {
            resultView.setButtonAction { presenter.buttonClick() }
            helpButton.setDebouncingOnClick { presenter.helpClick() }
        }
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

    override fun navigateToScanQr() {
        findNavController().navigate(
            SummaryFragmentDirections.actionSummaryFragmentToScanQrFragment()
        )
    }

    override fun navigateToStart() {
        startActivity(ActivityIntents.start(requireContext()))
    }

    override fun showLeaveConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.are_you_sure)
            .setMessage(R.string.auth_exit_dialog_message)
            .setPositiveButton(R.string.continue_setup) { _, _ -> }
            .setNegativeButton(R.string.cancel_setup) { _, _ -> presenter.leaveConfirmationClick() }
            .show()
    }

    override fun navigateToSignIn(userId: String) {
        authenticationResult.launch(
            ActivityIntents.authentication(
                requireContext(),
                ActivityIntents.AuthConfig.Setup,
                userId = userId
            )
        )
    }

    override fun navigateToManageAccounts() {
        startActivity(
            ActivityIntents.authentication(
                requireContext(),
                ActivityIntents.AuthConfig.ManageAccount
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        )
    }

    override fun showHelpButton() {
        binding.helpButton.visible()
    }

    override fun navigateToFingerprintSetup() {
        findNavController().navigate(
            SummaryFragmentDirections.actionSummaryFragmentToFingerprintFragment()
        )
    }

    override fun showHelpMenu() {
        HelpMenuFragment.newInstance()
            .show(childFragmentManager, HelpMenuFragment::class.java.name)
    }

    override fun menuShowLogsClick() {
        findNavController().navigate(
            SummaryFragmentDirections.actionSummaryFragmentToLogs()
        )
    }
}
