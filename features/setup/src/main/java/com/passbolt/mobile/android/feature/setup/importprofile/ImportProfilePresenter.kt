package com.passbolt.mobile.android.feature.setup.importprofile

import com.passbolt.mobile.android.common.UuidProvider
import com.passbolt.mobile.android.common.validation.StringIsHttpsWebUrl
import com.passbolt.mobile.android.common.validation.StringIsUuid
import com.passbolt.mobile.android.common.validation.StringNotBlank
import com.passbolt.mobile.android.common.validation.validation
import com.passbolt.mobile.android.feature.setup.summary.ResultStatus
import com.passbolt.mobile.android.storage.usecase.accountdata.UpdateAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.accounts.CheckAccountExistsUseCase
import com.passbolt.mobile.android.storage.usecase.privatekey.SavePrivateKeyUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.SaveCurrentApiUrlUseCase

class ImportProfilePresenter(
    private val uuidProvider: UuidProvider,
    private val updateAccountDataUseCase: UpdateAccountDataUseCase,
    private val saveCurrentApiUrlUseCase: SaveCurrentApiUrlUseCase,
    private val savePrivateKeyUseCase: SavePrivateKeyUseCase,
    private val checkAccountExistsUseCase: CheckAccountExistsUseCase
) : ImportProfileContract.Presenter {

    override var view: ImportProfileContract.View? = null
    private var userId: String = ""
    private var accountUrl: String = ""
    private var privateKey: String = ""

    override fun userIdChanged(userId: String) {
        this.userId = userId
    }

    override fun accountUrlChanged(accountUrl: String) {
        this.accountUrl = accountUrl
    }

    override fun privateKeyChanged(privateKey: String) {
        this.privateKey = privateKey
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
        val userExistsResult = checkAccountExistsUseCase.execute(CheckAccountExistsUseCase.Input(userId))
        if (userExistsResult.exist) {
            view?.navigateToSummary(ResultStatus.AlreadyLinked())
        } else {
            val localUserId = uuidProvider.get()
            saveCurrentApiUrlUseCase.execute(SaveCurrentApiUrlUseCase.Input(accountUrl))
            updateAccountDataUseCase.execute(
                UpdateAccountDataUseCase.Input(
                    userId = localUserId,
                    firstName = "",
                    lastName = "",
                    avatarUrl = "",
                    email = "",
                    url = accountUrl,
                    serverId = userId
                )
            )

            when (savePrivateKeyUseCase.execute(SavePrivateKeyUseCase.Input(localUserId, privateKey))) {
                SavePrivateKeyUseCase.Output.Failure -> {
                    view?.navigateToSummary(ResultStatus.Failure(""))
                }
                SavePrivateKeyUseCase.Output.Success -> {
                    view?.navigateToSummary(ResultStatus.Success(localUserId))
                }
            }
        }
    }
}
