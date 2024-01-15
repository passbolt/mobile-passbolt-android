package com.passbolt.mobile.android.feature.authentication.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.snackbar.Snackbar
import com.passbolt.mobile.android.common.dialogs.rootWarningAlertDialog
import com.passbolt.mobile.android.common.dialogs.serverNotReachableAlertDialog
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.gone
import com.passbolt.mobile.android.core.extension.hideSoftInput
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.core.extension.visible
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider.DUO
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider.TOTP
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider.YUBIKEY
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.feature.authentication.auth.accountdoesnotexist.AccountDoesNotExistDialog
import com.passbolt.mobile.android.feature.authentication.auth.presenter.SignInPresenter
import com.passbolt.mobile.android.feature.authentication.auth.uistrategy.AuthStrategy
import com.passbolt.mobile.android.feature.authentication.auth.uistrategy.AuthStrategyFactory
import com.passbolt.mobile.android.feature.authentication.databinding.FragmentAuthBinding
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoDialog
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoListener
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpDialog
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpListener
import com.passbolt.mobile.android.feature.authentication.mfa.unknown.UnknownProviderDialog
import com.passbolt.mobile.android.feature.authentication.mfa.youbikey.ScanYubikeyDialog
import com.passbolt.mobile.android.feature.authentication.mfa.youbikey.ScanYubikeyListener
import com.passbolt.mobile.android.featureflagserror.FeatureFlagsFetchErrorDialog
import com.passbolt.mobile.android.helpmenu.HelpMenuFragment
import com.passbolt.mobile.android.ui.HelpMenuModel
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import java.util.concurrent.Executor
import javax.crypto.Cipher
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
@Suppress("TooManyFunctions")
class AuthFragment : BindingScopedFragment<FragmentAuthBinding>(FragmentAuthBinding::inflate), AuthContract.View,
    FeatureFlagsFetchErrorDialog.Listener, ServerFingerprintChangedDialog.Listener, AccountDoesNotExistDialog.Listener,
    EnterTotpListener, ScanYubikeyListener, AuthWithDuoListener, HelpMenuFragment.Listener {

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
                setNavigationIcon(CoreUiR.drawable.ic_back)
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
            AuthContract.View.RefreshAuthReason.SESSION ->
                getString(LocalizationR.string.auth_reason_session_expired)
            AuthContract.View.RefreshAuthReason.PASSPHRASE ->
                getString(LocalizationR.string.auth_reason_passphrase_expired)
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
            authReason?.let { getMessageForReason(authReason) } ?: ""
        val promptInfo = biometricPromptBuilder
            .setTitle(getString(LocalizationR.string.auth_biometric_title))
            .setSubtitle(promptSubtitle)
            .setNegativeButtonText(getString(LocalizationR.string.cancel))
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(fingeprintCipherCrypto))
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

    override fun showAuthenticationError(@StringRes errorMessage: Int) {
        showSnackbar(
            errorMessage,
            length = Snackbar.LENGTH_LONG,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showWrongPassphrase() {
        showSnackbar(
            LocalizationR.string.auth_incorrect_passphrase,
            length = Snackbar.LENGTH_LONG,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showGenericError() {
        showSnackbar(
            LocalizationR.string.common_failure,
            length = Snackbar.LENGTH_LONG,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showChallengeInvalidSignature() {
        showSnackbar(
            LocalizationR.string.auth_error_invalid_signature,
            length = Snackbar.LENGTH_LONG,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showChallengeTokenExpired() {
        showSnackbar(
            LocalizationR.string.auth_error_token_expired,
            length = Snackbar.LENGTH_LONG,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showChallengeVerificationFailure() {
        showSnackbar(
            LocalizationR.string.auth_error_challenge_verification_failure,
            length = Snackbar.LENGTH_LONG,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showFailedToFetchUserProfile(message: String?) {
        val errorMessage = StringBuilder(getString(LocalizationR.string.auth_error_profile_fetch_failure))
        if (!message.isNullOrBlank()) {
            errorMessage.append("(%s)".format(message))
        }
        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
    }

    override fun showError(message: String) {
        showSnackbar(
            message, length = Snackbar.LENGTH_LONG,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showTimeIsOutOfSync() {
        showSnackbar(
            LocalizationR.string.common_time_is_out_of_sync,
            length = Snackbar.LENGTH_LONG,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showFingerprintChangedError() {
        AlertDialog.Builder(requireContext())
            .setCancelable(false)
            .setTitle(LocalizationR.string.fingerprint_biometric_changed_title)
            .setMessage(LocalizationR.string.fingerprint_biometric_changed_message)
            .setPositiveButton(LocalizationR.string.got_it) { _, _ -> }
            .show()
    }

    override fun showServerFingerprintChanged(newFingerprint: String) {
        ServerFingerprintChangedDialog.newInstance(newFingerprint).show(
            childFragmentManager, ServerFingerprintChangedDialog::class.java.name
        )
    }

    override fun showTotpDialog(jwtToken: String?, hasOtherProviders: Boolean) {
        EnterTotpDialog.newInstance(token = jwtToken, hasOtherProvider = hasOtherProviders).show(
            childFragmentManager, EnterTotpDialog::class.java.name
        )
    }

    override fun showDuoDialog(jwtToken: String?, hasOtherProviders: Boolean) {
        AuthWithDuoDialog.newInstance(token = jwtToken, hasOtherProvider = hasOtherProviders).show(
            childFragmentManager, AuthWithDuoDialog::class.java.name
        )
    }

    override fun showYubikeyDialog(jwtToken: String?, hasOtherProviders: Boolean) {
        ScanYubikeyDialog.newInstance(token = jwtToken, hasOtherProvider = hasOtherProviders).show(
            childFragmentManager, ScanYubikeyDialog::class.java.name
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
            error(CoreUiR.drawable.ic_avatar_placeholder)
            placeholder(CoreUiR.drawable.ic_avatar_placeholder)
        }
    }

    override fun showForgotPasswordDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(LocalizationR.string.auth_forgot_password_title)
            .setMessage(LocalizationR.string.auth_forgot_password_message)
            .setPositiveButton(LocalizationR.string.got_it) { _, _ -> }
            .show()
    }

    override fun showLeaveConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(LocalizationR.string.are_you_sure)
            .setMessage(LocalizationR.string.auth_exit_dialog_message)
            .setPositiveButton(LocalizationR.string.continue_setup) { _, _ -> }
            .setNegativeButton(LocalizationR.string.cancel_setup) { _, _ -> presenter.leaveConfirmationClick() }
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

    override fun showAccountDoesNotExistDialog(label: String, email: String?, url: String) {
        AccountDoesNotExistDialog.newInstance(label, email, url)
            .show(childFragmentManager, AccountDoesNotExistDialog::class.java.name)
    }

    override fun connectToExistingAccountClick() {
        presenter.connectToExistingAccountClick()
    }

    override fun navigateToAccountList() {
        findNavController().popBackStack()
    }

    override fun showDecryptionError(message: String?) {
        val errorMessage = StringBuilder(getString(LocalizationR.string.auth_decryption_error_description))
        if (!message.isNullOrBlank()) {
            errorMessage.append(getString(LocalizationR.string.auth_decryption_error_cause, message))
        }

        AlertDialog.Builder(requireContext())
            .setTitle(LocalizationR.string.auth_decryption_error_title)
            .setMessage(errorMessage)
            .setPositiveButton(LocalizationR.string.got_it) { _, _ -> }
            .show()
    }

    override fun totpOtherProviderClick(bearer: String) {
        presenter.otherProviderClick(bearer, TOTP)
    }

    override fun yubikeyOtherProviderClick(jwtToken: String?) {
        presenter.otherProviderClick(jwtToken, YUBIKEY)
    }

    override fun duoOtherProviderClick(jwtToken: String?) {
        presenter.otherProviderClick(jwtToken, DUO)
    }

    override fun totpVerificationSucceeded(mfaHeader: String?) {
        presenter.mfaSucceeded(mfaHeader)
    }

    override fun yubikeyVerificationSucceeded(mfaHeader: String?) {
        presenter.mfaSucceeded(mfaHeader)
    }

    override fun duoAuthSucceeded(mfaHeader: String?) {
        presenter.mfaSucceeded(mfaHeader)
    }

    override fun showServerNotReachable(serverDomain: String) {
        if (serverNotReachableDialog == null) {
            serverNotReachableDialog = serverNotReachableAlertDialog(requireContext(), serverDomain)
        }
        serverNotReachableDialog?.show()
    }

    override fun showDeviceRootedDialog() {
        rootWarningAlertDialog(requireContext()) { presenter.onRootedDeviceAcknowledged() }
            .show()
    }

    override fun showHelpMenu() {
        HelpMenuFragment.newInstance(
            HelpMenuModel(
                shouldShowShowQrCodesHelp = false,
                shouldShowImportProfile = false,
                shouldShowImportAccountKit = false
            )
        )
            .show(childFragmentManager, HelpMenuFragment::class.java.name)
    }

    override fun menuShowLogsClick() {
        findNavController().navigate(
            AuthFragmentDirections.actionAuthFragmentToLogs()
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
