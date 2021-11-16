package com.passbolt.mobile.android.feature.authentication

import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.storage.usecase.accountdata.GetAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.SaveCurrentApiUrlUseCase

class AuthenticationMainPresenter(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val getAccountDataUseCase: GetAccountDataUseCase,
    private val saveCurrentApiUrlUseCase: SaveCurrentApiUrlUseCase
) : AuthenticationMainContract.Presenter {

    override var view: AuthenticationMainContract.View? = null

    override fun bundleRetrieved(authConfig: ActivityIntents.AuthConfig, userId: String?) {
        val currentAccount = userId ?: getSelectedAccountUseCase.execute(Unit).selectedAccount

        if (authConfig is ActivityIntents.AuthConfig.Setup && currentAccount != null) {
            view?.initNavWithoutAccountList(currentAccount)
        } else {
            view?.initNavWithAccountList()
            if (authConfig !is ActivityIntents.AuthConfig.ManageAccount && currentAccount != null) {
                val account = getAccountDataUseCase.execute(UserIdInput(currentAccount))
                saveCurrentApiUrlUseCase.execute(SaveCurrentApiUrlUseCase.Input(account.url))
                view?.navigateToSignIn(currentAccount)
            }
        }
    }
}
