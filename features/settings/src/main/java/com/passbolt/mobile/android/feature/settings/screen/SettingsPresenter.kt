package com.passbolt.mobile.android.feature.settings.screen

import android.security.keystore.KeyPermanentlyInvalidatedException
import com.passbolt.mobile.android.common.FingerprintInformationProvider
import com.passbolt.mobile.android.common.autofill.AutofillInformationProvider
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.cache.passphrase.PotentialPassphrase
import com.passbolt.mobile.android.storage.encrypted.biometric.BiometricCipher
import com.passbolt.mobile.android.storage.usecase.biometrickey.RemoveBiometricKeyUseCase
import com.passbolt.mobile.android.storage.usecase.biometrickey.SaveBiometricKeyIvUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.passphrase.CheckIfPassphraseFileExistsUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.RemovePassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.SavePassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.crypto.Cipher

class SettingsPresenter(
    private val checkIfPassphraseExistsUseCase: CheckIfPassphraseFileExistsUseCase,
    private val autofillInfoProvider: AutofillInformationProvider,
    private val removePassphraseUseCase: RemovePassphraseUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val savePassphraseUseCase: SavePassphraseUseCase,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val biometricCipher: BiometricCipher,
    private val saveBiometricKeyIvUseCase: SaveBiometricKeyIvUseCase,
    private val removeBiometricKeyUseCase: RemoveBiometricKeyUseCase,
    private val fingerprintInformationProvider: FingerprintInformationProvider,
    private val getFeatureFlagsUseCase: GetFeatureFlagsUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : SettingsContract.Presenter {

    override var view: SettingsContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private lateinit var featureFlags: FeatureFlagsModel

    override fun attach(view: SettingsContract.View) {
        super.attach(view)
        handleAutofillVisibility()
        handleFingerprintSwitchState(view)
        handleFeatureFlagsUrls()
    }

    private fun handleFeatureFlagsUrls() {
        scope.launch {
            featureFlags = getFeatureFlagsUseCase.execute(Unit).featureFlags
            if (featureFlags.privacyPolicyUrl.isNullOrBlank()) {
                view?.hidePrivacyPolicyButton()
            }
            if (featureFlags.termsAndConditionsUrl.isNullOrBlank()) {
                view?.hideTermsAndConditionsButton()
            }
        }
    }

    override fun autofillEnabledDialogDismissed() {
        handleAutofillVisibility()
    }

    private fun handleAutofillVisibility() {
        if (!autofillInfoProvider.isPassboltAutofillServiceSet()) {
            view?.showAutofillSetting()
        } else {
            view?.hideAutofillSetting()
        }
    }

    private fun handleFingerprintSwitchState(view: SettingsContract.View) {
        if (checkIfPassphraseExistsUseCase.execute(
                UserIdInput(requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount))
            ).passphraseFileExists
        ) {
            view.toggleFingerprintOn(silently = true)
        } else {
            view.toggleFingerprintOff(silently = true)
        }
    }

    override fun privacyPolicyClick() {
        // if url is null the button is hidden
        view?.openUrl(requireNotNull(featureFlags.privacyPolicyUrl))
    }

    override fun termsClick() {
        // if url is null the button is hidden
        view?.openUrl(requireNotNull(featureFlags.termsAndConditionsUrl))
    }

    override fun signOutClick() {
        view?.showLogoutDialog()
    }

    override fun logoutConfirmed() {
        view?.navigateToSignInWithLogout()
    }

    override fun manageAccountsClick() {
        view?.navigateToAccountListWithLogout()
    }

    override fun autofillClick() {
        view?.showEncourageAutofillDialog()
    }

    override fun autofillSetupSuccessfully() {
        view?.showAutofillEnabledDialog()
    }

    override fun fingerprintSettingChanged(isEnabled: Boolean) {
        if (!isEnabled) {
            view?.showDisableFingerprintConfirmationDialog()
        } else {
            if (fingerprintInformationProvider.hasBiometricSetUp()) {
                if (passphraseMemoryCache.hasPassphrase()) {
                    getPassphraseSucceeded()
                } else {
                    view?.navigateToAuthGetPassphrase()
                }
            } else {
                view?.toggleFingerprintOff(silently = true)
                view?.showConfigureFingerprintFirst()
            }
        }
    }

    override fun systemSettingsClick() {
        view?.navigateToSystemSettings()
    }

    override fun disableFingerprintConfirmed() {
        val selectedAccount = getSelectedAccountUseCase.execute(Unit).selectedAccount
        removePassphraseUseCase.execute(UserIdInput(requireNotNull(selectedAccount)))
        view?.toggleFingerprintOff(silently = true)
    }

    override fun disableFingerprintCanceled() {
        view?.toggleFingerprintOn(silently = true)
    }

    override fun getPassphraseSucceeded() {
        tryShowingBiometricPrompt()
    }

    private fun tryShowingBiometricPrompt() {
        try {
            view?.showBiometricPrompt(biometricCipher.getBiometricEncryptCipher())
        } catch (exception: KeyPermanentlyInvalidatedException) {
            Timber.e(exception)
            removeBiometricKeyUseCase.execute(Unit)
            view?.showKeyChangesDetected()
        } catch (exception: Exception) {
            Timber.e(exception)
            view?.showGenericError()
        }
    }

    override fun biometricAuthError(errorMessage: Int) {
        view?.toggleFingerprintOff(silently = true)
        view?.showAuthenticationError(errorMessage)
    }

    override fun biometricAuthSucceeded(authenticatedCipher: Cipher?) {
        val passphrase = passphraseMemoryCache.get()
        if (passphrase is PotentialPassphrase.Passphrase && authenticatedCipher != null) {
            savePassphraseUseCase.execute(
                SavePassphraseUseCase.Input(passphrase.passphrase, authenticatedCipher)
            )
            saveBiometricKeyIvUseCase.execute(
                SaveBiometricKeyIvUseCase.Input(authenticatedCipher.iv)
            )
        } else {
            Timber.e("Error during turing biometrics on. Passphrase not in cache after auth.")
        }
    }

    override fun biometricAuthCanceled() {
        view?.toggleFingerprintOff(silently = true)
    }

    override fun keyChangesInfoConfirmClick() {
        view?.navigateToAuthGetPassphrase()
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super.detach()
    }
}
