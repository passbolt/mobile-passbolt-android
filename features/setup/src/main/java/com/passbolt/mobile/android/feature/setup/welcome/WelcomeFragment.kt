package com.passbolt.mobile.android.feature.setup.welcome

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.passbolt.mobile.android.common.dialogs.howToCreateAccountDialog
import com.passbolt.mobile.android.common.dialogs.qrCodesInformationDialog
import com.passbolt.mobile.android.common.dialogs.rootWarningAlertDialog
import com.passbolt.mobile.android.core.extension.gone
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.feature.setup.databinding.FragmentWelcomeBinding
import com.passbolt.mobile.android.feature.setup.summary.ResultStatus
import com.passbolt.mobile.android.helpmenu.HelpMenuFragment
import com.passbolt.mobile.android.ui.HelpMenuModel
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
class WelcomeFragment :
    BindingScopedFragment<FragmentWelcomeBinding>(FragmentWelcomeBinding::inflate),
    WelcomeContract.View,
    HelpMenuFragment.Listener {
    private val presenter: WelcomeContract.Presenter by inject()

    private val accountKitFileChosenResult =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) {
            it?.let { uri ->
                requireContext().applicationContext.contentResolver.openInputStream(uri).use { inputStream ->
                    inputStream?.use {
                        presenter.accountKitSelected(inputStream.reader().readText())
                    }
                }
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setUpListeners()
        presenter.attach(this)
        presenter.argsRetrieved(requireActivity().isTaskRoot)
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    private fun setUpListeners() {
        with(requiredBinding) {
            noAccountButton.setDebouncingOnClick { presenter.noAccountButtonClick() }
            connectToAccountButton.setDebouncingOnClick { presenter.connectToAccountClick() }
            helpButton.setDebouncingOnClick {
                presenter.helpClick()
            }
        }
    }

    override fun initBackNavigation() {
        with(requiredBinding.toolbar) {
            setNavigationIcon(com.passbolt.mobile.android.core.ui.R.drawable.ic_back)
            setNavigationOnClickListener { requireActivity().finish() }
        }
    }

    override fun showAccountCreationInfoDialog() {
        howToCreateAccountDialog(requireContext()).show()
    }

    override fun navigateToTransferDetails() {
        findNavController().navigate(
            WelcomeFragmentDirections.actionWelcomeFragmentToTransferDetailsFragment(),
        )
    }

    override fun hideToolbar() {
        requiredBinding.toolbar.gone()
    }

    override fun showDeviceRootedDialog() {
        rootWarningAlertDialog(requireContext())
            .show()
    }

    override fun showHelpMenu() {
        HelpMenuFragment
            .newInstance(
                HelpMenuModel(
                    shouldShowShowQrCodesHelp = true,
                    shouldShowImportProfile = true,
                    shouldShowImportAccountKit = true,
                ),
            ).show(childFragmentManager, HelpMenuFragment::class.java.name)
    }

    override fun menuImportProfileManuallyClick() {
        presenter.importProfileClick()
    }

    override fun menuShowLogsClick() {
        findNavController().navigate(
            WelcomeFragmentDirections.actionWelcomeFragmentToLogs(),
        )
    }

    override fun navigateToImportProfile() {
        findNavController().navigate(
            WelcomeFragmentDirections.actionWelcomeFragmentToImportProfileFragment(),
        )
    }

    override fun menuImportAccountKitClick() {
        presenter.importAccountKitClick()
    }

    override fun menuWhyScanQrCodesClick() {
        qrCodesInformationDialog(requireContext()).show()
    }

    override fun showAccountKitFilePicker() {
        accountKitFileChosenResult.launch(arrayOf("*/*"))
    }

    override fun navigateToSummary(status: ResultStatus) {
        findNavController().navigate(
            WelcomeFragmentDirections.actionWelcomeFragmentToSummaryFragment(status),
        )
    }
}
