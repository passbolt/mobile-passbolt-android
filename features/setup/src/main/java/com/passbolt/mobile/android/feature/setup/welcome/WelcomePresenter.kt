package com.passbolt.mobile.android.feature.setup.welcome

import com.passbolt.mobile.android.core.accounts.AccountKitParser
import com.passbolt.mobile.android.core.accounts.AccountsInteractor
import com.passbolt.mobile.android.core.accounts.AccountsInteractor.InjectAccountFailureType.ACCOUNT_ALREADY_LINKED
import com.passbolt.mobile.android.core.accounts.AccountsInteractor.InjectAccountFailureType.ERROR_NON_HTTPS_DOMAIN
import com.passbolt.mobile.android.core.accounts.AccountsInteractor.InjectAccountFailureType.ERROR_WHEN_SAVING_PRIVATE_KEY
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.navigation.AccountSetupDataModel
import com.passbolt.mobile.android.core.preferences.usecase.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.core.security.rootdetection.RootDetector
import com.passbolt.mobile.android.feature.setup.summary.ResultStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WelcomePresenter(
    coroutineLaunchContext: CoroutineLaunchContext,
    private val rootDetector: RootDetector,
    private val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase,
    private val accountsInteractor: AccountsInteractor,
    private val accountKitParser: AccountKitParser
) : WelcomeContract.Presenter {

    override var view: WelcomeContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun argsRetrieved(isTaskRoot: Boolean) {
        if (!isTaskRoot) {
            view?.initBackNavigation()
        }
        if (!getGlobalPreferencesUseCase.execute(Unit).isHideRootDialogEnabled && rootDetector.isDeviceRooted()) {
            view?.showDeviceRootedDialog()
        }
    }

    override fun noAccountButtonClick() {
        view?.showAccountCreationInfoDialog()
    }

    override fun connectToAccountClick() {
        view?.navigateToTransferDetails()
    }

    override fun helpClick() {
        view?.showHelpMenu()
    }

    override fun importProfileClick() {
        view?.navigateToImportProfile()
    }

    override fun importAccountKitClick() {
        view?.showAccountKitFilePicker()
    }

    override fun accountKitSelected(accountKit: String) {
        scope.launch {
            accountKitParser.parseAndVerify(accountKit,
                onSuccess = { injectPredefinedAccount(it) },
                onFailure = { view?.navigateToSummary(ResultStatus.Failure("")) }
            )
        }
    }

    private fun injectPredefinedAccount(accountSetupData: AccountSetupDataModel) {
        accountsInteractor.injectPredefinedAccountData(
            accountSetupData,
            onSuccess = { userId -> view?.navigateToSummary(ResultStatus.Success(userId)) },
            onFailure = { failureType ->
                view?.navigateToSummary(
                    when (failureType) {
                        ACCOUNT_ALREADY_LINKED -> ResultStatus.AlreadyLinked()
                        ERROR_NON_HTTPS_DOMAIN -> ResultStatus.HttpNotSupported()
                        ERROR_WHEN_SAVING_PRIVATE_KEY -> ResultStatus.Failure(failureType.name)
                    }
                )
            }
        )
    }
}
