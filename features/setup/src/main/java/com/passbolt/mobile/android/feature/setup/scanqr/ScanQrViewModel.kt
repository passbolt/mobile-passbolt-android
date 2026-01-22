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

package com.passbolt.mobile.android.feature.setup.scanqr

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.common.HttpsVerifier
import com.passbolt.mobile.android.common.UuidProvider
import com.passbolt.mobile.android.common.usecase.FetchFileAsStringUseCase
import com.passbolt.mobile.android.core.accounts.AccountKitParser
import com.passbolt.mobile.android.core.accounts.AccountsInteractor
import com.passbolt.mobile.android.core.accounts.AccountsInteractor.InjectAccountFailureType.ACCOUNT_ALREADY_LINKED
import com.passbolt.mobile.android.core.accounts.AccountsInteractor.InjectAccountFailureType.ERROR_NON_HTTPS_DOMAIN
import com.passbolt.mobile.android.core.accounts.AccountsInteractor.InjectAccountFailureType.ERROR_WHEN_SAVING_PRIVATE_KEY
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.UpdateAccountDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.accounts.CheckAccountExistsUseCase
import com.passbolt.mobile.android.core.accounts.usecase.privatekey.SavePrivateKeyUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.SaveCurrentApiUrlUseCase
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.navigation.AccountSetupDataModel
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.AccessLogs
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.ConfirmSetupLeave
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.DismissHelpMenu
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.DismissServerNotReachable
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.DismissSetupLeave
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.GoBack
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.ImportProfileManually
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.OpenHelpMenu
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.SelectedAccountKit
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.StartCameraError
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrSideEffect.NavigateToImportProfile
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrSideEffect.NavigateToLogs
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrSideEffect.NavigateToSummary
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrState.TooltipMessage.CAMERA_ERROR
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult.Failure
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult.FinishedWithSuccess
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult.PassboltQr
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult.PassboltQr.AccountKitPage
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult.PassboltQr.FirstPage
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult.PassboltQr.SubsequentPage
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult.ScanFailure
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult.UserResolvableError
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult.UserResolvableError.ErrorType.MULTIPLE_BARCODES
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult.UserResolvableError.ErrorType.NOT_A_PASSBOLT_QR
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult.UserResolvableError.ErrorType.NO_BARCODES_IN_RANGE
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ScanQrParser
import com.passbolt.mobile.android.feature.setup.scanqr.usecase.UpdateTransferUseCase
import com.passbolt.mobile.android.ui.ResultStatus
import com.passbolt.mobile.android.ui.Status
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.properties.Delegates

