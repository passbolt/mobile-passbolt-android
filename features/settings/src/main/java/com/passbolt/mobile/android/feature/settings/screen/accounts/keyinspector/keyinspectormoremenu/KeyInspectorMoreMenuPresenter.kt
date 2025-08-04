package com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu

import com.passbolt.mobile.android.core.accounts.usecase.privatekey.GetSelectedUserPrivateKeyUseCase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorMoreMenuPresenter.CurrentFlow.EXPORT_PRIVATE_KEY
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorMoreMenuPresenter.CurrentFlow.EXPORT_PUBLIC_KEY
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class KeyInspectorMoreMenuPresenter(
    private val getPrivateKeyUseCase: GetSelectedUserPrivateKeyUseCase,
    private val openPgp: OpenPgp,
    coroutineLaunchContext: CoroutineLaunchContext,
) : KeyInspectorMoreMenuContract.Presenter {
    override var view: KeyInspectorMoreMenuContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private lateinit var currentFlow: CurrentFlow

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super.detach()
    }

    override fun exportPrivateKeyClick() {
        currentFlow = EXPORT_PRIVATE_KEY
        view?.navigateToRefreshPassphrase()
    }

    override fun exportPublicKeyClick() {
        currentFlow = EXPORT_PUBLIC_KEY
        view?.navigateToRefreshPassphrase()
    }

    override fun authenticationSucceeded() {
        when (currentFlow) {
            EXPORT_PRIVATE_KEY -> sharePrivateKey()
            EXPORT_PUBLIC_KEY -> sharePublicKey()
        }
    }

    private fun sharePublicKey() {
        scope.launch {
            getPrivateKeyUseCase.execute(Unit).privateKey?.let {
                when (val publicKeyResult = openPgp.generatePublicKey(it)) {
                    is OpenPgpResult.Error -> view?.showFailedToGeneratePublicKey(publicKeyResult.error.message)
                    is OpenPgpResult.Result -> {
                        view?.close()
                        view?.showShareSheet(publicKeyResult.result)
                    }
                }
            }
        }
    }

    private fun sharePrivateKey() {
        scope.launch {
            getPrivateKeyUseCase.execute(Unit).privateKey?.let {
                view?.close()
                view?.showShareSheet(it)
            }
        }
    }

    private enum class CurrentFlow {
        EXPORT_PRIVATE_KEY,
        EXPORT_PUBLIC_KEY,
    }
}
