package com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount

import com.passbolt.mobile.android.core.authenticationcore.session.GetSessionUseCase
import com.passbolt.mobile.android.core.idlingresource.TransferAccountIdlingResource
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.summary.TransferAccountStatus
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.data.CreateTransferInputParametersGenerator
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.data.TransferQrCodesDataGenerator
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.usecase.CreateTransferUseCase
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.usecase.ViewTransferUseCase
import com.passbolt.mobile.android.ui.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
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

class TransferAccountPresenter(
    private val createTransferInputParametersGenerator: CreateTransferInputParametersGenerator,
    private val transferQrCodesDataGenerator: TransferQrCodesDataGenerator,
    private val createTransferUseCase: CreateTransferUseCase,
    private val viewTransferUseCase: ViewTransferUseCase,
    private val getSessionUseCase: GetSessionUseCase,
    private val transferAccountIdlingResource: TransferAccountIdlingResource,
    coroutineLaunchContext: CoroutineLaunchContext,
) : BaseAuthenticatedPresenter<TransferAccountContract.View>(coroutineLaunchContext),
    TransferAccountContract.Presenter {
    override var view: TransferAccountContract.View? = null
    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(job + coroutineLaunchContext.ui)
    private val getTransferJob = SupervisorJob()
    private val getTransferScope = CoroutineScope(getTransferJob + coroutineLaunchContext.ui)

    private lateinit var qrCodePagesData: List<String>
    private var currentPage: Int = 0
    private var transferStatus = Status.START

    override fun attach(view: TransferAccountContract.View) {
        super<BaseAuthenticatedPresenter>.attach(view)
        coroutineScope.launch {
            transferAccountIdlingResource.setIdle(false)
            when (val parameters = createTransferInputParametersGenerator.calculateCreateTransferParameters()) {
                is CreateTransferInputParametersGenerator.Output.Error ->
                    view.showCouldNotInitializeTransferParameters()
                is CreateTransferInputParametersGenerator.Output.Parameters ->
                    createTransfer(parameters)
            }
            transferAccountIdlingResource.setIdle(true)
        }
    }

    private suspend fun createTransfer(parameters: CreateTransferInputParametersGenerator.Output.Parameters) {
        when (
            val response =
                runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
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
                view?.showCouldNotCreateTransfer(headerMessage)
                Timber.e("Could not create transfer: $headerMessage")
            }
            is CreateTransferUseCase.Output.Success -> {
                Timber.d("Transfer created.")
                generateQrCodePagesData(response, parameters)
                view?.showQrCodeForData(qrCodePagesData.first())
                startTransferPolling(response.transfer.id, parameters.totalPagesCount)
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
            is TransferQrCodesDataGenerator.Output.Error ->
                view?.showCouldNotGenerateQrTransferData()
            is TransferQrCodesDataGenerator.Output.QrPages -> {
                qrCodePagesData = pagesDataResult.pages
            }
        }
    }

    private fun startTransferPolling(
        transferId: String,
        totalPageCount: Int,
    ) {
        getTransferScope.launch {
            while (shouldLoopForTransfer(totalPageCount)) {
                val accessToken =
                    "Bearer %s".format(
                        requireNotNull(getSessionUseCase.execute(Unit).accessToken),
                    )
                val mfaCookie = getSessionUseCase.execute(Unit).mfaToken

                delay(GET_TRANSFER_LOOP_INTERVAL_DELAY_MILLIS)
                when (
                    val response =
                        runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                            viewTransferUseCase.execute(ViewTransferUseCase.Input(accessToken, mfaCookie, transferId))
                        }
                ) {
                    is ViewTransferUseCase.Output.Failure<*> -> {
                        Timber.e("Error during transfer details fetch: %s", response.response.headerMessage)
                        view?.showErrorDuringTransferDetailsFetch(response.response.headerMessage)
                    }
                    is ViewTransferUseCase.Output.Success -> {
                        transferStatus = response.transfer.status
                        if (response.transfer.currentPage > currentPage) {
                            currentPage = response.transfer.currentPage
                            view?.showQrCodeForData(qrCodePagesData[currentPage])
                        }
                    }
                }
            }
            transferPollingFinished()
        }
    }

    private fun transferPollingFinished() {
        val result =
            when (transferStatus) {
                Status.ERROR -> TransferAccountStatus.Failure("")
                Status.IN_PROGRESS -> TransferAccountStatus.Canceled()
                Status.COMPLETE -> TransferAccountStatus.Success()
                Status.CANCEL -> TransferAccountStatus.Canceled()
                Status.START -> TransferAccountStatus.Canceled()
            }
        view?.navigateToResult(result)
    }

    private fun shouldLoopForTransfer(totalPageCount: Int) =
        transferStatus in setOf(Status.START, Status.IN_PROGRESS) && currentPage < totalPageCount

    override fun cancelTransferButtonClick() {
        view?.showCancelTransferDialog()
    }

    override fun backClick() {
        view?.showCancelTransferDialog()
    }

    override fun stopTransferClick() {
        view?.navigateToResult(TransferAccountStatus.Canceled())
    }

    override fun detach() {
        coroutineScope.coroutineContext.cancelChildren()
        getTransferScope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }

    private companion object {
        private const val GET_TRANSFER_LOOP_INTERVAL_DELAY_MILLIS = 500L
    }
}
