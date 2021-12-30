package com.passbolt.mobile.android.feature.authentication.auth

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.snackbar.Snackbar
import com.passbolt.mobile.android.common.extension.gone
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.common.extension.visible
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.hideSoftInput
import com.passbolt.mobile.android.core.logger.helpmenu.HelpMenuFragment
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.security.rootdetection.rootWarningAlertDialog
import com.passbolt.mobile.android.core.ui.dialog.CoreDialogFactory
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.feature.authentication.R
import com.passbolt.mobile.android.feature.authentication.auth.accountdoesnotexist.AccountDoesNotExistDialog
import com.passbolt.mobile.android.feature.authentication.auth.presenter.SignInPresenter
import com.passbolt.mobile.android.feature.authentication.auth.uistrategy.AuthStrategy
import com.passbolt.mobile.android.feature.authentication.auth.uistrategy.AuthStrategyFactory
import com.passbolt.mobile.android.feature.authentication.databinding.FragmentAuthBinding
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpDialog
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpListener
import com.passbolt.mobile.android.feature.authentication.mfa.unknown.UnknownProviderDialog
import com.passbolt.mobile.android.feature.authentication.mfa.youbikey.ScanYubikeyDialog
import com.passbolt.mobile.android.feature.authentication.mfa.youbikey.ScanYubikeyListener
import com.passbolt.mobile.android.featureflags.ui.FeatureFlagsFetchErrorDialog
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
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
class AuthFragment : BindingScopedFragment<FragmentAuthBinding>(FragmentAuthBinding::inflate), AuthContract.View,
    FeatureFlagsFetchErrorDialog.Listener, ServerFingerprintChangedDialog.Listener, AccountDoesNotExistDialog.Listener,
    EnterTotpListener, ScanYubikeyListener, HelpMenuFragment.Listener {

    private val strategyFactory: AuthStrategyFactory by inject()
    private lateinit var authStrategy: AuthStrategy

    private lateinit var presenter: AuthContract.Presenter
    private val biometricPromptBuilder: BiometricPrompt.PromptInfo.Builder by inject()
    private var serverNotReachableDialog: AlertDialog? = null

    private val executor: Executor by inject()
    private var featureFlagsFetchErrorDialog: FeatureFlagsFetchErrorDialog? = null
    private val authConfig by lifecycleAwareLazy {
        requireArguments().getSerializable(EXTRA_AUTH_CONFIG) as ActivityIntents.AuthConfig
    }
    private val context by lifecycleAwareLazy {
        requireArguments().getSerializable(EXTRA_CONTEXT) as AppContext
    }
    private val userId by lifecycleAwareLazy {
        requireNotNull(requireArguments().getString(EXTRA_USER_ID))
    }
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            presenter.backClick(authStrategy.showLeaveConfirmationDialog())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authStrategy = strategyFactory.get(authConfig, context, this)
        presenter = get { parametersOf(authConfig) }
        presenter.argsRetrieved(authConfig, userId)
        presenter.attach(this)
        presenter.viewCreated()
        setListeners()
    }

    private fun setListeners() {
        with(binding) {
            passphraseInput.setIsEmptyListener {
                presenter.passphraseInputIsEmpty(it)
            }
            authButton.setDebouncingOnClick {
                presenter.signInClick(binding.passphraseInput.getInputBytes())
            }
            forgotPasswordButton.setDebouncingOnClick {
                presenter.forgotPasswordClick()
            }
            biometricAuthButton.setDebouncingOnClick {
                presenter.biometricAuthClick()
            }
            with(toolbar) {
                setNavigationIcon(R.drawable.ic_back)
                setNavigationOnClickListener { presenter.backClick(authStrategy.showLeaveConfirmationDialog()) }
            }
            helpButton.setDebouncingOnClick {
                presenter.helpClick()
            }
        }
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, backPressedCallback)
    }

    override fun showAuthenticationReason(reason: AuthContract.View.RefreshAuthReason) {
        with(binding.authReasonLabel) {
            text = getMessageForReason(reason)
            visible()
        }
    }

    private fun getMessageForReason(reason: AuthContract.View.RefreshAuthReason) =
        when (reason) {
            AuthContract.View.RefreshAuthReason.SESSION -> getString(R.string.auth_reason_session_expired)
            AuthContract.View.RefreshAuthReason.PASSPHRASE -> getString(R.string.auth_reason_passphrase_expired)
        }

    override fun setBiometricAuthButtonVisible() {
        binding.biometricAuthButton.visible()
    }

    override fun setBiometricAuthButtonGone() {
        binding.biometricAuthButton.gone()
    }

    override fun showBiometricPrompt(authReason: AuthContract.View.RefreshAuthReason?, fingeprintCipherCrypto: Cipher) {
        val biometricPrompt = BiometricPrompt(
            this, executor, AuthBiometricCallback(
                presenter::biometricAuthError,
                presenter::biometricAuthSuccess
            )
        )

        val promptSubtitle =
            authReason?.let { getMessageForReason(authReason) } ?: getString(R.string.auth_biometric_subtitle)
        val promptInfo = biometricPromptBuilder
            .setTitle(getString(R.string.auth_biometric_title))
            .setSubtitle(promptSubtitle)
            .setNegativeButtonText(getString(R.string.cancel))
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(fingeprintCipherCrypto))
    }

    override fun showAuthenticationError(errorMessage: Int) {
        Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG)
            .show()
    }

    override fun onDestroyView() {
        serverNotReachableDialog?.dismiss()
        serverNotReachableDialog = null
        backPressedCallback.isEnabled = false
        featureFlagsFetchErrorDialog = null
        authStrategy.detach()
        presenter.detach()
        super.onDestroyView()
    }

    override fun showTitle() {
        binding.toolbar.toolbarTitle = authStrategy.title()
    }

    override fun navigateBack() {
        authStrategy.navigateBack()
    }

    override fun authSuccess() {
        authStrategy.authSuccess()
    }

    override fun showWrongPassphrase() {
        Snackbar.make(binding.root, R.string.auth_incorrect_passphrase, Snackbar.LENGTH_LONG)
            .show()
    }

    override fun showGenericError() {
        Snackbar.make(binding.root, R.string.unknown_error, Snackbar.LENGTH_LONG)
            .show()
    }

    override fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .show()
    }

    override fun showFingerprintChangedError() {
        AlertDialog.Builder(requireContext())
            .setCancelable(false)
            .setTitle(R.string.fingerprint_biometric_changed_title)
            .setMessage(R.string.fingerprint_biometric_changed_message)
            .setPositiveButton(R.string.got_it) { _, _ -> }
            .show()
    }

    override fun showServerFingerprintChanged(newFingerprint: String) {
        ServerFingerprintChangedDialog.newInstance(newFingerprint).show(
            childFragmentManager, ServerFingerprintChangedDialog::class.java.name
        )
    }

    override fun showTotpDialog(jwtToken: String, hasYubikeyProvider: Boolean) {
        EnterTotpDialog.newInstance(token = jwtToken, hasYubikeyProvider = hasYubikeyProvider).show(
            childFragmentManager, EnterTotpDialog::class.java.name
        )
    }

    override fun showYubikeyDialog(jwtToken: String, hasTotpProvider: Boolean) {
        ScanYubikeyDialog.newInstance(token = jwtToken, hasTotpProvider = hasTotpProvider).show(
            childFragmentManager, EnterTotpDialog::class.java.name
        )
    }

    override fun changeProviderToTotp(jwtToken: String?) {
        EnterTotpDialog.newInstance(token = jwtToken, hasYubikeyProvider = true).show(
            childFragmentManager, EnterTotpDialog::class.java.name
        )
    }

    override fun showUnknownProvider() {
        UnknownProviderDialog().show(
            childFragmentManager, UnknownProviderDialog::class.java.name
        )
    }

    override fun confirmationClick(fingerprint: String) {
        presenter.fingerprintServerConfirmationClick(fingerprint)
    }

    override fun showProgress() {
        showProgressDialog(childFragmentManager)
    }

    override fun hideProgress() {
        hideProgressDialog(childFragmentManager)
    }

    override fun showLabel(name: String) {
        binding.nameLabel.text = name
    }

    override fun showEmail(email: String) {
        binding.emailLabel.text = email
    }

    override fun showDomain(domain: String) {
        with(binding) {
            domainLabel.text = domain
        }
    }

    override fun showAvatar(url: String) {
        binding.avatarImage.load(url) {
            transformations(CircleCropTransformation())
            error(R.drawable.ic_avatar_placeholder)
            placeholder(R.drawable.ic_avatar_placeholder)
        }
    }

    override fun showForgotPasswordDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.auth_forgot_password_title)
            .setMessage(R.string.auth_forgot_password_message)
            .setPositiveButton(R.string.got_it) { _, _ -> }
            .show()
    }

    override fun showLeaveConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.auth_exit_dialog_title)
            .setMessage(R.string.auth_exit_dialog_message)
            .setPositiveButton(R.string.continue_setup) { _, _ -> }
            .setNegativeButton(R.string.cancel_setup) { _, _ -> presenter.leaveConfirmationClick() }
            .show()
    }

    override fun enableAuthButton() {
        binding.authButton.isEnabled = true
    }

    override fun disableAuthButton() {
        binding.authButton.isEnabled = false
    }

    override fun hideKeyboard() {
        hideSoftInput()
    }

    override fun clearPassphraseInput() {
        binding.passphraseInput.clearText()
    }

    override fun showFeatureFlagsErrorDialog() {
        featureFlagsFetchErrorDialog = FeatureFlagsFetchErrorDialog()
        featureFlagsFetchErrorDialog?.show(childFragmentManager, FeatureFlagsFetchErrorDialog::class.java.name)
    }

    override fun fetchFeatureFlagsErrorDialogRefreshClick() {
        // possible only during sign in
        (presenter as SignInPresenter).refreshClick()
    }

    override fun fetchFeatureFlagsErrorDialogSignOutClick() {
        // possible only during sign in
        (presenter as SignInPresenter).signOutClick()
    }

    override fun closeFeatureFlagsFetchErrorDialog() {
        featureFlagsFetchErrorDialog?.dismiss()
    }

    override fun showAccountDoesNotExistDialog(name: String, email: String?, url: String) {
        AccountDoesNotExistDialog.newInstance(name, email, url)
            .show(childFragmentManager, AccountDoesNotExistDialog::class.java.name)
    }

    override fun connectToExistingAccountClick() {
        presenter.connectToExistingAccountClick()
    }

    override fun navigateToAccountList() {
        findNavController().popBackStack()
    }

    override fun showDecryptionError(message: String?) {
        val errorMessage = StringBuilder(getString(R.string.auth_decryption_error_description))
        if (!message.isNullOrBlank()) {
            errorMessage.append(getString(R.string.auth_decryption_error_cause, message))
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.auth_decryption_error_title)
            .setMessage(errorMessage)
            .setPositiveButton(R.string.got_it) { _, _ -> }
            .show()
    }

    override fun changeProviderToYubikey(bearer: String) {
        ScanYubikeyDialog.newInstance(
            hasTotpProvider = true,
            token = bearer
        ).show(
            childFragmentManager, EnterTotpDialog::class.java.name
        )
    }

    override fun totpVerificationSucceeded(mfaHeader: String?) {
        presenter.totpSucceeded(mfaHeader)
    }

    override fun yubikeyVerificationSucceeded(mfaHeader: String?) {
        presenter.yubikeySucceeded(mfaHeader)
    }

    override fun showServerNotReachable(serverDomain: String) {
        if (serverNotReachableDialog == null) {
            serverNotReachableDialog = CoreDialogFactory.serverNotReachableDialog(requireContext(), serverDomain)
        }
        serverNotReachableDialog?.show()
    }

    override fun showDeviceRootedDialog() {
        rootWarningAlertDialog(requireContext()) { presenter.onRootedDeviceAcknowledged() }
            .show()
    }

    override fun showHelpMenu() {
        HelpMenuFragment.newInstance()
            .show(childFragmentManager, HelpMenuFragment::class.java.name)
    }

    override fun menuShowLogsClick() {
        findNavController().navigate(
            AuthFragmentDirections.actionAuthFragmentToLogsFragment()
        )
    }

    companion object {
        private const val EXTRA_AUTH_CONFIG = "AUTH_CONFIG"
        private const val EXTRA_CONTEXT = "CONTEXT"
        private const val EXTRA_USER_ID = "USER_ID"

        fun newBundle(authConfig: ActivityIntents.AuthConfig, context: AppContext, currentAccount: String) =
            bundleOf(
                EXTRA_AUTH_CONFIG to authConfig,
                EXTRA_USER_ID to currentAccount,
                EXTRA_CONTEXT to context
            )
    }
}
