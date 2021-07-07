package com.passbolt.mobile.android.feature.authentication

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import com.passbolt.mobile.android.core.navigation.AuthenticationTarget
import com.passbolt.mobile.android.core.navigation.AuthenticationType
import com.passbolt.mobile.android.feature.mockGetSelectedAccountUseCase
import com.passbolt.mobile.android.feature.mockLogoutRepository
import com.passbolt.mobile.android.feature.testAuthenticationMainModule
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import org.junit.After
import org.junit.Before
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

class AuthenticationMainPresenterTest : KoinTest {

    private val presenter: AuthenticationMainContract.Presenter by inject()
    private val mockView = mock<AuthenticationMainContract.View>()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testAuthenticationMainModule)
    }

    @Before
    fun setup() {
        presenter.attach(mockView)
    }

    @After
    fun tearDown() {
        reset(mockGetSelectedAccountUseCase)
    }

    @Test
    fun `view should navigate to manage account when manage account target is set`() {
        presenter.bundleRetrieved(AuthenticationTarget.MANAGE_ACCOUNTS, null, shouldLogOut = false)

        verify(mockView, never()).showProgress()
        verify(mockLogoutRepository, never()).logout()
        verify(mockView).navigateToManageAccounts()
        verifyNoMoreInteractions(mockView)
    }

    @Test
    fun `user should be logged out when appropriate flag is set`() {
        presenter.bundleRetrieved(AuthenticationTarget.MANAGE_ACCOUNTS, null, shouldLogOut = true)

        verify(mockView).showProgress()
        verify(mockLogoutRepository).logout()
        verify(mockView).hideProgress()
        verify(mockView).navigateToManageAccounts()
        verifyNoMoreInteractions(mockView)
    }

    @Test
    fun `view should navigate to auth with passphrase mode when appropriate flags are set`() {
        whenever(mockGetSelectedAccountUseCase.execute(Unit))
            .doReturn(
                GetSelectedAccountUseCase.Output(TEST_SELECTED_ACCOUNT)
            )

        presenter.bundleRetrieved(
            AuthenticationTarget.AUTHENTICATE,
            AuthenticationType.PASSPHRASE,
            shouldLogOut = false
        )

        verify(mockView).navigateToAuth(TEST_SELECTED_ACCOUNT, AuthenticationType.PASSPHRASE)
        verifyNoMoreInteractions(mockView)
    }

    @Test
    fun `view should navigate to auth with sign in mode when appropriate flags are set`() {
        whenever(mockGetSelectedAccountUseCase.execute(Unit))
            .doReturn(
                GetSelectedAccountUseCase.Output(TEST_SELECTED_ACCOUNT)
            )

        presenter.bundleRetrieved(
            AuthenticationTarget.AUTHENTICATE,
            AuthenticationType.SIGN_IN,
            shouldLogOut = false
        )

        verify(mockView).navigateToAuth(TEST_SELECTED_ACCOUNT, AuthenticationType.SIGN_IN)
        verifyNoMoreInteractions(mockView)
    }

    @Test
    fun `view should stay on account list to choose when no account is selected`() {
        whenever(mockGetSelectedAccountUseCase.execute(Unit))
            .thenThrow(IllegalStateException())

        presenter.bundleRetrieved(
            AuthenticationTarget.AUTHENTICATE,
            AuthenticationType.SIGN_IN,
            shouldLogOut = false
        )

        verifyNoMoreInteractions(mockView)
    }

    private companion object {
        private const val TEST_SELECTED_ACCOUNT = "aaa-bbb-ccc"
    }
}
