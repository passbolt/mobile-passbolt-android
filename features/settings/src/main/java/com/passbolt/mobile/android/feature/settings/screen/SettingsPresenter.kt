package com.passbolt.mobile.android.feature.settings.screen

import com.passbolt.mobile.android.common.autofill.AutofillInformationProvider
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.cache.passphrase.PotentialPassphrase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.passphrase.CheckIfPassphraseFileExistsUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.RemovePassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.SavePassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import timber.log.Timber

class SettingsPresenter(
    private val checkIfPassphraseExistsUseCase: CheckIfPassphraseFileExistsUseCase,
    private val autofillInfoProvider: AutofillInformationProvider,
    private val removePassphraseUseCase: RemovePassphraseUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val savePassphraseUseCase: SavePassphraseUseCase,
    private val passphraseMemoryCache: PassphraseMemoryCache
) : SettingsContract.Presenter {

    override var view: SettingsContract.View? = null

    override fun attach(view: SettingsContract.View) {
        super.attach(view)
        handleAutofillVisibility()
        handleFingerprintSwitchState(view)
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
        if (checkIfPassphraseExistsUseCase.execute(Unit).passphraseFileExists) {
            view.toggleFingerprintOn(silently = true)
        } else {
            view.toggleFingerprintOff(silently = true)
        }
    }

    override fun privacyPolicyClick() {
        // TODO use url from endpoint
        view?.openUrl("https://www.passbolt.com")
    }

    override fun termsClick() {
        // TODO use url from endpoint
        view?.openUrl("https://www.passbolt.com")
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
            if (passphraseMemoryCache.hasPassphrase()) {
                getPassphraseSucceeded()
            } else {
                view?.navigateToAuthGetPassphrase()
            }
        }
    }

    override fun disableFingerprintConfirmed() {
        val selectedAccount = getSelectedAccountUseCase.execute(Unit).selectedAccount
        removePassphraseUseCase.execute(UserIdInput(selectedAccount))
        view?.toggleFingerprintOff(silently = true)
    }

    override fun disableFingerprintCanceled() {
        view?.toggleFingerprintOn(silently = true)
    }

    override fun getPassphraseSucceeded() {
        view?.showBiometricPrompt()
    }

    override fun biometricAuthError(errorMessage: Int) {
        view?.showAuthenticationError(errorMessage)
    }

    override fun biometricAuthSucceeded() {
        val passphrase = passphraseMemoryCache.get()
        if (passphrase is PotentialPassphrase.Passphrase) {
            savePassphraseUseCase.execute(SavePassphraseUseCase.Input(passphrase.passphrase))
        } else {
            Timber.e("Error during turing biometrics on. Passphrase not in cache after auth.")
        }
    }
}
