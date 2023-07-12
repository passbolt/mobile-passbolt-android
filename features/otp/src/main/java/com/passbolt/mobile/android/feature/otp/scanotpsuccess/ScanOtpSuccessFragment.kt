package com.passbolt.mobile.android.feature.otp.scanotpsuccess

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.core.navigation.deeplinks.NavDeepLinkProvider
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.authentication.R
import com.passbolt.mobile.android.feature.otp.databinding.FragmentCreateOtpSuccessBinding
import com.passbolt.mobile.android.resourcepicker.ResourcePickerFragment
import com.passbolt.mobile.android.resourcepicker.ResourcePickerFragment.Companion.RESULT_PICKED_ACTION
import com.passbolt.mobile.android.resourcepicker.ResourcePickerFragment.Companion.RESULT_PICKED_RESOURCE
import com.passbolt.mobile.android.resourcepicker.model.PickResourceAction
import com.passbolt.mobile.android.ui.ResourceModel
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
class ScanOtpSuccessFragment :
    BindingScopedAuthenticatedFragment<FragmentCreateOtpSuccessBinding, ScanOtpSuccessContract.View>(
        FragmentCreateOtpSuccessBinding::inflate
    ), ScanOtpSuccessContract.View {

    override val presenter: ScanOtpSuccessContract.Presenter by inject()
    private val navArgs: ScanOtpSuccessFragmentArgs by navArgs()

    private val linkedResourceReceivedListener = { _: String, result: Bundle ->
        if (result.containsKey(RESULT_PICKED_ACTION) && result.containsKey(RESULT_PICKED_RESOURCE)) {
            val action = result.getSerializable(RESULT_PICKED_ACTION) as PickResourceAction
            val resource = result.getParcelable<ResourceModel>(RESULT_PICKED_RESOURCE)!!
            presenter.linkedResourceReceived(action, resource)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        setListeners()
        presenter.attach(this)
        presenter.argsRetrieved(navArgs.scannedTotp)
    }

    private fun setupView() {
        with(binding.resultView) {
            setIcon(R.drawable.ic_success)
            setTitle(getString(R.string.otp_create_success))
            setButtonLabel(getString(R.string.otp_create_totp_create_standalone))
        }
    }

    private fun setListeners() {
        with(binding) {
            resultView.setButtonAction {
                presenter.createStandaloneOtpClick()
            }
            linkToButton.setDebouncingOnClick {
                presenter.linkToResourceClick()
            }
        }
    }

    override fun showProgress() {
        showProgressDialog(childFragmentManager)
    }

    override fun hideProgress() {
        hideProgressDialog(childFragmentManager)
    }

    override fun showGenericError() {
        showSnackbar(
            messageResId = R.string.common_failure,
            backgroundColor = R.color.red
        )
    }

    override fun showError(message: String) {
        showSnackbar(
            messageResId = R.string.common_failure_format,
            backgroundColor = R.color.red,
            messageArgs = arrayOf(message)
        )
    }

    override fun showEncryptionError(message: String) {
        showSnackbar(R.string.common_encryption_failure, backgroundColor = R.color.red)
    }

    override fun navigateToOtpList(otpCreated: Boolean) {
        setFragmentResult(
            REQUEST_SCAN_OTP,
            bundleOf(EXTRA_OTP_CREATED to otpCreated)
        )
        findNavController().popBackStack()
    }

    override fun navigateToResourcePicker(suggestion: String) {
        setFragmentResultListener(
            ResourcePickerFragment.REQUEST_PICK_RESOURCE_FOR_RESULT,
            linkedResourceReceivedListener
        )
        findNavController().navigate(
            NavDeepLinkProvider.resourceResourcePickerDeepLinkRequest(suggestion)
        )
    }

    companion object {
        const val REQUEST_SCAN_OTP = "SCAN_OTP"
        const val EXTRA_OTP_CREATED = "OTP_CREATED"
    }
}
