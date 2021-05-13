package com.passbolt.mobile.android.feature.setup.scanqr

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.passbolt.mobile.android.core.mvp.viewbinding.BindingFragment
import com.passbolt.mobile.android.core.qrscan.manager.ScanManager
import com.passbolt.mobile.android.feature.setup.R
import com.passbolt.mobile.android.feature.setup.databinding.FragmentScanQrBinding
import com.passbolt.mobile.android.feature.setup.summary.ResultStatus
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

class ScanQrFragment : BindingFragment<FragmentScanQrBinding>(FragmentScanQrBinding::inflate), ScanQrContract.View {

    private val presenter: ScanQrContract.Presenter by inject()
    private val scanManager: ScanManager by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.attach(this)
        initToolbar()
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    override fun scanResultChannel() =
        scanManager.barcodeScanResultChannel

    override fun showExitConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.scan_qr_exit_confirmation_dialog_title)
            .setMessage(R.string.scan_qr_exit_confirmation_dialog_message)
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .setPositiveButton(R.string.yes) { _, _ -> presenter.exitConfirmClick() }
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
        // TODO
    }

    override fun showBarcodeScanError() {
        // TODO
    }

    override fun showMultipleCodesInRange() {
        // TODO
    }

    override fun showCenterCameraOnBarcode() {
        // TODO
    }

    override fun showKeepGoing() {
        // TODO
    }

    override fun navigateBack() {
        findNavController().popBackStack()
    }

    override fun navigateToSummary(status: ResultStatus) {
        findNavController().navigate(
            ScanQrFragmentDirections.actionScanQrFragmentToSummaryFragment(status)
        )
    }
}
