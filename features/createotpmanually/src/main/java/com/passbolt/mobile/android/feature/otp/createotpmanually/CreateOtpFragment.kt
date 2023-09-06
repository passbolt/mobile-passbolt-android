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

package com.passbolt.mobile.android.feature.otp.createotpmanually

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.passbolt.mobile.android.common.extension.gone
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.common.extension.visible
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.core.navigation.deeplinks.NavDeepLinkProvider
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Error
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.createotpmanually.R
import com.passbolt.mobile.android.feature.createotpmanually.databinding.FragmentCreateOtpBinding
import com.passbolt.mobile.android.feature.otp.createotpmanuallyexpertsettings.CreateOtpAdvancedSettingsFragment
import com.passbolt.mobile.android.resourcepicker.ResourcePickerFragment
import com.passbolt.mobile.android.resourcepicker.ResourcePickerFragment.Companion.RESULT_PICKED_ACTION
import com.passbolt.mobile.android.resourcepicker.ResourcePickerFragment.Companion.RESULT_PICKED_RESOURCE
import com.passbolt.mobile.android.resourcepicker.model.PickResourceAction
import com.passbolt.mobile.android.ui.OtpAdvancedSettingsModel
import com.passbolt.mobile.android.ui.ResourceModel
import org.koin.android.ext.android.inject

