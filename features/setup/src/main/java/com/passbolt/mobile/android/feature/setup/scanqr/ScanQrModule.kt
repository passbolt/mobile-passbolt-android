package com.passbolt.mobile.android.feature.setup.scanqr

import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.KeyAssembler
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.QrScanResultsMapper
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ScanQrParser
import com.passbolt.mobile.android.feature.setup.scanqr.usecase.UpdateTransferUseCase
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.scopedOf
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

fun Module.scanQrModule() {
    scope(named<ScanQrFragment>()) {
        scoped<ScanQrContract.Presenter> {
            ScanQrPresenter(
                coroutineLaunchContext = get(),
                updateTransferUseCase = get(),
                qrParser = get(),
                uuidProvider = get(),
                savePrivateKeyUseCase = get(),
                updateAccountDataUseCase = get(),
                checkAccountExistsUseCase = get(),
                httpsVerifier = get(),
                saveCurrentApiUrlUseCase = get(),
                accountsInteractor = get(),
                accountKitParser = get()
            )
        }
        scopedOf(::QrScanResultsMapper)
        scopedOf(::KeyAssembler)
        scopedOf(::UpdateTransferUseCase)
        scopedOf(::ScanQrParser)
    }
    single { Json { ignoreUnknownKeys = true } }
}
