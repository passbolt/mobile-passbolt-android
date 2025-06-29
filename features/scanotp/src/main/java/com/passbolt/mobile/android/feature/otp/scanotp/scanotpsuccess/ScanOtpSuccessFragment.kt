package com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess

import android.os.Bundle
import android.view.View
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.core.navigation.deeplinks.NavDeepLinkProvider
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.metadatakeytrust.ui.NewMetadataKeyTrustDialog
import com.passbolt.mobile.android.feature.metadatakeytrust.ui.NewTrustedMetadataKeyDeletedDialog
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpFragment
import com.passbolt.mobile.android.feature.scanotp.R
import com.passbolt.mobile.android.feature.scanotp.databinding.FragmentCreateOtpSuccessBinding
import com.passbolt.mobile.android.resourcepicker.ResourcePickerFragment
import com.passbolt.mobile.android.resourcepicker.ResourcePickerFragment.Companion.RESULT_PICKED_ACTION
import com.passbolt.mobile.android.resourcepicker.ResourcePickerFragment.Companion.RESULT_PICKED_RESOURCE
import com.passbolt.mobile.android.resourcepicker.model.PickResourceAction
import com.passbolt.mobile.android.ui.NewMetadataKeyToTrustModel
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.TrustedKeyDeletedModel
import org.koin.android.ext.android.inject
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
class ScanOtpSuccessFragment :
    BindingScopedAuthenticatedFragment<FragmentCreateOtpSuccessBinding, ScanOtpSuccessContract.View>(
        FragmentCreateOtpSuccessBinding::inflate
    ), ScanOtpSuccessContract.View, NewMetadataKeyTrustDialog.Listener, NewTrustedMetadataKeyDeletedDialog.Listener {

    override val presenter: ScanOtpSuccessContract.Presenter by inject()
    private val navArgs: ScanOtpSuccessFragmentArgs by navArgs()

    private val linkedResourceReceivedListener = { _: String, result: Bundle ->
        if (result.containsKey(RESULT_PICKED_ACTION) && result.containsKey(RESULT_PICKED_RESOURCE)) {
            val action = requireNotNull(
                BundleCompat.getSerializable(result, RESULT_PICKED_ACTION, PickResourceAction::class.java)
            )
            val resource = requireNotNull(
                BundleCompat.getParcelable(result, RESULT_PICKED_RESOURCE, ResourceModel::class.java)
            )
            presenter.linkedResourceReceived(action, resource)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        setListeners()
        presenter.attach(this)
        presenter.argsRetrieved(navArgs.scannedTotp, navArgs.parentFolderId)
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    private fun setupView() {
        with(binding.resultView) {
            setIcon(CoreUiR.drawable.ic_success)
            setTitle(getString(LocalizationR.string.otp_create_success))
            setButtonLabel(getString(LocalizationR.string.otp_create_totp_create_standalone))
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
            messageResId = LocalizationR.string.common_failure,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showError(message: String) {
        showSnackbar(
            messageResId = LocalizationR.string.common_failure_format,
            backgroundColor = CoreUiR.color.red,
            messageArgs = arrayOf(message)
        )
    }

    override fun showCannotUpdateTotpWithCurrentConfig() {
        showSnackbar(
            messageResId = LocalizationR.string.common_cannot_create_resource_with_current_config,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showEncryptionError(message: String) {
        showSnackbar(LocalizationR.string.common_encryption_failure, backgroundColor = CoreUiR.color.red)
    }

    override fun navigateToOtpList(totp: OtpParseResult.OtpQr.TotpQr, otpCreated: Boolean) {
        setFragmentResult(
            ScanOtpFragment.REQUEST_SCAN_OTP_FOR_RESULT,
            bundleOf(
                ScanOtpFragment.EXTRA_SCANNED_OTP to totp,
                EXTRA_OTP_CREATED to otpCreated
            )
        )
        findNavController().popBackStack(destinationId = R.id.scanOtpFragment, true)
    }

    override fun navigateToResourcePicker() {
        setFragmentResultListener(
            ResourcePickerFragment.REQUEST_PICK_RESOURCE_FOR_RESULT,
            linkedResourceReceivedListener
        )
        findNavController().navigate(
            NavDeepLinkProvider.resourceResourcePickerDeepLinkRequest(null)
        )
    }

    override fun showJsonResourceSchemaValidationError() {
        showSnackbar(
            LocalizationR.string.common_json_schema_resource_validation_error,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showJsonSecretSchemaValidationError() {
        showSnackbar(
            LocalizationR.string.common_json_schema_secret_validation_error,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showMetadataKeyModifiedDialog(model: NewMetadataKeyToTrustModel) {
        NewMetadataKeyTrustDialog.newInstance(model)
            .show(childFragmentManager, NewMetadataKeyTrustDialog::class.java.name)
    }

    override fun showMetadataKeyDeletedDialog(model: TrustedKeyDeletedModel) {
        NewTrustedMetadataKeyDeletedDialog.newInstance(model)
            .show(childFragmentManager, NewTrustedMetadataKeyDeletedDialog::class.java.name)
    }

    override fun showFailedToVerifyMetadataKey() {
        showSnackbar(
            messageResId = LocalizationR.string.common_metadata_key_verification_failure,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun trustNewMetadataKeyClick(newKeyToTrust: NewMetadataKeyToTrustModel) {
        presenter.trustNewMetadataKey(newKeyToTrust)
    }

    override fun trustMetadataKeyDeletionClick(model: TrustedKeyDeletedModel) {
        presenter.trustedMetadataKeyDeleted(model)
    }

    override fun showNewMetadataKeyIsTrusted() {
        showSnackbar(
            messageResId = LocalizationR.string.common_metadata_key_is_trusted,
            backgroundColor = CoreUiR.color.green
        )
    }

    override fun showFailedToTrustMetadataKey() {
        showSnackbar(
            messageResId = LocalizationR.string.common_metadata_key_trust_failed,
            backgroundColor = CoreUiR.color.red
        )
    }

    companion object {
        const val EXTRA_OTP_CREATED = "OTP_CREATED"
    }
}
