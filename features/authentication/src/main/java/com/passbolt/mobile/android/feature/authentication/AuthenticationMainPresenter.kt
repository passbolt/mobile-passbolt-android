package com.passbolt.mobile.android.feature.authentication

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.navigation.AuthenticationTarget
import com.passbolt.mobile.android.core.navigation.AuthenticationType
import com.passbolt.mobile.android.service.logout.LogoutRepository
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import timber.log.Timber

class AuthenticationMainPresenter(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val logoutRepository: LogoutRepository,
    coroutineLaunchContext: CoroutineLaunchContext
) : AuthenticationMainContract.Presenter {

    override var view: AuthenticationMainContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun bundleRetrieved(
        authTarget: AuthenticationTarget,
        authenticationStrategy: AuthenticationType?,
        shouldLogOut: Boolean
    ) {
        scope.launch {
            if (shouldLogOut) {
                view?.showProgress()
                logoutRepository.logout()
                view?.hideProgress()
            }
        }

        when (authTarget) {
            AuthenticationTarget.MANAGE_ACCOUNTS -> {
                view?.navigateToManageAccounts()
            }
            AuthenticationTarget.AUTHENTICATE -> {
                processAuthentication(requireNotNull(authenticationStrategy))
            }
        }
    }

    private fun processAuthentication(authenticationStrategy: AuthenticationType) {
        when (authenticationStrategy) {
            is AuthenticationType.Passphrase -> {
                val selectedAccount = getSelectedAccountUseCase.execute(Unit).selectedAccount
                view?.navigateToAuth(selectedAccount, authenticationStrategy)
            }
            is AuthenticationType.SignIn -> navigateToSingIn(authenticationStrategy)
        }
    }

    private fun navigateToSingIn(authenticationStrategy: AuthenticationType.SignIn) {
        authenticationStrategy.userId?.let {
            view?.navigateToAuth(it, authenticationStrategy)
        } ?: run {
            runCatching {
                getSelectedAccountUseCase.execute(Unit).selectedAccount
            }
                .onSuccess {
                    view?.navigateToAuth(it, authenticationStrategy)
                }
                .onFailure {
                    Timber.d(it)
                    // no account selected - remain on account list
                }
        }
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super.detach()
    }
}
