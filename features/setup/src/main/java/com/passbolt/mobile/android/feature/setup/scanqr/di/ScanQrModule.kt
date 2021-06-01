package com.passbolt.mobile.android.feature.setup.scanqr.di

import com.google.gson.GsonBuilder
import com.passbolt.mobile.android.core.qrscan.manager.ScanManager
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrContract
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrFragment
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrPresenter
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.KeyAssembler
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.QrScanResultsMapper
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ScanQrParser
import com.passbolt.mobile.android.feature.setup.scanqr.usecase.UpdateTransferUseCase
import org.koin.core.module.Module
import org.koin.core.qualifier.named

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

internal const val SCAN_MANAGER_SCOPE = "SCAN_MANAGER_SCOPE"

fun Module.scanQrModule() {

    scope(named<ScanQrFragment>()) {
        scoped<ScanQrContract.Presenter> {
            ScanQrPresenter(get(), get(), get(), get(), get(), get(), get(), get())
        }
        scoped { QrScanResultsMapper(gson = get()) }
        scoped { KeyAssembler() }
        scoped { GsonBuilder().create() }
        scoped {
            UpdateTransferUseCase(
                registrationRepository = get(),
                updateTransferMapper = get(),
                coroutineContext = get()
            )
        }
        scoped {
            ScanQrParser(
                qrScanResultsMapper = get(),
                keyAssembler = get(),
                coroutineLaunchContext = get()
            )
        }
    }

    scope(named(SCAN_MANAGER_SCOPE)) {
        scoped {
            ScanManager(
                cameraProviderFuture = get(),
                previewUseCase = get(),
                cameraSelector = get(),
                cameraBarcodeAnalyzer = get(),
                imageAnalysisUseCase = get(),
                mainExecutor = get()
            )
        }
    }
}
