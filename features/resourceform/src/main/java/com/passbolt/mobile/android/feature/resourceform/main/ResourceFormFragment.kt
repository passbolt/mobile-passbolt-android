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
import com.passbolt.mobile.android.feature.metadatakeytrust.ui.NewMetadataKeyTrustDialog
import com.passbolt.mobile.android.feature.metadatakeytrust.ui.NewTrustedMetadataKeyDeletedDialog
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpFragment
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpMode
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note.NoteFormFragment
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password.PasswordFormFragment
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormFragment
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormFragment
import com.passbolt.mobile.android.feature.resourceform.databinding.FragmentResourceFormBinding
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormFragment
import com.passbolt.mobile.android.feature.resourceform.metadata.appearance.AppearanceFormComposeFragment
import com.passbolt.mobile.android.feature.resourceform.metadata.description.DescriptionFormFragment
import com.passbolt.mobile.android.feature.resourceform.subform.password.PasswordSubformView
import com.passbolt.mobile.android.feature.resourceform.subform.totp.TotpSubformView
import com.passbolt.mobile.android.ui.AdditionalUrisUiModel
import com.passbolt.mobile.android.ui.NewMetadataKeyToTrustModel
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.PasswordStrength
import com.passbolt.mobile.android.ui.PasswordUiModel
import com.passbolt.mobile.android.ui.ResourceAppearanceModel
import com.passbolt.mobile.android.ui.ResourceFormUiModel
import com.passbolt.mobile.android.ui.TotpUiModel
import com.passbolt.mobile.android.ui.TrustedKeyDeletedModel
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
        FragmentResourceFormBinding::inflate,
    ),
    ResourceFormContract.View,
    NewMetadataKeyTrustDialog.Listener,
    NewTrustedMetadataKeyDeletedDialog.Listener {
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

    private val appearanceResult = { _: String, result: Bundle ->
        if (result.containsKey(AppearanceFormComposeFragment.EXTRA_APPEARANCE)) {
            presenter.appearanceChanged(
                BundleCompat.getParcelable(
                    result,
                    AppearanceFormComposeFragment.EXTRA_APPEARANCE,
                    ResourceAppearanceModel::class.java,
                ),
            )
        }
    }

    private val additionalUrisResult = { _: String, result: Bundle ->
        if (result.containsKey(AdditionalUrisFormFragment.EXTRA_ADDITIONAL_URIS)) {
            presenter.additionalUrisChanged(
                BundleCompat.getParcelable(
                    result,
                    AdditionalUrisFormFragment.EXTRA_ADDITIONAL_URIS,
                    AdditionalUrisUiModel::class.java,
                ),
            )
        }
    }

    private val totpResult = { _: String, result: Bundle ->
        if (result.containsKey(TotpFormFragment.EXTRA_TOTP)) {
            presenter.totpChanged(
                BundleCompat.getParcelable(result, TotpFormFragment.EXTRA_TOTP, TotpUiModel::class.java),
            )
        }
    }

    private val totpAdvancedResult = { _: String, result: Bundle ->
        if (result.containsKey(TotpAdvancedSettingsFormFragment.EXTRA_TOTP_ADVANCED)) {
            presenter.totpAdvancedSettingsChanged(
                BundleCompat.getParcelable(
                    result,
                    TotpAdvancedSettingsFormFragment.EXTRA_TOTP_ADVANCED,
                    TotpUiModel::class.java,
                ),
            )
        }
    }

    private val passwordResult = { _: String, result: Bundle ->
        if (result.containsKey(PasswordFormFragment.EXTRA_PASSWORD)) {
            presenter.passwordChanged(
                BundleCompat.getParcelable(result, PasswordFormFragment.EXTRA_PASSWORD, PasswordUiModel::class.java),
            )
        }
    }

    private val totpScanQrReturned = { _: String, result: Bundle ->
        presenter.totpScanned(
            result.getBoolean(ScanOtpFragment.EXTRA_MANUAL_CREATION_CHOSEN),
            BundleCompat.getParcelable(
                result,
                ScanOtpFragment.EXTRA_SCANNED_OTP,
                OtpParseResult.OtpQr.TotpQr::class.java,
            ),
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        requiredBinding.resourceName.disableSavingInstanceState()
        initDefaultToolbar(requiredBinding.toolbar)
        setListeners()
        presenter.attach(this)
        presenter.argsRetrieved(navArgs.mode)
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    override fun showName(name: String) {
        requiredBinding.resourceName.text = name
    }

    private fun setListeners() {
        with(requiredBinding) {
            resourceName.setTextChangeListener { presenter.nameTextChanged(it) }
            viewAdvancedSettings.setDebouncingOnClick {
                presenter.advancedSettingsClick()
            }
        }
    }

    override fun hideAdvancedSettings() {
        requiredBinding.viewAdvancedSettings.gone()
    }

    override fun setupAdditionalSecrets(supportedAdditionalSecrets: List<ResourceFormUiModel.Secret>) {
        requiredBinding.additionalSecretsSectionView.apply {
            visible()
            setUp(supportedAdditionalSecrets)
            additionalPasswordClick = { presenter.additionalPasswordClick() }
            additionalTotpClick = { presenter.additionalTotpClick() }
            additionalNoteClick = { presenter.additionalNoteClick() }
        }
    }

    override fun setupMetadata(supportedMetadata: List<ResourceFormUiModel.Metadata>) {
        requiredBinding.metadataSectionView.apply {
            visible()
            setUp(supportedMetadata)
            descriptionClick = { presenter.metadataDescriptionClick() }
            additionalUrisClick = { presenter.additionalUrisClick() }
            appearanceClick = { presenter.appearanceClick() }
        }
    }

    override fun navigateToMetadataDescription(metadataDescription: String) {
        setFragmentResultListener(DescriptionFormFragment.REQUEST_METADATA_DESCRIPTION, metadataDescriptionResult)
        findNavController().navigate(
            ResourceFormFragmentDirections.actionResourceFormFragmentToDescriptionFormFragment(
                navArgs.mode,
                metadataDescription,
            ),
        )
    }

    override fun navigateToAppearance(appearanceModel: ResourceAppearanceModel) {
        setFragmentResultListener(AppearanceFormComposeFragment.REQUEST_APPEARANCE, appearanceResult)
        findNavController().navigate(
            ResourceFormFragmentDirections.actionResourceFormFragmentToAppearanceFormComposeFragment(
                navArgs.mode,
                appearanceModel,
            ),
        )
    }

    override fun navigateToAdditionalUris(model: AdditionalUrisUiModel) {
        setFragmentResultListener(AdditionalUrisFormFragment.REQUEST_ADDITIONAL_URIS, additionalUrisResult)
        findNavController().navigate(
            ResourceFormFragmentDirections.actionResourceFormFragmentToAdditionalUrisFormFragment(
                navArgs.mode,
                model,
            ),
        )
    }

    override fun navigateToNote(note: String) {
        setFragmentResultListener(NoteFormFragment.REQUEST_NOTE, noteResult)
        findNavController().navigate(
            ResourceFormFragmentDirections.actionResourceFormFragmentToNoteFormFragment(navArgs.mode, note),
        )
    }

    override fun navigateToTotp(totpUiModel: TotpUiModel) {
        setFragmentResultListener(TotpFormFragment.REQUEST_TOTP, totpResult)
        findNavController().navigate(
            ResourceFormFragmentDirections.actionResourceFormFragmentToTotpFormFragment(navArgs.mode, totpUiModel),
        )
    }

    override fun navigateToScanTotp(scanMode: ScanOtpMode) {
        setFragmentResultListener(ScanOtpFragment.REQUEST_SCAN_OTP_FOR_RESULT, totpScanQrReturned)
        findNavController().navigate(
            ResourceFormFragmentDirections.actionResourceFormFragmentToScanOtp(scanMode),
        )
    }

    override fun navigateToPassword(passwordUiModel: PasswordUiModel) {
        setFragmentResultListener(PasswordFormFragment.REQUEST_PASSWORD, passwordResult)
        findNavController().navigate(
            ResourceFormFragmentDirections.actionResourceFormFragmentToPasswordFormFragment(
                navArgs.mode,
                passwordUiModel,
            ),
        )
    }

    override fun showCreatePasswordTitle() {
        requiredBinding.toolbar.toolbarTitle = getString(LocalizationR.string.resource_form_create_password)
    }

    override fun showCreateTotpTitle() {
        requiredBinding.toolbar.toolbarTitle = getString(LocalizationR.string.resource_form_create_totp)
    }

    override fun showEditTitle(resourceName: String) {
        requiredBinding.toolbar.toolbarTitle = getString(LocalizationR.string.resource_form_edit_resource, resourceName)
    }

    override fun addTotpLeadingForm(totpUiModel: TotpUiModel) {
        TotpSubformView(requireContext())
            .apply {
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
                            totpUiModel,
                        ),
                    )
                }
            }.let {
                requiredBinding.leadingTypeContainer.addView(it)
            }
    }

    override fun showTotpRequired() {
        requiredBinding.root.findViewWithTag<TotpSubformView>(TAG_TOTP_SUBFORM).apply {
            secretInput.setState(
                StatefulInput.State.Error(
                    getString(LocalizationR.string.validation_is_required),
                ),
            )
        }
    }

    override fun showTotpSecret(secret: String) {
        requiredBinding.leadingTypeContainer.findViewWithTag<TotpSubformView>(TAG_TOTP_SUBFORM).apply {
            secretInput.text = secret
        }
    }

    override fun showTotpIssuer(issuer: String) {
        requiredBinding.leadingTypeContainer.findViewWithTag<TotpSubformView>(TAG_TOTP_SUBFORM).apply {
            urlInput.text = issuer
        }
    }

    override fun addPasswordLeadingForm(
        password: String,
        passwordStrength: PasswordStrength,
        passwordEntropyBits: Double,
    ) {
        PasswordSubformView(requireContext())
            .apply {
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
                requiredBinding.leadingTypeContainer.addView(it)
            }
    }

    override fun showPasswordUsername(username: String) {
        requiredBinding.leadingTypeContainer.findViewWithTag<PasswordSubformView>(TAG_PASSWORD_SUBFORM).apply {
            usernameInput.text = username
        }
    }

    override fun showPasswordMainUri(mainUri: String) {
        requiredBinding.leadingTypeContainer.findViewWithTag<PasswordSubformView>(TAG_PASSWORD_SUBFORM).apply {
            mainUriInput.text = mainUri
        }
    }

    override fun showPassword(
        password: List<Codepoint>,
        entropy: Double,
        passwordStrength: PasswordStrength,
    ) {
        val passwordStringBuilder = StringBuilder()
        password.forEach {
            passwordStringBuilder.append(Character.toChars(it.value))
        }

        requiredBinding.leadingTypeContainer.findViewWithTag<PasswordSubformView>(TAG_PASSWORD_SUBFORM).apply {
            passwordGenerateInput.showPassword(passwordStringBuilder.toString(), passwordStrength, entropy)
        }
    }

    override fun showPasswordStrength(
        strength: PasswordStrength,
        entropyBits: Double,
    ) {
        requiredBinding.leadingTypeContainer.findViewWithTag<PasswordSubformView>(TAG_PASSWORD_SUBFORM).apply {
            passwordGenerateInput.setPasswordStrength(strength, entropyBits)
        }
    }

    override fun showCreateButton() {
        requiredBinding.primaryButton.apply {
            setText(LocalizationR.string.resource_form_create)
            setDebouncingOnClick { presenter.createResourceClick() }
            visible()
        }
    }

    override fun showSaveButton() {
        requiredBinding.primaryButton.apply {
            setText(LocalizationR.string.resource_form_save)
            setDebouncingOnClick { presenter.updateResourceClick() }
            visible()
        }
    }

    override fun showUnableToGeneratePassword(minimumEntropyBits: Int) {
        unableToGeneratePasswordAlertDialog(requireContext(), minimumEntropyBits).show()
    }

    override fun showInitializationProgress() {
        requiredBinding.fullScreenProgressLayout.visible()
    }

    override fun hideInitializationProgress() {
        requiredBinding.fullScreenProgressLayout.gone()
    }

    override fun showEditResourceInitializationError() {
        Toast.makeText(requireContext(), LocalizationR.string.resource_form_edit_init_error, Toast.LENGTH_LONG).show()
    }

    override fun showCannotCreateTotpWithCurrentConfig() {
        showSnackbar(
            messageResId = LocalizationR.string.common_cannot_create_resource_with_current_config,
            backgroundColor = R.color.red,
        )
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
            backgroundColor = R.color.red,
        )
    }

    override fun showJsonSecretSchemaValidationError() {
        showSnackbar(
            LocalizationR.string.common_json_schema_secret_validation_error,
            backgroundColor = R.color.red,
        )
    }

    override fun showGenericError() {
        showSnackbar(
            LocalizationR.string.common_failure,
            backgroundColor = R.color.red,
        )
    }

    override fun showEncryptionError(error: String) {
        showSnackbar(
            LocalizationR.string.common_encryption_failure,
            backgroundColor = R.color.red,
        )
    }

    override fun navigateBackWithCreateSuccess(name: String) {
        setFragmentResult(
            REQUEST_RESOURCE_FORM,
            bundleOf(
                EXTRA_RESOURCE_CREATED to true,
                EXTRA_RESOURCE_NAME to name,
            ),
        )
        findNavController().popBackStack()
    }

    override fun navigateBackWithEditSuccess(name: String) {
        setFragmentResult(
            REQUEST_RESOURCE_FORM,
            bundleOf(
                EXTRA_RESOURCE_EDITED to true,
                EXTRA_RESOURCE_NAME to name,
            ),
        )
        findNavController().popBackStack()
    }

    override fun showCannotUpdateTotpWithCurrentConfig() {
        showSnackbar(
            messageResId = LocalizationR.string.common_cannot_create_resource_with_current_config,
            backgroundColor = R.color.red,
        )
    }

    override fun showMetadataKeyModifiedDialog(model: NewMetadataKeyToTrustModel) {
        NewMetadataKeyTrustDialog
            .newInstance(model)
            .show(childFragmentManager, NewMetadataKeyTrustDialog::class.java.name)
    }

    override fun showMetadataKeyDeletedDialog(model: TrustedKeyDeletedModel) {
        NewTrustedMetadataKeyDeletedDialog
            .newInstance(model)
            .show(childFragmentManager, NewTrustedMetadataKeyDeletedDialog::class.java.name)
    }

    override fun showFailedToVerifyMetadataKey() {
        showSnackbar(
            messageResId = LocalizationR.string.common_metadata_key_verification_failure,
            backgroundColor = R.color.red,
        )
    }

    override fun showNewMetadataKeyIsTrusted() {
        showSnackbar(
            messageResId = LocalizationR.string.common_metadata_key_is_trusted,
            backgroundColor = R.color.green,
        )
    }

    override fun showFailedToTrustMetadataKey() {
        showSnackbar(
            messageResId = LocalizationR.string.common_metadata_key_trust_failed,
            backgroundColor = R.color.red,
        )
    }

    override fun trustNewMetadataKeyClick(newKeyToTrust: NewMetadataKeyToTrustModel) {
        presenter.trustNewMetadataKey(newKeyToTrust)
    }

    override fun trustMetadataKeyDeletionClick(model: TrustedKeyDeletedModel) {
        presenter.trustedMetadataKeyDeleted(model)
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
