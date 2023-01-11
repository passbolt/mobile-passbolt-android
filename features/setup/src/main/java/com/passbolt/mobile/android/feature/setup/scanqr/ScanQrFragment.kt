package com.passbolt.mobile.android.feature.setup.scanqr

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.core.qrscan.SCAN_MANAGER_SCOPE
import com.passbolt.mobile.android.core.qrscan.manager.ScanManager
import com.passbolt.mobile.android.core.security.flagsecure.FlagSecureSetter
import com.passbolt.mobile.android.core.ui.dialog.CoreDialogFactory
import com.passbolt.mobile.android.feature.setup.AccountSetupDataHolder
import com.passbolt.mobile.android.feature.setup.R
import com.passbolt.mobile.android.feature.setup.databinding.FragmentScanQrBinding
import com.passbolt.mobile.android.feature.setup.summary.ResultStatus
import com.passbolt.mobile.android.helpmenu.HelpMenuFragment
import com.passbolt.mobile.android.ui.HelpMenuModel
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope

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

class ScanQrFragment : BindingScopedFragment<FragmentScanQrBinding>(FragmentScanQrBinding::inflate),
    ScanQrContract.View, HelpMenuFragment.Listener {

    private val presenter: ScanQrContract.Presenter by inject()
    private lateinit var scanManagerScope: Scope
    private lateinit var scanManager: ScanManager
    private var serverNotReachableDialog: AlertDialog? = null
    private val flagSecureSetter: FlagSecureSetter by inject()

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            presenter.backClick()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar()
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, backPressedCallback)
        scanManagerScope = getKoin().getOrCreateScope(SCAN_MANAGER_SCOPE, named(SCAN_MANAGER_SCOPE))
        scanManager = scanManagerScope.get()

        presenter.attach(this)
        presenter.argsRetrieved((requireActivity() as? AccountSetupDataHolder)?.bundledAccountSetupData)
    }

    override fun onResume() {
        super.onResume()
        presenter.viewResumed()
    }

    override fun onPause() {
        presenter.viewPaused()
        super.onPause()
    }

    override fun onDestroyView() {
        serverNotReachableDialog?.dismiss()
        serverNotReachableDialog = null
        backPressedCallback.isEnabled = false
        scanManager.detach()
        scanManagerScope.close()
        presenter.detach()
        super.onDestroyView()
    }

    override fun scanResultChannel() = scanManager.barcodeScanPublisher

    override fun showExitConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.are_you_sure)
            .setMessage(R.string.scan_qr_exit_confirmation_dialog_message)
            .setPositiveButton(R.string.cancel) { _, _ -> }
            .setNegativeButton(R.string.stop_scanning) { _, _ -> presenter.exitConfirmClick() }
            .show()
    }

    override fun showInformationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.scan_qr_exit_information_dialog_title)
            .setMessage(R.string.scan_qr_exit_information_dialog_message)
            .setPositiveButton(R.string.got_it) { _, _ -> }
            .show()
    }

    private fun initToolbar() {
        with(binding.progressToolbar) {
            setNavigationIcon(R.drawable.ic_back)
            setNavigationOnClickListener { presenter.backClick() }
            addIconEnd(R.drawable.ic_help) { presenter.infoIconClick() }
        }
    }

    override fun initializeProgress(totalPages: Int) {
        with(binding.progressToolbar) {
            initializeProgressBar(0, totalPages)
            setCurrentProgress(0)
        }
    }

    override fun setProgress(progress: Int) {
        binding.progressToolbar.setCurrentProgress(progress)
    }

    override fun startAnalysis() {
        try {
            scanManager.attach(this, binding.cameraPreview)
        } catch (exception: Exception) {
            presenter.startCameraError(exception)
        }
    }

    override fun showStartCameraError() {
        binding.tooltip.text = getString(R.string.scan_qr_camera_error)
    }

    override fun showBarcodeScanError(message: String?) {
        val messageBuilder = StringBuilder(getString(R.string.scan_qr_scanning_error)).apply {
            if (!message.isNullOrBlank()) {
                append("(%s)".format(message))
            }
        }
        binding.tooltip.text = messageBuilder.toString()
    }

    override fun showMultipleCodesInRange() {
        binding.tooltip.text = getString(R.string.scan_qr_multiple_codes_in_range)
    }

    override fun showCenterCameraOnBarcode() {
        binding.tooltip.text = getString(R.string.scan_qr_aim_at_qr_code)
    }

    override fun showKeepGoing() {
        binding.tooltip.text = getString(R.string.scan_qr_keep_going)
    }

    override fun showUpdateTransferError(headerMessage: String) {
        val messageBuilder = StringBuilder(getString(R.string.scan_qr_update_transfer_error)).apply {
            if (headerMessage.isNotBlank()) {
                append("(%s)".format(headerMessage))
            }
        }
        Toast.makeText(requireContext(), messageBuilder.toString(), Toast.LENGTH_LONG)
            .show()
    }

    override fun showNotAPassboltQr() {
        binding.tooltip.text = getString(R.string.scan_qr_not_a_passbolt_qr)
    }

    override fun navigateBack() {
        findNavController().popBackStack()
    }

    override fun navigateToSummary(status: ResultStatus) {
        findNavController().navigate(
            ScanQrFragmentDirections.actionScanQrFragmentToSummaryFragment(status)
        )
    }

    override fun showServerNotReachable(serverDomain: String) {
        if (serverNotReachableDialog == null) {
            serverNotReachableDialog = CoreDialogFactory.serverNotReachableDialog(requireContext(), serverDomain)
        }
        serverNotReachableDialog?.show()
    }

    override fun setFlagSecure() {
        flagSecureSetter.set(requireActivity())
    }

    override fun removeFlagSecure() {
        flagSecureSetter.remove(requireActivity())
    }

    override fun showHelpMenu() {
        HelpMenuFragment.newInstance(HelpMenuModel(shouldShowShowQrCodesHelp = true, shouldShowImportProfile = true))
            .show(childFragmentManager, HelpMenuFragment::class.java.name)
    }

    override fun menuShowLogsClick() {
        findNavController().navigate(
            ScanQrFragmentDirections.actionScanQrFragmentToLogs()
        )
    }

    override fun menuImportProfileManuallyClick() {
        presenter.importProfileClick()
    }

    override fun menuWhyScanQrCodesClick() {
        showInformationDialog()
    }

    override fun navigateToImportProfile() {
        findNavController().navigate(
            ScanQrFragmentDirections.actionScanQrFragmentToImportProfileFragment()
        )
    }
}
