package com.passbolt.mobile.android.feature.resourceform.main

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.passbolt.mobile.android.common.dialogs.unableToGeneratePasswordAlertDialog
import com.passbolt.mobile.android.core.extension.gone
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.visible
import com.passbolt.mobile.android.core.navigation.deeplinks.NavDeepLinkProvider
import com.passbolt.mobile.android.core.passwordgenerator.codepoints.Codepoint
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.resourceform.databinding.FragmentResourceFormBinding
import com.passbolt.mobile.android.feature.resourceform.subform.password.PasswordSubformView
import com.passbolt.mobile.android.feature.resourceform.subform.totp.TotpSubformView
import com.passbolt.mobile.android.ui.PasswordStrength
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(binding.toolbar)
        setListeners()
        presenter.attach(this)
        presenter.argsRetrieved(navArgs.mode, navArgs.leadingContentType, navArgs.parentFolderId)
    }

    private fun setListeners() {
        with(binding) {
            viewAdvancedSettings.setDebouncingOnClick {
                viewAdvancedSettings.gone()
                // TODO forward via presenter to decide which fields are visible / possible
                with(additionalSecretsSectionView) {
                    visible()
                    additionalTotpClick = { navigateToTotp() }
                    additionalSecureNoteClick = { navigateToSecureNote() }
                }
                with(metadataSectionView) {
                    visible()
                    descriptionClick = { navigateToMetadataDescription() }
                }
            }
        }
    }

    private fun navigateToMetadataDescription() {
        // TODO listen for results and update model
        findNavController().navigate(
            NavDeepLinkProvider.resourceFormDescriptionDeepLinkRequest(navArgs.mode.name)
        )
    }

    private fun navigateToSecureNote() {
        // TODO listen for results and update model
        findNavController().navigate(
            NavDeepLinkProvider.resourceFormSecureNoteDeepLinkRequest(navArgs.mode.name)
        )
    }

    private fun navigateToTotp() {
        // TODO listen for results and update model
        findNavController().navigate(
            NavDeepLinkProvider.resourceFormTotpSettingsDeepLinkRequest(navArgs.mode.name)
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

    override fun addTotpLeadingForm() {
        val totpSubformView = TotpSubformView(requireContext())
        totpSubformView.moreSettingsClickListener = {
            // TODO listen for result and update model
            findNavController().navigate(
                NavDeepLinkProvider.resourceFormTotpAdvancedSettingsDeepLinkRequest(navArgs.mode.name)
            )
        }
        binding.leadingTypeContainer.addView(totpSubformView)
    }

    override fun addPasswordLeadingForm(
        initialPassword: String,
        initialPasswordStrength: PasswordStrength,
        initialPasswordEntropyBits: Double
    ) {
        PasswordSubformView(requireContext()).apply {
            tag = TAG_PASSWORD_SUBFORM
            passwordGenerateInput.showPassword(initialPassword, initialPasswordStrength, initialPasswordEntropyBits)
            passwordGenerateInput.setGenerateClickListener { presenter.passwordGenerateClick() }
            passwordGenerateInput.setPasswordChangeListener { presenter.passwordTextChanged(it) }
        }.let {
            binding.leadingTypeContainer.addView(it)
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
    }
}
