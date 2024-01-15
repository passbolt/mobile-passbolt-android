package com.passbolt.mobile.android.feature.setup.scanqr

import com.passbolt.mobile.android.common.HttpsVerifier
import com.passbolt.mobile.android.common.UuidProvider
import com.passbolt.mobile.android.core.accounts.AccountKitParser
import com.passbolt.mobile.android.core.accounts.AccountsInteractor
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.navigation.AccountSetupDataModel
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ScanQrParser
import com.passbolt.mobile.android.feature.setup.scanqr.usecase.UpdateTransferUseCase
import com.passbolt.mobile.android.feature.setup.summary.ResultStatus
import com.passbolt.mobile.android.storage.usecase.accountdata.UpdateAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.accounts.CheckAccountExistsUseCase
import com.passbolt.mobile.android.storage.usecase.privatekey.SavePrivateKeyUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.SaveCurrentApiUrlUseCase
import com.passbolt.mobile.android.ui.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.properties.Delegates

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */
class ScanQrPresenter(
    coroutineLaunchContext: CoroutineLaunchContext,
    private val updateTransferUseCase: UpdateTransferUseCase,
    private val qrParser: ScanQrParser,
    private val uuidProvider: UuidProvider,
    private val savePrivateKeyUseCase: SavePrivateKeyUseCase,
    private val updateAccountDataUseCase: UpdateAccountDataUseCase,
    private val checkAccountExistsUseCase: CheckAccountExistsUseCase,
    private val httpsVerifier: HttpsVerifier,
    private val saveCurrentApiUrlUseCase: SaveCurrentApiUrlUseCase,
    private val accountsInteractor: AccountsInteractor,
    private val accountKitParser: AccountKitParser
) : ScanQrContract.Presenter {

    override var view: ScanQrContract.View? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    private lateinit var authToken: String
    private lateinit var transferUuid: String
    private lateinit var userId: String
    private lateinit var serverDomain: String
    private var totalPages: Int by Delegates.notNull()
    private var currentPage = 0

    override fun argsRetrieved(bundledAccountSetupData: AccountSetupDataModel?) {
        if (bundledAccountSetupData != null) {
            injectPredefinedAccount(bundledAccountSetupData)
        } else {
            initQrScanning()
        }
    }

    private fun initQrScanning() {
        scope.launch {
            launch { qrParser.startParsing(view!!.scanResultChannel()) }
            launch {
                qrParser.parseResultFlow
                    .collect { processParseResult(it) }
            }
        }
        view?.startAnalysis()
    }

    private suspend fun processParseResult(parserResult: ParseResult) {
        when (parserResult) {
            is ParseResult.Failure -> parserFailure(parserResult.exception)
            is ParseResult.PassboltQr -> when (parserResult) {
                is ParseResult.PassboltQr.FirstPage -> parserFirstPage(parserResult)
                is ParseResult.PassboltQr.SubsequentPage -> parserSubsequentPage(parserResult)
            }
            is ParseResult.FinishedWithSuccess -> parserFinishedWithSuccess(parserResult.armoredKey)
            is ParseResult.UserResolvableError -> when (parserResult.errorType) {
                ParseResult.UserResolvableError.ErrorType.MULTIPLE_BARCODES -> view?.showMultipleCodesInRange()
                ParseResult.UserResolvableError.ErrorType.NO_BARCODES_IN_RANGE -> view?.showCenterCameraOnBarcode()
                ParseResult.UserResolvableError.ErrorType.NOT_A_PASSBOLT_QR -> view?.showNotAPassboltQr()
            }
            is ParseResult.ScanFailure -> view?.showBarcodeScanError(parserResult.exception?.message)
        }
    }

    private suspend fun parserFailure(exception: Throwable?) {
        exception?.let { Timber.e(it) }
        updateTransfer(pageNumber = currentPage, Status.ERROR)
    }

    private suspend fun parserFirstPage(firstPage: ParseResult.PassboltQr.FirstPage) {
        val userId = firstPage.content.userId
        transferUuid = firstPage.content.transferId.toString()
        authToken = firstPage.content.authenticationToken
        totalPages = firstPage.content.totalPages
        serverDomain = firstPage.content.domain

        val userExistsResult = checkAccountExistsUseCase.execute(CheckAccountExistsUseCase.Input(userId.toString()))
        if (userExistsResult.exist) {
            currentPage = totalPages - 1
            updateTransferAlreadyLinked(currentPage)
        } else if (!httpsVerifier.isHttps(firstPage.content.domain)) {
            view?.navigateToSummary(ResultStatus.HttpNotSupported())
        } else {
            if (currentPage > 0) {
                parserFailure(Throwable("Other qr code scanning has been already started"))
            } else {
                view?.initializeProgress(totalPages)
                view?.showKeepGoing()
                saveAccountDetails(userId.toString(), firstPage.content.domain)
                updateTransfer(pageNumber = firstPage.reservedBytesDto.page + 1)
            }
        }
    }

    private suspend fun parserSubsequentPage(subsequentPage: ParseResult.PassboltQr.SubsequentPage) {
        currentPage = subsequentPage.reservedBytesDto.page
        view?.showKeepGoing()

        if (subsequentPage.reservedBytesDto.page < totalPages - 1) {
            updateTransfer(pageNumber = currentPage + 1)
        } else {
            qrParser.verifyScannedKey()
        }
    }

    private suspend fun parserFinishedWithSuccess(armoredKey: String) {
        when (savePrivateKeyUseCase.execute(SavePrivateKeyUseCase.Input(userId, armoredKey))) {
            SavePrivateKeyUseCase.Output.Failure -> {
                updateTransfer(pageNumber = currentPage, Status.ERROR)
                view?.navigateToSummary(ResultStatus.Failure(""))
            }
            SavePrivateKeyUseCase.Output.Success -> {
                updateTransfer(pageNumber = currentPage, Status.COMPLETE)
                view?.navigateToSummary(ResultStatus.Success(userId))
            }
        }
    }

    private suspend fun updateTransferAlreadyLinked(pageNumber: Int) {
        updateTransferUseCase.execute(
            UpdateTransferUseCase.Input(
                uuid = transferUuid,
                authToken = authToken,
                currentPage = pageNumber,
                status = Status.COMPLETE
            )
        )
        // ignoring result
        view?.navigateToSummary(ResultStatus.AlreadyLinked())
    }

    private suspend fun updateTransfer(pageNumber: Int, status: Status = Status.IN_PROGRESS) {
        // in case of the first qr code is not a correct one
        if (!::transferUuid.isInitialized || !::authToken.isInitialized || !::serverDomain.isInitialized) {
            view?.navigateToSummary(ResultStatus.Failure("Could not initialize private key transfer"))
            return
        }
        val response = updateTransferUseCase.execute(
            UpdateTransferUseCase.Input(
                uuid = transferUuid,
                authToken = authToken,
                currentPage = pageNumber,
                status = status
            )
        )
        when (response) {
            is UpdateTransferUseCase.Output.Failure -> {
                Timber.e(response.error.exception, "There was an error during transfer update")
                if (status == Status.ERROR || status == Status.CANCEL) {
                    // ignoring
                } else {
                    if (response.error.isServerNotReachable) {
                        view?.showServerNotReachable(serverDomain)
                    } else if (response.error.isNoNetworkException) {
                        view?.navigateToSummary(ResultStatus.NoNetwork())
                    } else {
                        view?.showUpdateTransferError(response.error.headerMessage)
                    }
                }
            }
            is UpdateTransferUseCase.Output.Success -> {
                onUpdateTransferSuccess(pageNumber, status, response)
            }
        }
    }

    private fun onUpdateTransferSuccess(
        pageNumber: Int,
        status: Status,
        response: UpdateTransferUseCase.Output.Success
    ) {
        view?.setProgress(pageNumber)
        when (status) {
            Status.COMPLETE -> {
                updateAccountDataUseCase.execute(
                    UpdateAccountDataUseCase.Input(
                        userId = userId,
                        firstName = response.updateTransferModel.firstName,
                        lastName = response.updateTransferModel.lastName,
                        avatarUrl = response.updateTransferModel.avatarUrl,
                        email = response.updateTransferModel.email
                    )
                )
            }
            Status.ERROR -> {
                view?.navigateToSummary(ResultStatus.Failure(""))
            }
            else -> {
                // ignoring
            }
        }
    }

    private fun saveAccountDetails(serverId: String, url: String) {
        userId = uuidProvider.get()
        saveCurrentApiUrlUseCase.execute(SaveCurrentApiUrlUseCase.Input(url))
        updateAccountDataUseCase.execute(
            UpdateAccountDataUseCase.Input(
                userId = userId,
                url = url,
                serverId = serverId
            )
        )
    }

    private fun injectPredefinedAccount(accountSetupData: AccountSetupDataModel) {
        accountsInteractor.injectPredefinedAccountData(
            accountSetupData,
            onSuccess = { userId -> view?.navigateToSummary(ResultStatus.Success(userId)) },
            onFailure = { view?.navigateToSummary(ResultStatus.Failure("")) }
        )
    }

    override fun startCameraError(exc: Exception) {
        Timber.e(exc)
        view?.showStartCameraError()
    }

    override fun backClick() {
        view?.showExitConfirmation()
    }

    override fun exitConfirmClick() {
        view?.navigateBack()
    }

    override fun infoIconClick() {
        view?.showHelpMenu()
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super.detach()
    }

    override fun viewResumed() {
        view?.setFlagSecure()
    }

    override fun viewPaused() {
        view?.removeFlagSecure()
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
}
