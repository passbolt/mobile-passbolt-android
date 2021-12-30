package com.passbolt.mobile.android.feature.authentication

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import com.passbolt.mobile.android.core.navigation.ActivityIntents
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
    fun `view should be initialized with no account list if on setup`() =
        runBlockingTest {
            whenever(mockGetSelectedAccountUseCase.execute(Unit))
                .doReturn(GetSelectedAccountUseCase.Output(null))

            presenter.bundleRetrieved(ActivityIntents.AuthConfig.Setup, USER_ID)

            verify(mockView).initNavWithoutAccountList(USER_ID)
            verifyNoMoreInteractions(mockView)
        }

    @Test
    fun `view should be initialized with account list if not on setup`() =
        runBlockingTest {
            whenever(mockGetSelectedAccountUseCase.execute(Unit))
                .doReturn(GetSelectedAccountUseCase.Output(null))

            val testForConfigurationValue: (config: ActivityIntents.AuthConfig) -> Unit = {
                reset(mockView)
                presenter.bundleRetrieved(it, null)
                verify(mockView).initNavWithAccountList()
                verifyNoMoreInteractions(mockView)
            }

            ActivityIntents.AuthConfig::class.nestedClasses
                .filter { it.objectInstance != null }
                .map { it.objectInstance as ActivityIntents.AuthConfig }
                .filter { it !is ActivityIntents.AuthConfig.Setup }
                .forEach { testForConfigurationValue(it) }
        }

    @Test
    fun `view should be initialized with account list and navigate to auth if selected account during auth`() =
        runBlockingTest {
            whenever(mockGetSelectedAccountUseCase.execute(Unit))
                .doReturn(GetSelectedAccountUseCase.Output(USER_ID))
            whenever(mockGetAccountDataUseCase.execute(any()))
                .doReturn(
                    GetAccountDataUseCase.Output(
                        firstName = "firstName",
                        lastName = "lastName",
                        email = "email",
                        avatarUrl = "avatarUrl",
                        serverId = "serverId",
                        url = "https://passbolt.com",
                        label = "label"
                    )
                )

            val testForConfigurationValue: (config: ActivityIntents.AuthConfig) -> Unit = {
                reset(mockView)
                presenter.bundleRetrieved(it, null)
                verify(mockView).initNavWithAccountList()
                verify(mockView).navigateToSignIn(USER_ID)
                verifyNoMoreInteractions(mockView)
            }

            ActivityIntents.AuthConfig::class.nestedClasses
                .filter { it.objectInstance != null }
                .map { it.objectInstance as ActivityIntents.AuthConfig }
                .filter {
                    it !in setOf(
                        ActivityIntents.AuthConfig.ManageAccount,
                        ActivityIntents.AuthConfig.Setup
                    )
                }
                .forEach { testForConfigurationValue(it) }
        }

    private companion object {
        private const val USER_ID = "aaa-bbb-ccc"
    }
}