internal class ScanQrViewModel(
    private val updateTransferUseCase: UpdateTransferUseCase,
    private val qrParser: ScanQrParser,
    private val uuidProvider: UuidProvider,
    private val savePrivateKeyUseCase: SavePrivateKeyUseCase,
    private val updateAccountDataUseCase: UpdateAccountDataUseCase,
    private val checkAccountExistsUseCase: CheckAccountExistsUseCase,
    private val httpsVerifier: HttpsVerifier,
    private val saveCurrentApiUrlUseCase: SaveCurrentApiUrlUseCase,
    private val accountsInteractor: AccountsInteractor,
    private val accountKitParser: AccountKitParser,
    private val fetchFileAsStringUseCase: FetchFileAsStringUseCase,
) : SideEffectViewModel<ScanQrState, ScanQrSideEffect>(ScanQrState()) {
    private lateinit var authToken: String
    private lateinit var transferUuid: String
    private lateinit var userId: String
    private lateinit var serverDomain: String
    private var totalPages: Int by Delegates.notNull()
    private var currentPage = 0

    fun onIntent(intent: ScanQrIntent) {
        when (intent) {
            GoBack -> updateViewState { copy(showSetupLeaveConfirmationDialog = true) }
            ConfirmSetupLeave -> {
                updateViewState { copy(showSetupLeaveConfirmationDialog = false) }
                emitSideEffect(NavigateBack)
            }
            DismissSetupLeave -> updateViewState { copy(showSetupLeaveConfirmationDialog = false) }
            OpenHelpMenu -> updateViewState { copy(showHelpMenu = true) }
            DismissHelpMenu -> updateViewState { copy(showHelpMenu = false) }
            ImportProfileManually -> emitSideEffect(NavigateToImportProfile)
            AccessLogs -> emitSideEffect(NavigateToLogs)
            is StartCameraError -> {
                Timber.e(intent.exception)
                updateViewState { copy(tooltipMessage = CAMERA_ERROR) }
            }
            DismissServerNotReachable -> updateViewState { copy(showServerNotReachableDialog = false) }
            is SelectedAccountKit -> accountKitSelected(intent.accountKit)
            is ScanQrIntent.Initialize -> initialize(intent)
        }
    }

    private fun initialize(intent: ScanQrIntent.Initialize) {
        if (intent.accountSetupDataModel != null) {
            injectPredefinedAccount(intent.accountSetupDataModel)
        } else {
            viewModelScope.launch {
                launch { qrParser.startParsing(intent.barcodeScanFlow) }
                launch { qrParser.parseResultFlow.collect { processParseResult(it) } }
            }
        }
    }

    private suspend fun processParseResult(parserResult: ParseResult) {
        when (parserResult) {
            is Failure -> parserFailure(parserResult.exception)
            is PassboltQr ->
                when (parserResult) {
                    is FirstPage -> parserFirstPage(parserResult)
                    is SubsequentPage -> parserSubsequentPage(parserResult)
                    is AccountKitPage -> setupFromAccountKit(parserResult)
                }
            is FinishedWithSuccess -> parserFinishedWithSuccess(parserResult.armoredKey)
            is UserResolvableError ->
                when (parserResult.errorType) {
                    MULTIPLE_BARCODES -> updateViewState { copy(tooltipMessage = ScanQrState.TooltipMessage.MULTIPLE_BARCODES) }
                    NO_BARCODES_IN_RANGE -> updateViewState { copy(tooltipMessage = ScanQrState.TooltipMessage.CENTER_CAMERA_ON_BARCODE) }
                    NOT_A_PASSBOLT_QR -> updateViewState { copy(tooltipMessage = ScanQrState.TooltipMessage.NOT_A_PASSBOLT_QR) }
                }
            is ScanFailure ->
                updateViewState {
                    copy(
                        tooltipMessage = ScanQrState.TooltipMessage.SCAN_ERROR,
                        scanErrorMessage = parserResult.exception?.message,
                    )
                }
        }
    }

    private suspend fun setupFromAccountKit(accountKitPage: AccountKitPage) {
        updateViewState { copy(showProgress = true) }
        val failureAction = { emitSideEffect(NavigateToSummary(ResultStatus.Failure(""))) }
        try {
            when (
                val fileContentResult =
                    fetchFileAsStringUseCase.execute(
                        FetchFileAsStringUseCase.Input(accountKitPage.content.accountKitUrl),
                    )
            ) {
                is FetchFileAsStringUseCase.Output.Failure -> failureAction()
                is FetchFileAsStringUseCase.Output.Success ->
                    accountKitParser.parseAndVerify(
                        fileContentResult.fileContent,
                        onSuccess = { setupDataModel -> injectPredefinedAccount(setupDataModel) },
                        onFailure = { failureAction() },
                    )
            }
            updateViewState { copy(showProgress = false) }
        } catch (e: Exception) {
            Timber.e(e, "Error while reading account kit file")
            updateViewState { copy(showProgress = false) }
            emitSideEffect(NavigateToSummary(ResultStatus.Failure("")))
        }
    }

    private suspend fun parserFailure(exception: Throwable?) {
        exception?.let { Timber.e(it) }
        updateTransfer(pageNumber = currentPage, Status.ERROR)
    }

    private suspend fun parserFirstPage(firstPage: FirstPage) {
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
            emitSideEffect(NavigateToSummary(ResultStatus.HttpNotSupported()))
        } else {
            if (currentPage > 0) {
                parserFailure(Throwable("Other qr code scanning has been already started"))
            } else {
                updateViewState {
                    copy(
                        totalPages = this@ScanQrViewModel.totalPages,
                        tooltipMessage = ScanQrState.TooltipMessage.KEEP_GOING,
                    )
                }
                saveAccountDetails(userId.toString(), firstPage.content.domain)
                updateTransfer(pageNumber = firstPage.reservedBytesDto.page + 1)
            }
        }
    }

    private suspend fun parserSubsequentPage(subsequentPage: SubsequentPage) {
        currentPage = subsequentPage.reservedBytesDto.page
        updateViewState { copy(tooltipMessage = ScanQrState.TooltipMessage.KEEP_GOING) }

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
                emitSideEffect(NavigateToSummary(ResultStatus.Failure("")))
            }
            SavePrivateKeyUseCase.Output.Success -> {
                updateTransfer(pageNumber = currentPage, Status.COMPLETE)
                emitSideEffect(NavigateToSummary(ResultStatus.Success(userId)))
            }
        }
    }

    private suspend fun updateTransferAlreadyLinked(pageNumber: Int) {
        updateTransferUseCase.execute(
            UpdateTransferUseCase.Input(
                uuid = transferUuid,
                authToken = authToken,
                currentPage = pageNumber,
                status = Status.COMPLETE,
            ),
        )
        // ignoring result
        emitSideEffect(NavigateToSummary(ResultStatus.AlreadyLinked()))
    }

    private suspend fun updateTransfer(
        pageNumber: Int,
        status: Status = Status.IN_PROGRESS,
    ) {
        // in case of the first qr code is not a correct one
        if (!::transferUuid.isInitialized || !::authToken.isInitialized || !::serverDomain.isInitialized) {
            emitSideEffect(NavigateToSummary(ResultStatus.Failure("Could not initialize private key transfer")))
            return
        }
        val response =
            updateTransferUseCase.execute(
                UpdateTransferUseCase.Input(
                    uuid = transferUuid,
                    authToken = authToken,
                    currentPage = pageNumber,
                    status = status,
                ),
            )
        when (response) {
            is UpdateTransferUseCase.Output.Failure -> {
                Timber.e(response.error.exception, "There was an error during transfer update")
                if (status == Status.ERROR || status == Status.CANCEL) {
                    // ignoring
                } else {
                    if (response.error.isServerNotReachable) {
                        updateViewState {
                            copy(
                                showServerNotReachableDialog = true,
                                serverDomain = serverDomain,
                            )
                        }
                    } else if (response.error.isNoNetworkException) {
                        emitSideEffect(NavigateToSummary(ResultStatus.NoNetwork()))
                    } else {
                        emitSideEffect(ScanQrSideEffect.ShowToast(ToastType.UPDATE_TRANSFER_ERROR))
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
        response: UpdateTransferUseCase.Output.Success,
    ) {
        updateViewState { copy(currentPage = pageNumber) }
        when (status) {
            Status.COMPLETE -> {
                updateAccountDataUseCase.execute(
                    UpdateAccountDataUseCase.Input(
                        userId = userId,
                        firstName = response.updateTransferModel.firstName,
                        lastName = response.updateTransferModel.lastName,
                        avatarUrl = response.updateTransferModel.avatarUrl,
                        email = response.updateTransferModel.email,
                    ),
                )
            }
            Status.ERROR -> {
                emitSideEffect(NavigateToSummary(ResultStatus.Failure("")))
            }
            else -> {
                // ignoring
            }
        }
    }

    private fun saveAccountDetails(
        serverId: String,
        url: String,
    ) {
        userId = uuidProvider.get()
        saveCurrentApiUrlUseCase.execute(SaveCurrentApiUrlUseCase.Input(url))
        updateAccountDataUseCase.execute(
            UpdateAccountDataUseCase.Input(
                userId = userId,
                url = url,
                serverId = serverId,
            ),
        )
    }

    private fun injectPredefinedAccount(accountSetupData: AccountSetupDataModel) {
        accountsInteractor.injectPredefinedAccountData(
            accountSetupData,
            onSuccess = { userId ->
                emitSideEffect(NavigateToSummary(ResultStatus.Success(userId)))
            },
            onFailure = { failureType ->
                emitSideEffect(
                    NavigateToSummary(
                        when (failureType) {
                            ACCOUNT_ALREADY_LINKED -> ResultStatus.AlreadyLinked()
                            ERROR_NON_HTTPS_DOMAIN -> ResultStatus.HttpNotSupported()
                            ERROR_WHEN_SAVING_PRIVATE_KEY -> ResultStatus.Failure(failureType.name)
                        },
                    ),
                )
            },
        )
    }

    private fun accountKitSelected(accountKit: String) {
        viewModelScope.launch {
            accountKitParser.parseAndVerify(
                accountKit,
                onSuccess = { injectPredefinedAccount(it) },
                onFailure = { emitSideEffect(NavigateToSummary(ResultStatus.Failure(""))) },
            )
        }
    }
}
