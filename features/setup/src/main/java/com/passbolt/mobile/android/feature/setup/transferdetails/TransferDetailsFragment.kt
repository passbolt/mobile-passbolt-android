package com.passbolt.mobile.android.feature.setup.transferdetails

import android.Manifest.permission.CAMERA
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.passbolt.mobile.android.common.extension.fromHtml
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.mvp.viewbinding.BindingFragment
import com.passbolt.mobile.android.feature.setup.R
import com.passbolt.mobile.android.feature.setup.databinding.FragmentTransferDetailsBinding
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
class TransferDetailsFragment : BindingFragment<FragmentTransferDetailsBinding>(
    FragmentTransferDetailsBinding::inflate
), TransferDetailsContract.View {

    private val presenter: TransferDetailsContract.Presenter by inject()
    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.attach(this)
        initToolbar()
        setListeners()
        addSteps()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initActivityResultLauncher()
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    override fun showCameraPermissionRequiredDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.transfer_details_camera_access_dialog_title)
            .setMessage(R.string.transfer_details_camera_access_dialog_message)
            .setPositiveButton(R.string.settings) { _, _ ->
                presenter.settingsButtonClick()
            }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .show()
    }

    override fun navigateToAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .apply {
                data = Uri.fromParts("package", requireContext().packageName, null)
            }
        startActivity(intent)
    }

    private fun initActivityResultLauncher() {
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                presenter.scanQrCodesButtonClick()
            } else {
                presenter.permissionRejectedClick()
            }
        }
    }

    private fun initToolbar() {
        with(binding) {
            toolbar.setNavigationIcon(R.drawable.ic_back)
            toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        }
    }

    private fun setListeners() {
        binding.scanQrCodesButton.setDebouncingOnClick {
            presenter.scanQrCodesButtonClick()
        }
    }

    override fun showCameraRequiredDialog() {
        // TODO PAS-97
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.transfer_details_camera_required_dialog_title)
            .setMessage(R.string.transfer_details_camera_required_dialog_message)
            .setPositiveButton(R.string.settings) { _, _ ->
                presenter.settingsButtonClick()
            }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .show()
    }

    override fun requestCameraPermission() {
        requestPermissionLauncher?.launch(CAMERA)
    }

    private fun addSteps() {
        binding.steps.addList(
            requireContext().resources.getStringArray(R.array.transfer_details_steps_array)
                .map { it.fromHtml() }
        )
    }

    override fun navigateToScanQr() {
        findNavController().navigate(
            TransferDetailsFragmentDirections.actionTransferDetailsFragmentToScanQrFragment()
        )
    }
}
