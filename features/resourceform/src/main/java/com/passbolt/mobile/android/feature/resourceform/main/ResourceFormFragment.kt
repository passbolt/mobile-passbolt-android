package com.passbolt.mobile.android.feature.resourceform.main

import android.os.Bundle
import android.view.View
import androidx.core.os.BundleCompat
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.passbolt.mobile.android.common.dialogs.unableToGeneratePasswordAlertDialog
import com.passbolt.mobile.android.core.extension.gone
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.visible
import com.passbolt.mobile.android.core.passwordgenerator.codepoints.Codepoint
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.securenote.SecureNoteFormFragment
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormFragment
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormFragment
import com.passbolt.mobile.android.feature.resourceform.databinding.FragmentResourceFormBinding
import com.passbolt.mobile.android.feature.resourceform.metadata.description.DescriptionFormFragment
import com.passbolt.mobile.android.feature.resourceform.subform.password.PasswordSubformView
import com.passbolt.mobile.android.feature.resourceform.subform.totp.TotpSubformView
import com.passbolt.mobile.android.ui.PasswordStrength
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
class ResourceFormFragment :
    BindingScopedAuthenticatedFragment<FragmentResourceFormBinding, ResourceFormContract.View>(
        FragmentResourceFormBinding::inflate
    ), ResourceFormContract.View {

    override val presenter: ResourceFormContract.Presenter by inject()
    private val navArgs: ResourceFormFragmentArgs by navArgs()

    private val secureNoteResult = { _: String, result: Bundle ->
        if (result.containsKey(SecureNoteFormFragment.EXTRA_SECURE_NOTE)) {
            presenter.secureNoteChanged(result.getString(SecureNoteFormFragment.EXTRA_SECURE_NOTE))
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.resourceName.disableSavingInstanceState()
        initDefaultToolbar(binding.toolbar)
        setListeners()
        presenter.attach(this)
        presenter.argsRetrieved(navArgs.mode, navArgs.leadingContentType, navArgs.parentFolderId)
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
            additionalTotpClick = { presenter.additionalTotpClick() }
            additionalSecureNoteClick = { presenter.additionalSecureNoteClick() }
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

    override fun navigateToSecureNote(secureNote: String) {
        setFragmentResultListener(SecureNoteFormFragment.REQUEST_SECURE_NOTE, secureNoteResult)
        findNavController().navigate(
            ResourceFormFragmentDirections.actionResourceFormFragmentToSecureNoteFormFragment(navArgs.mode, secureNote)
        )
    }

    override fun navigateToTotp(totpUiModel: TotpUiModel) {
        setFragmentResultListener(TotpFormFragment.REQUEST_TOTP, totpResult)
        findNavController().navigate(
            ResourceFormFragmentDirections.actionResourceFormFragmentToTotpFormFragment(navArgs.mode, totpUiModel)
        )
    }

    override fun onResume() {
        super.onResume()
        presenter.resume(this)
    }

    override fun onPause() {
        presenter.pause()
        super.onPause()
    }

    override fun showCreatePasswordTitle() {
        binding.toolbar.toolbarTitle = getString(LocalizationR.string.resource_form_create_password)
    }

    override fun showCreateTotpTitle() {
        binding.toolbar.toolbarTitle = getString(LocalizationR.string.resource_form_create_totp)
    }

    override fun addTotpLeadingForm(totpUiModel: TotpUiModel) {
        val totpSubformView = TotpSubformView(requireContext()).apply {
            tag = TAG_TOTP_SUBFORM
            secretInput.apply {
                disableSavingInstanceState()
                setTextChangeListener { presenter.totpSecretChanged(it) }
            }
            urlInput.apply {
                disableSavingInstanceState()
                setTextChangeListener { presenter.totpUrlChanged(it) }
            }
        }
        totpSubformView.moreSettingsClickListener = {
            setFragmentResultListener(TotpAdvancedSettingsFormFragment.REQUEST_TOTP_ADVANCED, totpAdvancedResult)
            findNavController().navigate(
                ResourceFormFragmentDirections.actionResourceFormFragmentToTotpAdvancedSettingsFormFragment(
                    navArgs.mode,
                    totpUiModel
                )
            )
        }
        binding.leadingTypeContainer.addView(totpSubformView)
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
                setTextChangeListener { presenter.passowrdUsernameTextChanged(it) }
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

    override fun hideRefreshProgress() {
//        TODO("Not yet implemented")
    }

    override fun showRefreshProgress() {
//        TODO("Not yet implemented")
    }

    private companion object {
        private const val TAG_PASSWORD_SUBFORM = "PasswordSubform"
        private const val TAG_TOTP_SUBFORM = "TotpSubform"
    }
}
