package com.passbolt.mobile.android.feature.settings.screen

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.passbolt.mobile.android.common.WebsiteOpener
import com.passbolt.mobile.android.common.extension.gone
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.common.extension.visible
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.feature.authentication.auth.AuthBiometricCallback
import com.passbolt.mobile.android.feature.autofill.enabled.AutofillEnabledDialog
import com.passbolt.mobile.android.feature.settings.R
import com.passbolt.mobile.android.feature.settings.databinding.FragmentSettingsBinding
import org.koin.android.ext.android.inject
import java.util.concurrent.Executor
import javax.crypto.Cipher

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
class SettingsFragment : BindingScopedFragment<FragmentSettingsBinding>(FragmentSettingsBinding::inflate),
    SettingsContract.View, AutofillEnabledDialog.Listener {

    private val presenter: SettingsContract.Presenter by inject()
    private val websiteOpener: WebsiteOpener by inject()
    private val authenticationResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            presenter.getPassphraseSucceeded()
        }
    }
    private val biometricPromptBuilder: BiometricPrompt.PromptInfo.Builder by inject()
    private val executor: Executor by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
    }

    override fun onResume() {
        super.onResume()
        presenter.attach(this)
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    private fun setListeners() {
        with(binding) {
            fingerprintSetting.onChanged = {
                presenter.fingerprintSettingChanged(it)
            }
            autofillSetting.setDebouncingOnClick {
                presenter.autofillClick()
            }
            manageAccountsSetting.setDebouncingOnClick {
                presenter.manageAccountsClick()
            }
            termsSetting.setDebouncingOnClick {
                presenter.termsClick()
            }
            privacySetting.setDebouncingOnClick {
                presenter.privacyPolicyClick()
            }
            signOutSetting.setDebouncingOnClick {
                presenter.signOutClick()
            }
        }
    }

    override fun toggleFingerprintOn(silently: Boolean) {
        binding.fingerprintSetting.turnOn(silently)
    }

    override fun toggleFingerprintOff(silently: Boolean) {
        binding.fingerprintSetting.turnOff(silently)
    }

    override fun showAutofillSetting() {
        binding.autofillSetting.visible()
    }

    override fun hideAutofillSetting() {
        binding.autofillSetting.gone()
    }

    override fun openUrl(url: String) {
        websiteOpener.open(requireContext(), url)
    }

    override fun navigateToAutofill() {
        findNavController().navigate(
            SettingsFragmentDirections.actionSettingsToSettingsAutofillFragment()
        )
    }

    override fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_logout_dialog_title)
            .setMessage(R.string.settings_logout_dialog_message)
            .setPositiveButton(R.string.settings_logout_dialog_sign_out) { _, _ -> presenter.logoutConfirmed() }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .show()
    }

    override fun showAutofillEnabledDialog() {
        AutofillEnabledDialog().show(
            childFragmentManager, AutofillEnabledDialog::class.java.name
        )
    }

    override fun goToAppClick() {
        // no action - dialog closed
    }

    override fun navigateToManageAccounts() {
        startActivity(ActivityIntents.authentication(requireContext(), ActivityIntents.AuthConfig.ManageAccount))
    }

    override fun navigateToSignInWithLogout() {
        startActivity(
            ActivityIntents.authentication(
                requireContext(),
                ActivityIntents.AuthConfig.Startup
            )
        )
        requireActivity().finish()
    }

    override fun showDisableFingerprintConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_disable_fingerprint_confirmation_title)
            .setMessage(R.string.settings_disable_fingerprint_confirmation_message)
            .setPositiveButton(R.string.settings_disable) { _, _ -> presenter.disableFingerprintConfirmed() }
            .setNegativeButton(R.string.cancel) { _, _ -> presenter.disableFingerprintCanceled() }
            .show()
    }

    override fun navigateToAuthGetPassphrase() {
        authenticationResult.launch(
            ActivityIntents.authentication(requireContext(), ActivityIntents.AuthConfig.RefreshPassphrase)
        )
    }

    override fun showBiometricPrompt(fingerprintEncryptionCipher: Cipher) {
        val biometricPrompt = BiometricPrompt(
            this, executor, AuthBiometricCallback(
                presenter::biometricAuthError,
                presenter::biometricAuthSucceeded,
                presenter::biometricAuthCanceled
            )
        )

        val promptInfo = biometricPromptBuilder
            .setTitle(getString(R.string.settings_turn_on_biometric_title))
            .setSubtitle(getString(R.string.settings_turn_on_biometric_subtitle))
            .setNegativeButtonText(getString(R.string.cancel))
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(fingerprintEncryptionCipher))
    }

    override fun showAuthenticationError(errorMessage: Int) {
        Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG)
            .show()
    }

    override fun showGenericError() {
        Snackbar.make(binding.root, R.string.common_failure, Snackbar.LENGTH_SHORT)
            .show()
    }

    override fun showConfigureFingerprintFirst() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_add_first_fingerprint_title)
            .setMessage(R.string.settings_add_first_fingerprint)
            .setPositiveButton(
                R.string.settings_add_first_fingerprint_settings
            ) { _, _ -> presenter.systemSettingsClick() }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .setCancelable(false)
            .show()
    }

    override fun navigateToSystemSettings() {
        startActivity(Intent(Settings.ACTION_SETTINGS))
    }

    override fun showKeyChangesDetected() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.fingerprint_biometric_changed_title)
            .setMessage(R.string.fingerprint_authenticate_again)
            .setPositiveButton(R.string.got_it) { _, _ -> presenter.keyChangesInfoConfirmClick() }
            .setCancelable(false)
            .show()
    }

    override fun hidePrivacyPolicyButton() {
        binding.privacySetting.gone()
    }

    override fun hideTermsAndConditionsButton() {
        binding.termsSetting.gone()
    }

    override fun showProgress() {
        showProgressDialog(childFragmentManager)
    }

    override fun hideProgress() {
        hideProgressDialog(childFragmentManager)
    }
}
