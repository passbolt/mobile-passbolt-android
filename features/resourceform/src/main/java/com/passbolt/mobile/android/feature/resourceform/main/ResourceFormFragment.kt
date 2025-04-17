package com.passbolt.mobile.android.feature.resourceform.main

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.passbolt.mobile.android.common.dialogs.unableToGeneratePasswordAlertDialog
import com.passbolt.mobile.android.core.extension.gone
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.core.extension.visible
import com.passbolt.mobile.android.core.passwordgenerator.codepoints.Codepoint
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpFragment
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpMode
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password.PasswordFormFragment
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note.NoteFormFragment
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormFragment
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormFragment
import com.passbolt.mobile.android.feature.resourceform.databinding.FragmentResourceFormBinding
import com.passbolt.mobile.android.feature.resourceform.metadata.description.DescriptionFormFragment
import com.passbolt.mobile.android.feature.resourceform.subform.password.PasswordSubformView
import com.passbolt.mobile.android.feature.resourceform.subform.totp.TotpSubformView
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.PasswordStrength
import com.passbolt.mobile.android.ui.PasswordUiModel
import com.passbolt.mobile.android.ui.ResourceFormUiModel
import com.passbolt.mobile.android.ui.TotpUiModel
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

@Suppress("TooManyFunctions")
class ResourceFormFragment :
    BindingScopedAuthenticatedFragment<FragmentResourceFormBinding, ResourceFormContract.View>(
        FragmentResourceFormBinding::inflate
    ), ResourceFormContract.View {

    override val presenter: ResourceFormContract.Presenter by inject()
    private val navArgs: ResourceFormFragmentArgs by navArgs()

    private val noteResult = { _: String, result: Bundle ->
        if (result.containsKey(NoteFormFragment.EXTRA_NOTE)) {
            presenter.noteChanged(result.getString(NoteFormFragment.EXTRA_NOTE))
        }
    }

    private val metadataDescriptionResult = { _: String, result: Bundle ->
        if (result.containsKey(DescriptionFormFragment.EXTRA_METADATA_DESCRIPTION)) {
            presenter.metadataDescriptionChanged(result.getString(DescriptionFormFragment.EXTRA_METADATA_DESCRIPTION))
        }
    }

    private val totpResult = { _: String, result: Bundle ->
        if (result.containsKey(TotpFormFragment.EXTRA_TOTP)) {
            presenter.totpChanged(
                BundleCompat.getParcelable(result, TotpFormFragment.EXTRA_TOTP, TotpUiModel::class.java)
            )
        }
    }

    private val totpAdvancedResult = { _: String, result: Bundle ->
        if (result.containsKey(TotpAdvancedSettingsFormFragment.EXTRA_TOTP_ADVANCED)) {
            presenter.totpAdvancedSettingsChanged(
                BundleCompat.getParcelable(
                    result,
                    TotpAdvancedSettingsFormFragment.EXTRA_TOTP_ADVANCED,
                    TotpUiModel::class.java
                )
            )
        }
    }

    private val passwordResult = { _: String, result: Bundle ->
        if (result.containsKey(PasswordFormFragment.EXTRA_PASSWORD)) {
            presenter.passwordChanged(
                BundleCompat.getParcelable(result, PasswordFormFragment.EXTRA_PASSWORD, PasswordUiModel::class.java)
            )
        }
    }

    private val totpScanQrReturned = { _: String, result: Bundle ->
        presenter.totpScanned(
            result.getBoolean(ScanOtpFragment.EXTRA_MANUAL_CREATION_CHOSEN),
            BundleCompat.getParcelable(
                result,
                ScanOtpFragment.EXTRA_SCANNED_OTP,
                OtpParseResult.OtpQr.TotpQr::class.java
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.resourceName.disableSavingInstanceState()
        initDefaultToolbar(binding.toolbar)
        setListeners()
        presenter.attach(this)
        presenter.argsRetrieved(navArgs.mode)
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    override fun showName(name: String) {
        binding.resourceName.text = name
    }

    private fun setListeners() {
        with(binding) {
            resourceName.setTextChangeListener { presenter.nameTextChanged(it) }
            viewAdvancedSettings.setDebouncingOnClick {
                presenter.advancedSettingsClick()
            }
        }
    }

    override fun hideAdvancedSettings() {
        binding.viewAdvancedSettings.gone()
    }

    override fun setupAdditionalSecrets(supportedAdditionalSecrets: List<ResourceFormUiModel.Secret>) {
        binding.additionalSecretsSectionView.apply {
            visible()
            setUp(supportedAdditionalSecrets)
            additionalPasswordClick = { presenter.additionalPasswordClick() }
            additionalTotpClick = { presenter.additionalTotpClick() }
            additionalNoteClick = { presenter.additionalNoteClick() }
        }
    }

    override fun setupMetadata(supportedMetadata: List<ResourceFormUiModel.Metadata>) {
        binding.metadataSectionView.apply {
            visible()
            setUp(supportedMetadata)
            descriptionClick = { presenter.metadataDescriptionClick() }
        }
    }

    override fun navigateToMetadataDescription(metadataDescription: String) {
        setFragmentResultListener(DescriptionFormFragment.REQUEST_METADATA_DESCRIPTION, metadataDescriptionResult)
        findNavController().navigate(
            ResourceFormFragmentDirections.actionResourceFormFragmentToDescriptionFormFragment(
                navArgs.mode,
                metadataDescription
            )
        )
    }

    override fun navigateToNote(note: String) {
        setFragmentResultListener(NoteFormFragment.REQUEST_NOTE, noteResult)
        findNavController().navigate(
            ResourceFormFragmentDirections.actionResourceFormFragmentToNoteFormFragment(navArgs.mode, note)
        )
    }

    override fun navigateToTotp(totpUiModel: TotpUiModel) {
        setFragmentResultListener(TotpFormFragment.REQUEST_TOTP, totpResult)
        findNavController().navigate(
            ResourceFormFragmentDirections.actionResourceFormFragmentToTotpFormFragment(navArgs.mode, totpUiModel)
        )
    }

    override fun navigateToScanTotp(scanMode: ScanOtpMode) {
        setFragmentResultListener(ScanOtpFragment.REQUEST_SCAN_OTP_FOR_RESULT, totpScanQrReturned)
        findNavController().navigate(
            ResourceFormFragmentDirections.actionResourceFormFragmentToScanOtp(scanMode)
        )
    }

    override fun navigateToPassword(passwordUiModel: PasswordUiModel) {
        setFragmentResultListener(PasswordFormFragment.REQUEST_PASSWORD, passwordResult)
        findNavController().navigate(
            ResourceFormFragmentDirections.actionResourceFormFragmentToPasswordFormFragment(
                navArgs.mode,
                passwordUiModel
            )
        )
    }

    override fun showCreatePasswordTitle() {
        binding.toolbar.toolbarTitle = getString(LocalizationR.string.resource_form_create_password)
    }

    override fun showCreateTotpTitle() {
        binding.toolbar.toolbarTitle = getString(LocalizationR.string.resource_form_create_totp)
    }

    override fun showEditTitle(resourceName: String) {
        binding.toolbar.toolbarTitle = getString(LocalizationR.string.resource_form_edit_resource, resourceName)
    }

    override fun addTotpLeadingForm(totpUiModel: TotpUiModel) {
        TotpSubformView(requireContext()).apply {
            tag = TAG_TOTP_SUBFORM
            isRequired = true
            secretInput.apply {
                disableSavingInstanceState()
                setTextChangeListener {
                    setState(StatefulInput.State.Default)
                    presenter.totpSecretChanged(it)
                }
            }
            urlInput.apply {
                disableSavingInstanceState()
                setTextChangeListener { presenter.totpUrlChanged(it) }
            }
            scanTotpClickListener = { navigateToScanTotp(ScanOtpMode.SCAN_FOR_RESULT) }
            moreSettingsClickListener = {
                setFragmentResultListener(TotpAdvancedSettingsFormFragment.REQUEST_TOTP_ADVANCED, totpAdvancedResult)
                findNavController().navigate(
                    ResourceFormFragmentDirections.actionResourceFormFragmentToTotpAdvancedSettingsFormFragment(
                        navArgs.mode,
                        totpUiModel
                    )
                )
            }
        }.let {
            binding.leadingTypeContainer.addView(it)
        }
    }

    override fun showTotpRequired() {
        binding.root.findViewWithTag<TotpSubformView>(TAG_TOTP_SUBFORM).apply {
            secretInput.setState(
                StatefulInput.State.Error(
                    getString(LocalizationR.string.validation_is_required)
                )
            )
        }
    }

    override fun showTotpSecret(secret: String) {
        binding.leadingTypeContainer.findViewWithTag<TotpSubformView>(TAG_TOTP_SUBFORM).apply {
            secretInput.text = secret
        }
    }

    override fun showTotpIssuer(issuer: String) {
        binding.leadingTypeContainer.findViewWithTag<TotpSubformView>(TAG_TOTP_SUBFORM).apply {
            urlInput.text = issuer
        }
    }

    override fun addPasswordLeadingForm(
        password: String,
        passwordStrength: PasswordStrength,
        passwordEntropyBits: Double
    ) {
        PasswordSubformView(requireContext()).apply {
            tag = TAG_PASSWORD_SUBFORM
            mainUriInput.apply {
                disableSavingInstanceState()
                setTextChangeListener { presenter.passwordMainUriTextChanged(it) }
            }
            usernameInput.apply {
                disableSavingInstanceState()
                setTextChangeListener { presenter.passwordUsernameTextChanged(it) }
            }
            passwordGenerateInput.apply {
                disableSavingInstanceState()
                showPassword(password, passwordStrength, passwordEntropyBits)
                setGenerateClickListener { presenter.passwordGenerateClick() }
                setPasswordChangeListener { presenter.passwordTextChanged(it) }
            }
        }.let {
            binding.leadingTypeContainer.addView(it)
        }
    }

    override fun showPasswordUsername(username: String) {
        binding.leadingTypeContainer.findViewWithTag<PasswordSubformView>(TAG_PASSWORD_SUBFORM).apply {
            usernameInput.text = username
        }
    }

    override fun showPasswordMainUri(mainUri: String) {
        binding.leadingTypeContainer.findViewWithTag<PasswordSubformView>(TAG_PASSWORD_SUBFORM).apply {
            mainUriInput.text = mainUri
        }
    }

    override fun showPassword(password: List<Codepoint>, entropy: Double, passwordStrength: PasswordStrength) {
        val passwordStringBuilder = StringBuilder()
        password.forEach {
            passwordStringBuilder.append(Character.toChars(it.value))
        }

        binding.leadingTypeContainer.findViewWithTag<PasswordSubformView>(TAG_PASSWORD_SUBFORM).apply {
            passwordGenerateInput.showPassword(passwordStringBuilder.toString(), passwordStrength, entropy)
        }
    }

    override fun showPasswordStrength(strength: PasswordStrength, entropyBits: Double) {
        binding.leadingTypeContainer.findViewWithTag<PasswordSubformView>(TAG_PASSWORD_SUBFORM).apply {
            passwordGenerateInput.setPasswordStrength(strength, entropyBits)
        }
    }

    override fun showCreateButton() {
        binding.primaryButton.apply {
            setText(LocalizationR.string.resource_form_create)
            setDebouncingOnClick { presenter.createResourceClick() }
            visible()
        }
    }

    override fun showSaveButton() {
        binding.primaryButton.apply {
            setText(LocalizationR.string.resource_form_save)
            setDebouncingOnClick { presenter.updateResourceClick() }
            visible()
        }
    }

    override fun showUnableToGeneratePassword(minimumEntropyBits: Int) {
        unableToGeneratePasswordAlertDialog(requireContext(), minimumEntropyBits).show()
    }

    override fun showInitializationProgress() {
        binding.fullScreenProgressLayout.visible()
    }

    override fun hideInitializationProgress() {
        binding.fullScreenProgressLayout.gone()
    }

    override fun showEditResourceInitializationError() {
        Toast.makeText(requireContext(), LocalizationR.string.resource_form_edit_init_error, Toast.LENGTH_LONG).show()
    }

    override fun navigateBack() {
        findNavController().popBackStack()
    }

    override fun showProgress() {
        showProgressDialog(childFragmentManager)
    }

    override fun hideProgress() {
        hideProgressDialog(childFragmentManager)
    }

    override fun showJsonResourceSchemaValidationError() {
        showSnackbar(
            LocalizationR.string.common_json_schema_resource_validation_error,
            backgroundColor = R.color.red
        )
    }

    override fun showJsonSecretSchemaValidationError() {
        showSnackbar(
            LocalizationR.string.common_json_schema_secret_validation_error,
            backgroundColor = R.color.red
        )
    }

    override fun showGenericError() {
        showSnackbar(
            LocalizationR.string.common_failure,
            backgroundColor = R.color.red
        )
    }

    override fun showEncryptionError(error: String) {
        showSnackbar(
            LocalizationR.string.common_encryption_failure,
            backgroundColor = R.color.red
        )
    }

    override fun navigateBackWithCreateSuccess(name: String) {
        setFragmentResult(
            REQUEST_RESOURCE_FORM, bundleOf(
                EXTRA_RESOURCE_CREATED to true,
                EXTRA_RESOURCE_NAME to name
            )
        )
        findNavController().popBackStack()
    }

    override fun navigateBackWithEditSuccess(name: String) {
        setFragmentResult(
            REQUEST_RESOURCE_FORM, bundleOf(
                EXTRA_RESOURCE_EDITED to true,
                EXTRA_RESOURCE_NAME to name
            )
        )
        findNavController().popBackStack()
    }

    companion object {
        const val REQUEST_RESOURCE_FORM = "RESOURCE_FORM"

        const val EXTRA_RESOURCE_CREATED = "RESOURCE_CREATED"
        const val EXTRA_RESOURCE_EDITED = "RESOURCE_EDITED"
        const val EXTRA_RESOURCE_NAME = "RESOURCE_NAME"

        private const val TAG_PASSWORD_SUBFORM = "PasswordSubform"
        private const val TAG_TOTP_SUBFORM = "TotpSubform"
    }
}
