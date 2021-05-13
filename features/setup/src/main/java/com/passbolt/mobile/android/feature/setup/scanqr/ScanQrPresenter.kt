package com.passbolt.mobile.android.feature.setup.scanqr

import com.passbolt.mobile.android.core.mvp.CoroutineLaunchContext
import com.passbolt.mobile.android.core.networking.UserIdProvider
import com.passbolt.mobile.android.core.qrscan.analyzer.CameraBarcodeAnalyzer
import com.passbolt.mobile.android.feature.setup.scanqr.usecase.NextPageUseCase
import com.passbolt.mobile.android.feature.setup.summary.ResultStatus
import com.passbolt.mobile.android.storage.usecase.SaveAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.SaveSelectedAccountUseCase
import com.passbolt.mobile.android.ui.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val nextPageUseCase: NextPageUseCase,
    private val qrParser: ScanQrParser,
    private val saveSelectedAccountUseCase: SaveSelectedAccountUseCase,
    private val saveAccountDataUseCase: SaveAccountDataUseCase,
    private val userIdProvider: UserIdProvider
) : ScanQrContract.Presenter {

    override var view: ScanQrContract.View? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    private lateinit var authToken: String
    private lateinit var transferUuid: String
    private var totalPages: Int by Delegates.notNull()
    private var currentPage = 0

    override fun attach(view: ScanQrContract.View) {
        super.attach(view)
        registerForScanResults()
        registerForParseResults()
        this.view?.startAnalysis()
    }

    private fun registerForParseResults() {
        qrParser.parseResultsChannel.let { barcodeParseResultChannel ->
            scope.launch {
                barcodeParseResultChannel
                    .consumeAsFlow()
                    .distinctUntilChanged()
                    .collect {
                        when (it) {
                            is ScanQrParser.ParseResult.FirstPage -> processFirstPage(it)
                            is ScanQrParser.ParseResult.SubsequentPage -> processSubsequentPage(it)
                            is ScanQrParser.ParseResult.Error -> processError()
                        }
                    }
            }
        }
    }

    private suspend fun processError() {
        updateTransfer(pageNumber = currentPage, Status.ERROR)
        view?.navigateToSummary(ResultStatus.Failure(""))
    }

    private suspend fun processFirstPage(firstPage: ScanQrParser.ParseResult.FirstPage) {
        transferUuid = firstPage.content.transferId
        authToken = firstPage.content.authenticationToken
        totalPages = firstPage.content.totalPages
        currentPage = 0
        view?.initializeProgress(totalPages - 1)
        view?.showKeepGoing()
        saveAccountDetails(firstPage.content.userId, firstPage.content.domain)
        updateTransfer(pageNumber = firstPage.reservedBytesDto.page + 1)
    }

    private fun saveAccountDetails(id: String, url: String) {
        val userId = userIdProvider.get(id, url)
        saveSelectedAccountUseCase.execute(SaveSelectedAccountUseCase.Input(userId))
        saveAccountDataUseCase.execute(SaveAccountDataUseCase.Input(userId, url))
    }

    private suspend fun processSubsequentPage(subsequentPage: ScanQrParser.ParseResult.SubsequentPage) {
        currentPage = subsequentPage.reservedBytesDto.page
        view?.showKeepGoing()
        if (subsequentPage.reservedBytesDto.page < totalPages - 1) {
            updateTransfer(pageNumber = currentPage + 1)
        } else {
            qrParser.assembleKey()
            updateTransfer(pageNumber = currentPage, Status.COMPLETE)
            view?.navigateToSummary(ResultStatus.Success)
        }
    }

    private suspend fun updateTransfer(pageNumber: Int, status: Status = Status.IN_PROGRESS) {
        val response = nextPageUseCase.execute(
            NextPageUseCase.Input(
                uuid = transferUuid,
                authToken = authToken,
                currentPage = pageNumber,
                status = status
            )
        )
        when (response) {
            is NextPageUseCase.Output.Failure -> {
                // TODO prepare error handler class
                Timber.e("There was an error during transfer update")
                processError()
            }
            is NextPageUseCase.Output.Success -> view?.setProgress(pageNumber)
        }
    }

    private fun registerForScanResults() {
        view?.scanResultChannel()?.let { barcodeScanResultChannel ->
            scope.launch {
                for (result in barcodeScanResultChannel) {
                    barcodeResult(result)
                }
            }
        }
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
        view?.showInformationDialog()
    }

    private fun barcodeResult(barcodeScanResult: CameraBarcodeAnalyzer.BarcodeScanResult) {
        when (barcodeScanResult) {
            is CameraBarcodeAnalyzer.BarcodeScanResult.MultipleBarcodes -> view?.showMultipleCodesInRange()
            is CameraBarcodeAnalyzer.BarcodeScanResult.NoBarcodeInRange -> view?.showCenterCameraOnBarcode()
            is CameraBarcodeAnalyzer.BarcodeScanResult.Failure -> scope.launch {
                view?.navigateToSummary(ResultStatus.Failure(""))
            }
            is CameraBarcodeAnalyzer.BarcodeScanResult.SingleBarcode -> scope.launch {
                qrParser.process(barcodeScanResult.data)
            }
        }
    }

    override fun detach() {
        scope.cancel()
        super.detach()
    }
}
