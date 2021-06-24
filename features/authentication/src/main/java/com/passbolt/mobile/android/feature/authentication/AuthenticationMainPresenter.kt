package com.passbolt.mobile.android.feature.authentication

import com.passbolt.mobile.android.storage.usecase.GetSelectedAccountUseCase
import timber.log.Timber

class AuthenticationMainPresenter(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase
) : AuthenticationMainContract.Presenter {

    override var view: AuthenticationMainContract.View? = null

    override fun bundleRetrieved(authenticationStrategy: AuthenticationType) {
        when (authenticationStrategy) {
            AuthenticationType.PASSPHRASE -> {
                val selectedAccount = getSelectedAccountUseCase.execute(Unit).selectedAccount
                view?.navigateToAuth(selectedAccount, authenticationStrategy)
            }
            AuthenticationType.SIGN_IN -> {
                runCatching {
                    getSelectedAccountUseCase.execute(Unit).selectedAccount
                }
                    .onSuccess { view?.navigateToAuth(it, authenticationStrategy) }
                    .onFailure {
                        Timber.d(it)
                        // no account selected - remain on account list
                    }
            }
        }
    }
}
