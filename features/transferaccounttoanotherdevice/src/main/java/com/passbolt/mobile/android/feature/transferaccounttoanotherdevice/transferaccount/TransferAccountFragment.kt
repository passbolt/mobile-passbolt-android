package com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.passbolt.mobile.android.common.dialogs.cancelTransferAccountAlertDialog
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.databinding.FragmentTransferAccountBinding
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.summary.TransferAccountStatus
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

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

class TransferAccountFragment :
    BindingScopedAuthenticatedFragment<FragmentTransferAccountBinding, TransferAccountContract.View>(
        FragmentTransferAccountBinding::inflate
    ), TransferAccountContract.View {

    override val presenter: TransferAccountContract.Presenter by inject()
    private val barcodeEncoder: BarcodeEncoder by inject()
    private val qrCodeGenHints: HashMap<EncodeHintType, Any> by inject(named(QR_CODE_GEN_HINTS))
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            presenter.backClick()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.attach(this)
        setListeners()
    }

    override fun onDestroyView() {
        backPressedCallback.isEnabled = false
        presenter.detach()
        super.onDestroyView()
    }

    private fun setListeners() {
        binding.cancelTransferButton.setDebouncingOnClick {
            presenter.cancelTransferButtonClick()
        }
        requireActivity().onBackPressedDispatcher.addCallback(backPressedCallback)
    }

    override fun showCancelTransferDialog() {
        cancelTransferAccountAlertDialog(requireContext()) {
            presenter.stopTransferClick()
        }.show()
    }

    override fun showQrCodeForData(qrCodeContent: String) {
        val bitmap = barcodeEncoder.encodeBitmap(
            qrCodeContent,
            BarcodeFormat.QR_CODE,
            QR_SQUARE_DIM_PX,
            QR_SQUARE_DIM_PX,
            qrCodeGenHints
        )
        binding.qrCode.setImageBitmap(bitmap)
    }

    override fun showCouldNotInitializeTransferParameters() {
        showSnackbar(
            LocalizationR.string.transfer_account_could_not_initialize_parameters,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showCouldNotCreateTransfer(message: String) {
        showSnackbar(
            LocalizationR.string.transfer_account_could_not_create_transfer_format,
            backgroundColor = CoreUiR.color.red,
            messageArgs = arrayOf(message)
        )
    }

    override fun showCouldNotGenerateQrTransferData() {
        showSnackbar(
            LocalizationR.string.transfer_account_could_not_initialize_qr_code_page_data,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showErrorDuringTransferDetailsFetch(message: String) {
        showSnackbar(
            LocalizationR.string.transfer_account_error_during_fetch_transfer_format,
            backgroundColor = CoreUiR.color.red,
            messageArgs = arrayOf(message)
        )
    }

    override fun navigateToResult(result: TransferAccountStatus) {
        findNavController().navigate(
            TransferAccountFragmentDirections.actionTransferAccountToTransferAccountSummaryFragment(result)
        )
    }

    private companion object {
        private const val QR_SQUARE_DIM_PX = 399
    }
}
