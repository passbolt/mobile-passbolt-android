package com.passbolt.mobile.android.feature.login

import com.passbolt.mobile.android.storage.usecase.GetSelectedAccountUseCase
import timber.log.Timber

class LoginMainPresenter(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase
) : LoginMainContract.Presenter {

    override var view: LoginMainContract.View? = null

    override fun attach(view: LoginMainContract.View) {
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
