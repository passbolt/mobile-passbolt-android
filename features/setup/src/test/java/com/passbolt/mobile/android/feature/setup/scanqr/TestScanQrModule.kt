package com.passbolt.mobile.android.feature.setup.scanqr

import com.nhaarman.mockitokotlin2.mock
import com.passbolt.mobile.android.common.UserIdProvider
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.KeyAssembler
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.QrScanResultsMapper
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ScanQrParser
import com.passbolt.mobile.android.feature.setup.scanqr.usecase.NextPageUseCase
import com.passbolt.mobile.android.storage.usecase.SaveAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.SavePrivateKeyUseCase
import com.passbolt.mobile.android.storage.usecase.SaveSelectedAccountUseCase
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

internal val nextPageUseCase = mock<NextPageUseCase>()
internal val saveAccountDataUseCase = mock<SaveAccountDataUseCase>()
internal val selectedAccountUseCase = mock<SaveSelectedAccountUseCase>()
internal val userIdProvider = mock<UserIdProvider>()
internal val savePrivateKeyUseCase = mock<SavePrivateKeyUseCase>()

val testScanQrModule = module {
    factory { nextPageUseCase }
    factory { saveAccountDataUseCase }
    factory { selectedAccountUseCase }
    factory { userIdProvider }
    factory { savePrivateKeyUseCase }
    factory { ScanQrParser(get(), get(), get()) }
    factory { KeyAssembler() }
    factory { QrScanResultsMapper(get()) }
    factory<ScanQrContract.Presenter> { ScanQrPresenter(get(), get(), get(), get(), get(), get(), get()) }
}
