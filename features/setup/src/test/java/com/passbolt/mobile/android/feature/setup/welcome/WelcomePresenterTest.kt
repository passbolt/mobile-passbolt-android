package com.passbolt.mobile.android.feature.setup.welcome

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import com.passbolt.mobile.android.feature.setup.di.testModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject

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
@ExperimentalCoroutinesApi
class WelcomePresenterTest : KoinTest {

    private val presenter: WelcomeContract.Presenter by inject()
    private var view: WelcomeContract.View = mock()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testModule, welcomeModule)
    }

    @Test
    fun `click no account button should display account creation info dialog`() {
        presenter.attach(view)
        presenter.noAccountButtonClick()

        verify(view).showAccountCreationInfoDialog()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `click connect to account should navigate to transfer details`() {
        presenter.attach(view)
        presenter.connectToAccountClick()

        verify(view).navigateToTransferDetails()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `view should show root warning when detected`() {
        whenever(mockRootDetector.isDeviceRooted()).doReturn(true)

        presenter.attach(view)
        presenter.argsRetrieved(isTaskRoot = true)

        verify(view).showDeviceRootedDialog()
    }
}
