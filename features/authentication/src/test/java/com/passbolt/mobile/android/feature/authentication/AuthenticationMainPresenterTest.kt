package com.passbolt.mobile.android.feature.authentication

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import com.passbolt.mobile.android.core.navigation.AuthenticationTarget
import com.passbolt.mobile.android.core.navigation.AuthenticationType
import com.passbolt.mobile.android.storage.usecase.accountdata.GetAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
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

@ExperimentalCoroutinesApi
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
    fun `view should navigate to manage account when manage account target is set`() = runBlockingTest {
        presenter.bundleRetrieved(AuthenticationTarget.MANAGE_ACCOUNTS, null, shouldLogOut = false, userId = "userId1")

        verify(mockView, never()).showProgress()
        verify(mockSignOutUseCase, never()).execute(any())
        verify(mockView).navigateToManageAccounts()
        verifyNoMoreInteractions(mockView)
    }

    @Test
    fun `user should be logged out when appropriate flag is set`() = runBlockingTest {
        presenter.bundleRetrieved(AuthenticationTarget.MANAGE_ACCOUNTS, null, shouldLogOut = true, userId = "userId1")

        verify(mockView).showProgress()
        verify(mockSignOutUseCase).execute(any())
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
        whenever(getAccountDataUseCase.execute(anyOrNull())).doReturn(
            GetAccountDataUseCase.Output(
                firstName = null,
                lastName = null,
                email = null,
                avatarUrl = null,
                url = "url1",
                serverId = null
            )
        )

        presenter.bundleRetrieved(
            AuthenticationTarget.AUTHENTICATE,
            AuthenticationType.Passphrase,
            shouldLogOut = false,
            userId = TEST_SELECTED_ACCOUNT
        )

        verify(mockView).navigateToAuth(TEST_SELECTED_ACCOUNT, AuthenticationType.Passphrase)
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
            AuthenticationType.SignIn,
            shouldLogOut = false,
            userId = TEST_SELECTED_ACCOUNT
        )

        verify(mockView).navigateToAuth(TEST_SELECTED_ACCOUNT, AuthenticationType.SignIn)
        verifyNoMoreInteractions(mockView)
    }

    @Test
    fun `view should stay on account list to choose when no account is selected`() {
        whenever(mockGetSelectedAccountUseCase.execute(Unit))
            .thenThrow(IllegalStateException())

        presenter.bundleRetrieved(
            AuthenticationTarget.AUTHENTICATE,
            AuthenticationType.SignIn,
            shouldLogOut = false,
            userId = null
        )

        verify(mockView).setDefaultNavGraph()
        verifyNoMoreInteractions(mockView)
    }

    private companion object {
        private const val TEST_SELECTED_ACCOUNT = "aaa-bbb-ccc"
    }
}
