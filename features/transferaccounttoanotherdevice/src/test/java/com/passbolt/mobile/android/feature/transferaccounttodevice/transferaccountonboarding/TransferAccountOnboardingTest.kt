package com.passbolt.mobile.android.feature.transferaccounttodevice.transferaccountonboarding

import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccountonboarding.TransferAccountOnboardingContract
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

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

class TransferAccountOnboardingTest : KoinTest {
    private val presenter: TransferAccountOnboardingContract.Presenter by inject()
    private val view: TransferAccountOnboardingContract.View = mock()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(transferAccountOnboardingModule)
        }

    @Test
    fun `start transfer clicked should authenticate and then navigate to transfer account`() {
        presenter.attach(view)
        presenter.startTransferButtonClick()
        presenter.authenticationSucceeded()

        verify(view).navigateToRefreshPassphrase()
        verify(view).navigateToTransferAccount()
    }
}
