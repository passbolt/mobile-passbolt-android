package com.passbolt.mobile.android.feature.setup.fingerprint

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import com.google.android.material.snackbar.Snackbar
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AuthenticationType
import com.passbolt.mobile.android.feature.autofill.enabled.AutofillEnabledDialog
import com.passbolt.mobile.android.feature.autofill.encourage.EncourageAutofillDialog
import com.passbolt.mobile.android.feature.setup.R
import com.passbolt.mobile.android.feature.setup.databinding.FragmentFingerprintBinding
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

class FingerprintFragment : BindingScopedFragment<FragmentFingerprintBinding>(FragmentFingerprintBinding::inflate),
    FingerprintContract.View, EncourageAutofillDialog.Listener, AutofillEnabledDialog.Listener {

    private val presenter: FingerprintContract.Presenter by inject()
    private val biometricPromptBuilder: BiometricPrompt.PromptInfo.Builder by inject()
    private val executor: Executor by inject()

    override fun onStart() {
        super.onStart()
        presenter.attach(this)
    }

    override fun onStop() {
        presenter.detach()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        presenter.resume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
    }

    private fun setListeners() {
        with(binding) {
            useFingerprintButton.setDebouncingOnClick { presenter.useFingerprintClick() }
            maybeLaterButton.setDebouncingOnClick { presenter.maybeLaterClick() }
        }
    }

    override fun showUseFingerprint() {
        with(binding) {
            icon.setImageResource(R.drawable.ic_use_fingerprint)
            title.text = getString(R.string.fingerprint_setup_use_title)
            description.text = getString(R.string.fingerprint_setup_use_description)
        }
    }

    override fun showConfigureFingerprint() {
        with(binding) {
            icon.setImageResource(R.drawable.ic_configure_fingerprint)
            title.text = getString(R.string.fingerprint_setup_configure_title)
            description.text = getString(R.string.fingerprint_setup_configure_description)
        }
    }

    override fun navigateToSystemSettings() {
        startActivity(Intent(Settings.ACTION_SETTINGS))
    }

    override fun showEncourageAutofillDialog() {
        EncourageAutofillDialog().show(
            childFragmentManager, EncourageAutofillDialog::class.java.name
        )
    }

    override fun showAutofillEnabledDialog() {
        AutofillEnabledDialog().show(
            childFragmentManager, AutofillEnabledDialog::class.java.name
        )
    }

    override fun navigateToHome() {
        startActivity(ActivityIntents.home(requireContext()))
        requireActivity().finish()
    }

    override fun showBiometricPrompt(fingerprintEncryptionCipher: Cipher) {
        val biometricPrompt = BiometricPrompt(
            this, executor, SetupBiometricCallback(
                presenter::authenticationError,
                presenter::authenticationSucceeded
            )
        )

        val promptInfo = biometricPromptBuilder
            .setTitle(getString(R.string.fingerprint_setup_biometric_title))
            .setSubtitle(getString(R.string.fingerprint_setup_biometric_subtitle))
            .setNegativeButtonText(getString(R.string.cancel))
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(fingerprintEncryptionCipher))
    }

    override fun showAuthenticationError(errorMessage: Int) {
        Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG)
            .show()
    }

    override fun setupAutofillLaterClick() {
        presenter.setupAutofillLaterClick()
    }

    override fun autofillSetupSuccessfully() {
        presenter.autofillSetupSuccessfully()
    }

    override fun goToAppClick() {
        presenter.goToTheAppClick()
    }

    private val authenticationResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            presenter.getPassphraseSucceeded()
        }
    }

    override fun startAuthActivity() {
        authenticationResult.launch(
            ActivityIntents.authentication(
                requireContext(),
                AuthenticationType.SignInForResult
            )
        )
    }

    override fun showGenericError() {
        Snackbar.make(binding.root, R.string.common_failure, Snackbar.LENGTH_SHORT)
            .show()
    }

    override fun showKeyChangesDetected() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.fingerprint_biometric_changed_title)
            .setTitle(R.string.fingerprint_authenticate_again)
            .setPositiveButton(R.string.got_it) { _, _ -> presenter.keyChangesInfoConfirmClick() }
            .setCancelable(false)
            .show()
    }
}