class CreateOtpFragment :
    BindingScopedAuthenticatedFragment<FragmentCreateOtpBinding, CreateOtpContract.View>(
        FragmentCreateOtpBinding::inflate
    ),
    CreateOtpContract.View {

    override val presenter: CreateOtpContract.Presenter by inject()
    private val args: CreateOtpFragmentArgs by navArgs()

    private val otpSettingsModifiedListener = { _: String, bundle: Bundle ->
        val algorithm = requireNotNull(bundle.getString(CreateOtpAdvancedSettingsFragment.EXTRA_ALGORITHM))
        val period = requireNotNull(bundle.getLong(CreateOtpAdvancedSettingsFragment.EXTRA_PERIOD))
        val digits = requireNotNull(bundle.getInt(CreateOtpAdvancedSettingsFragment.EXTRA_DIGITS))
        presenter.otpSettingsModified(algorithm, period, digits)
    }

    private val linkedResourceReceivedListener = { _: String, result: Bundle ->
        if (result.containsKey(RESULT_PICKED_ACTION) && result.containsKey(RESULT_PICKED_RESOURCE)) {
            val action = result.getSerializable(RESULT_PICKED_ACTION) as PickResourceAction
            val resource = result.getParcelable<ResourceModel>(RESULT_PICKED_RESOURCE)!!
            presenter.linkedResourceReceived(action, resource)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        disableSaveInstanceStateForInputs()
        setListeners()
        initDefaultToolbar(binding.toolbar)
        presenter.attach(this)
        presenter.argsRetrieved(args.editedOtpResourceId)
    }

    override fun setupEditUi() {
        with(binding) {
            toolbar.toolbarTitle = getString(R.string.otp_edit_title)
            mainButton.text = getString(R.string.save)
            linkToButton.gone()
        }
    }

    override fun setupCreateUi() {
        with(binding) {
            toolbar.toolbarTitle = getString(R.string.otp_create_totp_title)
            mainButton.text = getString(R.string.otp_create_totp_create_standalone)
            linkToButton.visible()
        }
    }

    private fun disableSaveInstanceStateForInputs() {
        setOf(binding.totpLabelInput, binding.totpIssuerInput, binding.totpSecretInput)
            .forEach { it.disableSavingInstanceState() }
    }

    private fun setListeners() {
        with(binding) {
            totpLabelInput.setTextChangeListener {
                presenter.totpLabelChanged(it)
            }
            totpIssuerInput.setTextChangeListener {
                presenter.totpIssuerChanged(it)
            }
            totpSecretInput.setTextChangeListener {
                presenter.totpSecretChanged(it)
            }
            advancedSettings.setDebouncingOnClick {
                presenter.advancedSettingsClick()
            }
            mainButton.setDebouncingOnClick {
                presenter.mainButtonClick()
            }
            linkToButton.setDebouncingOnClick {
                presenter.linkToResourceClick()
            }
        }
    }

    override fun setFormValues(label: String, issuer: String, secret: String) {
        with(binding) {
            totpLabelInput.text = label
            totpIssuerInput.text = issuer
            totpSecretInput.text = secret
        }
    }

    override fun showError(message: String) {
        showSnackbar(getString(R.string.common_failure_format, message), backgroundColor = R.color.red)
    }

    override fun navigateToCreateOtpAdvancedSettings(advancedSettingsModel: OtpAdvancedSettingsModel) {
        setFragmentResultListener(
            CreateOtpAdvancedSettingsFragment.REQUEST_MODIFY_TOTP_SETTINGS,
            otpSettingsModifiedListener
        )
        findNavController().navigate(
            CreateOtpFragmentDirections.actionCreateOtpManuallyFragmentToCreateOtpAdvancedSettingsFragment(
                advancedSettingsModel
            )
        )
    }

    override fun showLabelValidationError(maxLength: Int) {
        binding.totpLabelInput.setState(
            Error(getString(R.string.validation_required_with_max_length, maxLength))
        )
    }

    override fun showSecretValidationError(maxLength: Int) {
        binding.totpSecretInput.setState(
            Error(getString(R.string.validation_required_with_max_length, maxLength))
        )
    }

    override fun showIssuerValidationError(maxLength: Int) {
        binding.totpIssuerInput.setState(
            Error(getString(R.string.validation_required_with_max_length, maxLength))
        )
    }

    override fun showProgress() {
        showProgressDialog(childFragmentManager)
    }

    override fun hideProgress() {
        hideProgressDialog(childFragmentManager)
    }

    override fun showGenericError() {
        showSnackbar(
            R.string.common_failure,
            backgroundColor = R.color.red
        )
    }

    override fun showEncryptionError(message: String) {
        showSnackbar(
            R.string.common_encryption_failure,
            backgroundColor = R.color.red
        )
    }

    override fun navigateBackInCreateFlow(resourceName: String, otpCreated: Boolean) {
        setFragmentResult(
            REQUEST_CREATE_OTP,
            bundleOf(
                EXTRA_OTP_CREATED to otpCreated,
                EXTRA_RESOURCE_NAME to resourceName
            )
        )
        findNavController().popBackStack()
    }

    override fun navigateBackInUpdateFlow(resourceName: String, otpUpdated: Boolean) {
        setFragmentResult(
            REQUEST_UPDATE_OTP,
            bundleOf(
                EXTRA_OTP_UPDATED to otpUpdated,
                EXTRA_RESOURCE_NAME to resourceName
            )
        )
        findNavController().popBackStack()
    }

    override fun setValues(label: String, issuer: String, secret: String) {
        with(binding) {
            totpLabelInput.text = label
            totpIssuerInput.text = issuer
            totpSecretInput.text = secret
        }
    }

    override fun navigateToResourcePicker(suggestionUri: String) {
        setFragmentResultListener(
            ResourcePickerFragment.REQUEST_PICK_RESOURCE_FOR_RESULT,
            linkedResourceReceivedListener
        )
        findNavController().navigate(
            NavDeepLinkProvider.resourceResourcePickerDeepLinkRequest(suggestionUri)
        )
    }

    override fun showEditingValuesAlsoEditsResourceValuesWarning() {
        binding.alsoEditsResourceWarning.visible()
    }

    override fun showDecryptionError() {
        showSnackbar(
            messageResId = R.string.common_decryption_failure,
            backgroundColor = R.color.red
        )
    }

    override fun showFetchError() {
        showSnackbar(
            messageResId = R.string.common_fetch_failure,
            backgroundColor = R.color.red
        )
    }

    companion object {
        const val REQUEST_CREATE_OTP = "CREATE_OTP"
        const val EXTRA_OTP_CREATED = "OTP_CREATED"
        const val REQUEST_UPDATE_OTP = "UPDATE_OTP"
        const val EXTRA_OTP_UPDATED = "OTP_UPDATED"
        const val EXTRA_RESOURCE_NAME = "RESOURCE_NAME"
    }
}
