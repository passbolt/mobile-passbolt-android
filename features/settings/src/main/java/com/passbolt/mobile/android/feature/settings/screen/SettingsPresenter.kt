package com.passbolt.mobile.android.feature.settings.screen

import com.passbolt.mobile.android.common.autofill.AutofillInformationProvider
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.passphrase.CheckIfPassphraseFileExistsUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.RemovePassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class SettingsPresenter(
    private val checkIfPassphraseExistsUseCase: CheckIfPassphraseFileExistsUseCase,
    private val autofillInfoProvider: AutofillInformationProvider,
    private val removePassphraseUseCase: RemovePassphraseUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val signOutUseCase: SignOutUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : SettingsContract.Presenter {

    override var view: SettingsContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun attach(view: SettingsContract.View) {
        super.attach(view)
        handleAutofillVisibility()
        handleFingerprintSwitchState(view)
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super.detach()
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
        scope.launch {
            signOutUseCase.execute(Unit)
            view?.navigateToAccountList(withSignOut = true)
        }
    }

    override fun manageAccountsClick() {
        view?.navigateToAccountList()
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
            view?.navigateToAuthenticationSignIn()
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
}
