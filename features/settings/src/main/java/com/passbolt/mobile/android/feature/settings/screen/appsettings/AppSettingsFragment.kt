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

package com.passbolt.mobile.android.feature.settings.screen.appsettings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.passbolt.mobile.android.common.dialogs.configureFingerprintFirstDialog
import com.passbolt.mobile.android.common.dialogs.disableFingerprintConfirmationDialog
import com.passbolt.mobile.android.common.dialogs.keyChangesDetectedAlertDialog
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.feature.authentication.auth.AuthBiometricCallback
import com.passbolt.mobile.android.feature.settings.R
import com.passbolt.mobile.android.feature.settings.databinding.FragmentAppSettingsBinding
import com.passbolt.mobile.android.ui.DefaultFilterModel
import org.koin.android.ext.android.inject
import java.util.concurrent.Executor
import javax.crypto.Cipher

class AppSettingsFragment :
    BindingScopedFragment<FragmentAppSettingsBinding>(FragmentAppSettingsBinding::inflate),
    AppSettingsContract.View {

    private val presenter: AppSettingsContract.Presenter by inject()
    private val biometricPromptBuilder: BiometricPrompt.PromptInfo.Builder by inject()
    private val executor: Executor by inject()
    private val authenticationResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            presenter.getPassphraseSucceeded()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(binding.toolbar)
        setListeners()
        presenter.attach(this)
    }

    override fun onResume() {
        super.onResume()
        presenter.viewResumed()
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
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(fingerprintEncryptionCipher))
    }

    private fun setListeners() {
        with(binding) {
            fingerprintSetting.onChanged = {
                presenter.fingerprintSettingChanged(it)
            }
            autofillSetting.setDebouncingOnClick {
                presenter.autofillClick()
            }
            defaultFilterSetting.setDebouncingOnClick {
                presenter.defaultFilterClick()
            }
        }
    }

    override fun showConfigureFingerprintFirst() {
        configureFingerprintFirstDialog(
            requireContext(),
            confirmAction = { presenter.systemSettingsClick() }
        )
            .show()
    }

    override fun showDisableFingerprintConfirmationDialog() {
        disableFingerprintConfirmationDialog(
            requireContext(),
            confirmAction = { presenter.disableFingerprintConfirmed() },
            cancelAction = { presenter.disableFingerprintCanceled() }
        )
            .show()
    }

    override fun showKeyChangesDetected() {
        keyChangesDetectedAlertDialog(
            requireContext(),
            confirmAction = { presenter.keyChangesInfoConfirmClick() }
        )
            .show()
    }

    override fun showAuthenticationError(@StringRes errorMessage: Int) {
        showSnackbar(
            R.string.common_failure,
            backgroundColor = R.color.red,
            length = Snackbar.LENGTH_LONG
        )
    }

    override fun showBiometryError(message: String?) {
        showSnackbar(
            R.string.settings_app_settings_biometry_error,
            backgroundColor = R.color.red,
            length = Snackbar.LENGTH_LONG,
            messageArgs = if (message != null) arrayOf(message) else emptyArray()
        )
    }

    override fun toggleFingerprintOn(silently: Boolean) {
        binding.fingerprintSetting.turnOn(silently)
    }

    override fun toggleFingerprintOff(silently: Boolean) {
        binding.fingerprintSetting.turnOff(silently)
    }

    override fun navigateToAuthGetPassphrase() {
        authenticationResult.launch(
            ActivityIntents.authentication(requireContext(), ActivityIntents.AuthConfig.RefreshPassphrase)
        )
    }

    override fun navigateToSystemSettings() {
        startActivity(Intent(Settings.ACTION_SETTINGS))
    }

    override fun navigateToDefaultFilterSettings(userSetHomeView: DefaultFilterModel) {
        findNavController().navigate(
            AppSettingsFragmentDirections.actionAppSettingsFragmentToDefaultFilterFragment(userSetHomeView)
        )
    }

    override fun navigateToAutofillSettings() {
        findNavController().navigate(
            AppSettingsFragmentDirections.actionAppSettingsFragmentToSettingsAutofillFragment()
        )
    }
}
