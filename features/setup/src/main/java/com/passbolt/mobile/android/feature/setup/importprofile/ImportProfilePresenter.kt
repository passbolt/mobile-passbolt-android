package com.passbolt.mobile.android.feature.setup.importprofile

import com.passbolt.mobile.android.common.validation.StringIsHttpsWebUrl
import com.passbolt.mobile.android.common.validation.StringIsUuid
import com.passbolt.mobile.android.common.validation.StringNotBlank
import com.passbolt.mobile.android.common.validation.validation
import com.passbolt.mobile.android.core.accounts.AccountsInteractor
import com.passbolt.mobile.android.core.accounts.AccountsInteractor.InjectAccountFailureType.ACCOUNT_ALREADY_LINKED
import com.passbolt.mobile.android.core.accounts.AccountsInteractor.InjectAccountFailureType.ERROR_NON_HTTPS_DOMAIN
import com.passbolt.mobile.android.core.accounts.AccountsInteractor.InjectAccountFailureType.ERROR_WHEN_SAVING_PRIVATE_KEY
import com.passbolt.mobile.android.core.navigation.AccountSetupDataModel
import com.passbolt.mobile.android.feature.setup.summary.ResultStatus

class ImportProfilePresenter(
    private val accountsInteractor: AccountsInteractor,
) : ImportProfileContract.Presenter {
    override var view: ImportProfileContract.View? = null
    private var userId: String = ""
    private var accountUrl: String = ""
    private var privateKey: String = ""

    override fun userIdChanged(userId: String) {
        this.userId = userId.trim()
    }

    override fun accountUrlChanged(accountUrl: String) {
        this.accountUrl = accountUrl.trim()
    }

    override fun privateKeyChanged(privateKey: String) {
        this.privateKey = privateKey.trim()
    }

    override fun importClick() {
        view?.clearValidationErrors()
        validation {
            of(userId) {
                withRules(StringNotBlank, StringIsUuid) {
                    onInvalid {
                        view?.showIncorrectUuid()
                    }
                }
            }
            of(accountUrl) {
                withRules(StringNotBlank, StringIsHttpsWebUrl) {
                    onInvalid {
                        view?.showIncorrectAccountUrl()
                    }
                }
            }
            of(privateKey) {
                withRules(StringNotBlank) {
                    onInvalid {
                        view?.showIncorrectPrivateKey()
                    }
                }
            }
            onValid {
                importAccount()
            }
        }
    }

    private fun importAccount() {
        accountsInteractor.injectPredefinedAccountData(
            AccountSetupDataModel(
                serverUserId = userId,
                domain = accountUrl,
                armoredKey = privateKey,
                firstName = "",
                lastName = "",
                avatarUrl = "",
                userName = "",
                keyFingerprint = "",
            ),
            onSuccess = { userId ->
                view?.navigateToSummary(ResultStatus.Success(userId))
            },
            onFailure = { failureType ->
                view?.navigateToSummary(
                    when (failureType) {
                        ACCOUNT_ALREADY_LINKED -> ResultStatus.AlreadyLinked()
                        ERROR_NON_HTTPS_DOMAIN -> ResultStatus.HttpNotSupported()
                        ERROR_WHEN_SAVING_PRIVATE_KEY -> ResultStatus.Failure(failureType.name)
                    },
                )
            },
        )
    }
}
