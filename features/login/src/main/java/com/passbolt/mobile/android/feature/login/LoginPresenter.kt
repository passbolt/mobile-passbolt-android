package com.passbolt.mobile.android.feature.login

import com.passbolt.mobile.android.storage.usecase.GetSelectedAccountUseCase
import timber.log.Timber

class LoginPresenter(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase
) : LoginContract.Presenter {

    override var view: LoginContract.View? = null

    override fun attach(view: LoginContract.View) {
        super.attach(view)
        runCatching {
            getSelectedAccountUseCase.execute(Unit).selectedAccount
        }
            .onSuccess { view.navigateToAccountLogin(it) }
            .onFailure {
                Timber.d(it)
                // no account selected - remain on account list
            }
    }
}
