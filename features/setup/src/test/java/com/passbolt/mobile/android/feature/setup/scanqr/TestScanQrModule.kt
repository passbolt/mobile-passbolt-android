package com.passbolt.mobile.android.feature.setup.scanqr

import com.nhaarman.mockitokotlin2.mock
import com.passbolt.mobile.android.common.HttpsVerifier
import com.passbolt.mobile.android.common.UuidProvider
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.KeyAssembler
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.QrScanResultsMapper
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ScanQrParser
import com.passbolt.mobile.android.feature.setup.scanqr.usecase.UpdateTransferUseCase
import com.passbolt.mobile.android.storage.usecase.account.SaveAccountUseCase
import com.passbolt.mobile.android.storage.usecase.accountdata.SaveAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.privatekey.SavePrivateKeyUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.SaveSelectedAccountUseCase
import com.passbolt.mobile.android.storage.usecase.accountdata.UpdateAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.accounts.CheckAccountExistsUseCase
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

internal val updateTransferUseCase = mock<UpdateTransferUseCase>()
internal val saveAccountDataUseCase = mock<SaveAccountDataUseCase>()
internal val uuidProvider = mock<UuidProvider>()
internal val savePrivateKeyUseCase = mock<SavePrivateKeyUseCase>()
internal val updateAccountDataUseCase = mock<UpdateAccountDataUseCase>()
internal val addAccountUseCase = mock<SaveAccountUseCase>()
internal val checkAccountExistsUseCase = mock<CheckAccountExistsUseCase>()
internal val qrParser = mock<ScanQrParser>()
internal val httpsVerifier = mock<HttpsVerifier>()
internal val selectedAccountUseCase = mock<SaveSelectedAccountUseCase>()

val testScanQrModule = module {
    factory { httpsVerifier }
    factory { updateTransferUseCase }
    factory { saveAccountDataUseCase }
    factory { selectedAccountUseCase }
    factory { uuidProvider }
    factory { savePrivateKeyUseCase }
    factory { updateAccountDataUseCase }
    factory { addAccountUseCase }
    factory { checkAccountExistsUseCase }
    factory {
        ScanQrParser(
            coroutineLaunchContext = get(),
            qrScanResultsMapper = get(),
            keyAssembler = get()
        )
    }
    factory {
        KeyAssembler(
            gson = get()
        )
    }
    factory { QrScanResultsMapper() }
    factory<ScanQrContract.Presenter> {
        ScanQrPresenter(
            coroutineLaunchContext = get(),
            qrParser = qrParser,
            updateTransferUseCase = get(),
            saveAccountDataUseCase = get(),
            uuidProvider = get(),
            savePrivateKeyUseCase = get(),
            updateAccountDataUseCase = get(),
            checkAccountExistsUseCase = get(),
            httpsVerifier = get(),
            saveSelectedAccountUseCase = selectedAccountUseCase
        )
    }
}
