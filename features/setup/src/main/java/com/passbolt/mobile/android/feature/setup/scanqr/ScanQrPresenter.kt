package com.passbolt.mobile.android.feature.setup.scanqr

import com.passbolt.mobile.android.core.mvp.CoroutineLaunchContext
import com.passbolt.mobile.android.core.qrscan.analyzer.CameraBarcodeAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

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
class ScanQrPresenter @Inject constructor(
    coroutineLaunchContext: CoroutineLaunchContext
) : ScanQrContract.Presenter {

    override var view: ScanQrContract.View? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.io)

    override fun attach(view: ScanQrContract.View) {
        super.attach(view)
        registerForScanResults()
        this.view?.startAnalysis()
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

    override fun barcodeResult(it: CameraBarcodeAnalyzer.BarcodeScanResult) {
        when (it) {
            is CameraBarcodeAnalyzer.BarcodeScanResult.Failure -> Timber.e(it.exception)
            is CameraBarcodeAnalyzer.BarcodeScanResult.Success -> Timber.e(it.barcodes.joinToString())
        }
    }

    override fun detach() {
        scope.cancel()
        super.detach()
    }
}
