package com.passbolt.mobile.android.feature.setup.scanqr

import com.nhaarman.mockitokotlin2.mock
import com.passbolt.mobile.android.common.UserIdProvider
import com.passbolt.mobile.android.feature.setup.scanqr.usecase.UpdateTransferUseCase
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.KeyAssembler
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.QrScanResultsMapper
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ScanQrParser
import com.passbolt.mobile.android.storage.usecase.SaveAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.SavePrivateKeyUseCase
import com.passbolt.mobile.android.storage.usecase.SaveSelectedAccountUseCase
import com.passbolt.mobile.android.storage.usecase.UpdateAccountDataUseCase
import org.koin.dsl.module

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

private val updateTransferUseCase = mock<UpdateTransferUseCase>()
private val saveAccountDataUseCase = mock<SaveAccountDataUseCase>()
private val selectedAccountUseCase = mock<SaveSelectedAccountUseCase>()
private val userIdProvider = mock<UserIdProvider>()
private val savePrivateKeyUseCase = mock<SavePrivateKeyUseCase>()
private val updateAccountDataUseCase = mock<UpdateAccountDataUseCase>()

val testScanQrModule = module {
    factory<ScanQrContract.Presenter> { ScanQrPresenter(get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { updateTransferUseCase }
    factory { saveAccountDataUseCase }
    factory { selectedAccountUseCase }
    factory { userIdProvider }
    factory { savePrivateKeyUseCase }
    factory { updateAccountDataUseCase }
    factory { ScanQrParser(get(), get(), get()) }
    factory { KeyAssembler() }
    factory { QrScanResultsMapper(get()) }
}
