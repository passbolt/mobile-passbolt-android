package com.passbolt.mobile.android.feature.transferaccounttodevice.transferaccount

import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.authenticationcore.session.GetSessionUseCase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountContract
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountPresenter
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.data.CreateTransferInputParametersGenerator
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.data.TransferQrCodesDataGenerator
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.usecase.CreateTransferUseCase
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.usecase.ViewTransferUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.dsl.module
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

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

internal val mockCreateTransferUseCase = mock<CreateTransferUseCase>()
internal val mockViewTransferUseCase = mock<ViewTransferUseCase>()
internal val mockCreateTransferInputParametersGenerator = mock<CreateTransferInputParametersGenerator>()
internal val mockTransferQrCodesDataGenerator = mock<TransferQrCodesDataGenerator>()
internal val mockGetSessionUseCase =
    mock<GetSessionUseCase> {
        on { execute(Unit) }.doReturn(
            GetSessionUseCase.Output(
                accessToken = "accessToken",
                refreshToken = "refreshToken",
                mfaToken = "mfaToken",
            ),
        )
    }

@ExperimentalCoroutinesApi
val transferAccountModule =
    module {
        factory<TransferAccountContract.Presenter> {
            TransferAccountPresenter(
                coroutineLaunchContext = get(),
                createTransferUseCase = mockCreateTransferUseCase,
                viewTransferUseCase = mockViewTransferUseCase,
                createTransferInputParametersGenerator = mockCreateTransferInputParametersGenerator,
                transferQrCodesDataGenerator = mockTransferQrCodesDataGenerator,
                getSessionUseCase = mockGetSessionUseCase,
                transferAccountIdlingResource = mock(),
            )
        }
        factory<CoroutineLaunchContext> { TestCoroutineLaunchContext() }
    }
