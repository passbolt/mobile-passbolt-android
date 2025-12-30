package com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.core.authenticationcore.session.GetSessionUseCase
import com.passbolt.mobile.android.core.idlingresource.TransferAccountIdlingResource
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticatedViewModel
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountIntent.CancelTransfer
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountIntent.ConfirmCancelTransfer
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountIntent.DismissCancelDialog
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountIntent.GoBack
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountScreenSideEffect.ErrorSnackbarType.FAILED_TO_CREATE_TRANSFER
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountScreenSideEffect.ErrorSnackbarType.FAILED_TO_FETCH_TRANSFER_DETAILS
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountScreenSideEffect.ErrorSnackbarType.FAILED_TO_GENERATE_QR_DATA
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountScreenSideEffect.ErrorSnackbarType.FAILED_TO_INITIALIZE_PARAMETERS
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountScreenSideEffect.NavigateToResult
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountScreenSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.data.CreateTransferInputParametersGenerator
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.data.TransferQrCodesDataGenerator
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.usecase.CreateTransferUseCase
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.usecase.ViewTransferUseCase
import com.passbolt.mobile.android.ui.Status
import com.passbolt.mobile.android.ui.TransferAccountStatusType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

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

internal class TransferAccountViewModel(
    private val createTransferInputParametersGenerator: CreateTransferInputParametersGenerator,
    private val transferQrCodesDataGenerator: TransferQrCodesDataGenerator,
    private val createTransferUseCase: CreateTransferUseCase,
    private val viewTransferUseCase: ViewTransferUseCase,
    private val getSessionUseCase: GetSessionUseCase,
    private val transferAccountIdlingResource: TransferAccountIdlingResource,
    coroutineLaunchContext: CoroutineLaunchContext,
) : AuthenticatedViewModel<TransferAccountState, TransferAccountScreenSideEffect>(TransferAccountState()) {
    private var qrCodePagesData: List<String> = emptyList()
    private var transferStatus = Status.START
    private var transferPollingJob: Job? = null

    init {
        viewModelScope.launch(coroutineLaunchContext.ui) {
            transferAccountIdlingResource.setIdle(false)
            updateViewState { copy(showProgress = true) }
            when (val parameters = createTransferInputParametersGenerator.calculateCreateTransferParameters()) {
                is CreateTransferInputParametersGenerator.Output.Error -> {
                    emitSideEffect(ShowErrorSnackbar(FAILED_TO_INITIALIZE_PARAMETERS))
                }
                is CreateTransferInputParametersGenerator.Output.Parameters -> {
                    createTransfer(parameters)
                }
            }
            updateViewState { copy(showProgress = false) }
            transferAccountIdlingResource.setIdle(true)
        }
    }

    fun onIntent(intent: TransferAccountIntent) {
        when (intent) {
            GoBack, CancelTransfer -> updateViewState { copy(showCancelDialog = true) }
            DismissCancelDialog -> updateViewState { copy(showCancelDialog = false) }
            ConfirmCancelTransfer -> {
                transferPollingJob?.cancel()
                updateViewState { copy(showCancelDialog = false) }
                emitSideEffect(NavigateToResult(TransferAccountStatusType.CANCELED))
            }
        }
    }

    private suspend fun createTransfer(parameters: CreateTransferInputParametersGenerator.Output.Parameters) {
        when (
            val response =
                runAuthenticatedOperation {
                    createTransferUseCase.execute(
                        CreateTransferUseCase.Input(
                            parameters.totalPagesCount,
                            parameters.pagesDataHash,
                        ),
                    )
                }
        ) {
            is CreateTransferUseCase.Output.Failure<*> -> {
                val headerMessage = response.response.headerMessage
                emitSideEffect(ShowErrorSnackbar(FAILED_TO_CREATE_TRANSFER, headerMessage))
                Timber.e("Could not create transfer: $headerMessage")
            }
            is CreateTransferUseCase.Output.Success -> {
                Timber.d("Transfer created.")
                generateQrCodePagesData(response, parameters)
                if (qrCodePagesData.isNotEmpty()) {
                    updateViewState {
                        copy(
                            qrCodeContent = qrCodePagesData.first(),
                            totalPages = parameters.totalPagesCount,
                            currentPage = 0,
                        )
                    }
                    startTransferPolling(response.transfer.id, parameters.totalPagesCount)
                }
            }
        }
    }

    private suspend fun generateQrCodePagesData(
        response: CreateTransferUseCase.Output.Success,
        parameters: CreateTransferInputParametersGenerator.Output.Parameters,
    ) {
        val pagesDataResult =
            transferQrCodesDataGenerator.generateQrCodesDataPages(
                TransferQrCodesDataGenerator.Input(
                    response.transfer.id,
                    response.transfer.authenticationToken,
                    parameters.totalPagesCount,
                    parameters.pagesDataHash,
                    parameters.keyJson,
                ),
            )
        when (pagesDataResult) {
            is TransferQrCodesDataGenerator.Output.Error -> {
                emitSideEffect(ShowErrorSnackbar(FAILED_TO_GENERATE_QR_DATA))
            }
            is TransferQrCodesDataGenerator.Output.QrPages -> {
                qrCodePagesData = pagesDataResult.pages
            }
        }
    }

    private fun startTransferPolling(
        transferId: String,
        totalPageCount: Int,
    ) {
        transferPollingJob =
            viewModelScope.launch {
                while (shouldLoopForTransfer(totalPageCount)) {
                    val accessToken =
                        "Bearer %s".format(
                            requireNotNull(getSessionUseCase.execute(Unit).accessToken),
                        )
                    val mfaCookie = getSessionUseCase.execute(Unit).mfaToken

                    delay(GET_TRANSFER_LOOP_INTERVAL_DELAY_MILLIS)
                    when (
                        val response =
                            runAuthenticatedOperation {
                                viewTransferUseCase.execute(ViewTransferUseCase.Input(accessToken, mfaCookie, transferId))
                            }
                    ) {
                        is ViewTransferUseCase.Output.Failure<*> -> {
                            Timber.e("Error during transfer details fetch: %s", response.response.headerMessage)
                            emitSideEffect(
                                ShowErrorSnackbar(
                                    FAILED_TO_FETCH_TRANSFER_DETAILS,
                                    response.response.headerMessage,
                                ),
                            )
                        }
                        is ViewTransferUseCase.Output.Success -> {
                            transferStatus = response.transfer.status
                            if (response.transfer.currentPage > viewState.value.currentPage) {
                                updateViewState {
                                    copy(
                                        qrCodeContent = qrCodePagesData[response.transfer.currentPage],
                                        currentPage = response.transfer.currentPage,
                                    )
                                }
                            }
                        }
                    }
                }
                transferPollingFinished()
            }
    }

    private fun transferPollingFinished() {
        val statusType =
            when (transferStatus) {
                Status.ERROR -> TransferAccountStatusType.FAILURE
                Status.IN_PROGRESS -> TransferAccountStatusType.CANCELED
                Status.COMPLETE -> TransferAccountStatusType.SUCCESS
                Status.CANCEL -> TransferAccountStatusType.CANCELED
                Status.START -> TransferAccountStatusType.CANCELED
            }
        emitSideEffect(NavigateToResult(statusType))
    }

    private fun shouldLoopForTransfer(totalPageCount: Int) =
        transferStatus in setOf(Status.START, Status.IN_PROGRESS) && viewState.value.currentPage < totalPageCount

    override fun onCleared() {
        transferPollingJob?.cancel()
        super.onCleared()
    }

    @VisibleForTesting
    fun cancelPollingForTests() {
        transferPollingJob?.cancel()
    }

    companion object {
        @VisibleForTesting
        const val GET_TRANSFER_LOOP_INTERVAL_DELAY_MILLIS = 500L
    }
}
