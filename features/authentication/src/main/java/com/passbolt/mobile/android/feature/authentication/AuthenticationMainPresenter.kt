package com.passbolt.mobile.android.feature.authentication

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.navigation.AuthenticationTarget
import com.passbolt.mobile.android.core.navigation.AuthenticationType
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.storage.usecase.accountdata.GetAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.SaveCurrentApiUrlUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class AuthenticationMainPresenter(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val saveCurrentApiUrlUseCase: SaveCurrentApiUrlUseCase,
    private val getAccountDataUseCase: GetAccountDataUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : AuthenticationMainContract.Presenter {

    override var view: AuthenticationMainContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun bundleRetrieved(
        authTarget: AuthenticationTarget,
        authenticationStrategy: AuthenticationType?,
        shouldLogOut: Boolean,
        userId: String?
    ) {
        scope.launch {
            if (shouldLogOut) {
                view?.showProgress()
                signOutUseCase.execute(Unit)
                view?.hideProgress()
            }

            when (authTarget) {
                AuthenticationTarget.MANAGE_ACCOUNTS -> {
                    view?.navigateToManageAccounts()
                }
                AuthenticationTarget.AUTHENTICATE -> {
                    processAuthentication(requireNotNull(authenticationStrategy), userId)
                }
            }
        }
    }

    private fun processAuthentication(authenticationStrategy: AuthenticationType, userId: String?) {
        val selectedUserId = try {
            userId ?: getSelectedAccountUseCase.execute(Unit).selectedAccount
        } catch (exception: Exception) {
            null
        }

        // navigate to auth if user is selected else stay on account list
        selectedUserId?.let {
            val userUrl = getAccountDataUseCase.execute(UserIdInput(it)).url
            saveCurrentApiUrlUseCase.execute(SaveCurrentApiUrlUseCase.Input(userUrl))
            view?.navigateToAuth(it, authenticationStrategy)
        } ?: view?.setDefaultNavGraph()
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super.detach()
    }
}
