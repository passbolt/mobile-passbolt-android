package com.passbolt.mobile.android.feature.settings.screen

import android.security.keystore.KeyPermanentlyInvalidatedException
import com.passbolt.mobile.android.common.FingerprintInformationProvider
import com.passbolt.mobile.android.core.logger.FileLoggingTree
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.authentication.auth.usecase.BiometryInteractor
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.cache.passphrase.PotentialPassphrase
import com.passbolt.mobile.android.storage.encrypted.biometric.BiometricCipher
import com.passbolt.mobile.android.storage.usecase.biometrickey.SaveBiometricKeyIvUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.passphrase.CheckIfPassphraseFileExistsUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.RemovePassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.SavePassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.preferences.GetAccountPreferencesUseCase
import com.passbolt.mobile.android.storage.usecase.preferences.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.storage.usecase.preferences.SaveGlobalPreferencesUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.crypto.Cipher

class SettingsPresenter(
    private val checkIfPassphraseExistsUseCase: CheckIfPassphraseFileExistsUseCase,
    private val removePassphraseUseCase: RemovePassphraseUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val savePassphraseUseCase: SavePassphraseUseCase,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val biometricCipher: BiometricCipher,
    private val saveBiometricKeyIvUseCase: SaveBiometricKeyIvUseCase,
    private val fingerprintInformationProvider: FingerprintInformationProvider,
    private val getFeatureFlagsUseCase: GetFeatureFlagsUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase,
    private val saveGlobalPreferencesUseCase: SaveGlobalPreferencesUseCase,
    private val fileLoggingTree: FileLoggingTree,
    private val biometryInteractor: BiometryInteractor,
    private val getAccountPreferencesUseCase: GetAccountPreferencesUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : SettingsContract.Presenter {

    override var view: SettingsContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private lateinit var featureFlags: FeatureFlagsModel

    override fun attach(view: SettingsContract.View) {
        super.attach(view)
        refreshMenuItemsState()
    }

    override fun viewResumed() {
        scope.launch {
            val latestFeatureFlags = getFeatureFlagsUseCase.execute(Unit).featureFlags
            if (latestFeatureFlags != featureFlags) {
                refreshMenuItemsState()
            }
        }
    }

    private fun refreshMenuItemsState() {
        handleFingerprintSwitchState()
        handleFeatureFlagsUrls()
        logSettingChanged(getGlobalPreferencesUseCase.execute(Unit).areDebugLogsEnabled)
    }

    private fun handleFeatureFlagsUrls() {
        scope.launch {
            featureFlags = getFeatureFlagsUseCase.execute(Unit).featureFlags
            if (featureFlags.privacyPolicyUrl.isNullOrBlank()) {
                view?.hidePrivacyPolicyButton()
            } else {
                view?.showPrivacyPolicyButton()
            }
            if (featureFlags.termsAndConditionsUrl.isNullOrBlank()) {
                view?.hideTermsAndConditionsButton()
            } else {
                view?.showTermsAndConditionsButton()
            }
        }
    }

    private fun handleFingerprintSwitchState() {
        if (checkIfPassphraseExistsUseCase.execute(
                UserIdInput(requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount))
            ).passphraseFileExists
        ) {
            view?.toggleFingerprintOn(silently = true)
        } else {
            view?.toggleFingerprintOff(silently = true)
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
        scope.launch {
            view?.showProgress()
            signOutUseCase.execute(Unit)
            view?.hideProgress()
            view?.navigateToSignInWithLogout()
        }
    }

    override fun manageAccountsClick() {
        view?.navigateToManageAccounts()
    }

    override fun autofillClick() {
        view?.navigateToAutofill()
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
            biometryInteractor.disableBiometry()
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

    override fun licensesClick() {
        view?.navigateToLicenses()
    }

    override fun logsClick() {
        view?.navigateToLogs()
    }

    override fun defaultFilterClick() {
        view?.navigateToDefaultFilter(
            getAccountPreferencesUseCase.execute(Unit).userSetHomeView
        )
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super.detach()
    }

    override fun enableDebugLogsChanged(areLogsEnabled: Boolean) {
        logSettingChanged(areLogsEnabled)
    }

    private fun logSettingChanged(areLogsEnabled: Boolean) {
        saveGlobalPreferencesUseCase.execute(SaveGlobalPreferencesUseCase.Input(areLogsEnabled))
        if (areLogsEnabled) {
            view?.apply {
                setEnableLogsSwitchOn()
                enableAccessLogs()
                if (!Timber.forest().contains(fileLoggingTree)) {
                    Timber.plant(fileLoggingTree)
                }
            }
        } else {
            view?.apply {
                setEnableLogsSwitchOff()
                disableAccessLogs()
                if (Timber.forest().contains(fileLoggingTree)) {
                    Timber.uproot(fileLoggingTree)
                }
            }
        }
    }
}
