package com.password.mobile.android.feature.home.switchaccount.compose

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.accounts.usecase.accounts.GetAllAccountsDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.SaveSelectedAccountUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.entity.account.Account
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.home.switchaccount.compose.SwitchAccountIntent
import com.passbolt.mobile.android.feature.home.switchaccount.compose.SwitchAccountSideEffect
import com.passbolt.mobile.android.feature.home.switchaccount.compose.SwitchAccountViewModel
import com.passbolt.mobile.android.mappers.SwitchAccountModelMapper
import com.passbolt.mobile.android.mappers.comparator.SwitchAccountUiModelComparator
import com.passbolt.mobile.android.ui.SwitchAccountUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime

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

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class SwitchAccountViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.Companion.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<GetAllAccountsDataUseCase>() }
                        single { mock<SignOutUseCase>() }
                        single { mock<SaveSelectedAccountUseCase>() }
                        single { mock<FullDataRefreshExecutor>() }
                        single { mock<GetSelectedAccountUseCase>() }
                        singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                        factoryOf(::SwitchAccountViewModel)
                        factoryOf(::SwitchAccountModelMapper)
                        factoryOf(::SwitchAccountUiModelComparator)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: SwitchAccountViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val getAllAccountsDataUseCase: GetAllAccountsDataUseCase = get()
        val accounts =
            listOf(
                Account(
                    userId = "id1",
                    firstName = "firstName1",
                    lastName = "lastName1",
                    email = "email1",
                    avatarUrl = "avatarUrl1",
                    url = "url1",
                    serverId = "serverId1",
                    label = "label",
                ),
            )
        whenever(getAllAccountsDataUseCase.execute(Unit)) doReturn GetAllAccountsDataUseCase.Output(accounts)

        val getSelectedAccountUseCase: GetSelectedAccountUseCase = get()
        whenever(getSelectedAccountUseCase.execute(Unit)) doReturn GetSelectedAccountUseCase.Output("id1")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should initialize with one account`() =
        runTest {
            val getAllAccountsDataUseCase: GetAllAccountsDataUseCase = get()
            val accounts =
                listOf(
                    Account(
                        userId = "id1",
                        firstName = "firstName1",
                        lastName = "lastName1",
                        email = "email1",
                        avatarUrl = "avatarUrl1",
                        url = "url1",
                        serverId = "serverId1",
                        label = "label",
                    ),
                )
            whenever(getAllAccountsDataUseCase.execute(Unit)) doReturn GetAllAccountsDataUseCase.Output(accounts)

            val getSelectedAccountUseCase: GetSelectedAccountUseCase = get()
            whenever(getSelectedAccountUseCase.execute(Unit)) doReturn GetSelectedAccountUseCase.Output("id1")

            viewModel = get()
            viewModel.onIntent(SwitchAccountIntent.Initialize(AppContext.APP))

            val state = viewModel.viewState.value
            assertThat(state.appContext).isEqualTo(AppContext.APP)
            assertThat(state.accountsList).hasSize(2)

            assertIs<SwitchAccountUiModel.HeaderItem>(state.accountsList[0])
            val headerItem = state.accountsList[0] as SwitchAccountUiModel.HeaderItem
            assertThat(headerItem.label).isEqualTo(accounts[0].label)
            assertThat(headerItem.email).isEqualTo(accounts[0].email)
            assertThat(headerItem.avatarUrl).isEqualTo(accounts[0].avatarUrl)

            assertIs<SwitchAccountUiModel.ManageAccountsItem>(state.accountsList[1])
        }

    @Test
    fun `should initialize with more than one account`() =
        runTest {
            val getAllAccountsDataUseCase: GetAllAccountsDataUseCase = get()
            val accounts =
                listOf(
                    Account(
                        userId = "id1",
                        firstName = "firstName1",
                        lastName = "lastName1",
                        email = "email1",
                        avatarUrl = "avatarUrl1",
                        url = "url1",
                        serverId = "serverId1",
                        label = "label1",
                    ),
                    Account(
                        userId = "id2",
                        firstName = "firstName2",
                        lastName = "lastName2",
                        email = "email2",
                        avatarUrl = "avatarUrl2",
                        url = "url2",
                        serverId = "serverId2",
                        label = "label2",
                    ),
                )
            whenever(getAllAccountsDataUseCase.execute(Unit)) doReturn GetAllAccountsDataUseCase.Output(accounts)

            viewModel = get()
            viewModel.onIntent(SwitchAccountIntent.Initialize(AppContext.APP))

            val state = viewModel.viewState.value
            assertThat(state.appContext).isEqualTo(AppContext.APP)
            assertThat(state.accountsList).hasSize(3)

            assertIs<SwitchAccountUiModel.HeaderItem>(state.accountsList[0])
            val headerItem = state.accountsList[0] as SwitchAccountUiModel.HeaderItem
            assertThat(headerItem.label).isEqualTo(accounts[0].label)
            assertThat(headerItem.email).isEqualTo(accounts[0].email)
            assertThat(headerItem.avatarUrl).isEqualTo(accounts[0].avatarUrl)

            assertIs<SwitchAccountUiModel.AccountItem>(state.accountsList[1])
            val accountItem = state.accountsList[1] as SwitchAccountUiModel.AccountItem
            assertThat(accountItem.userId).isEqualTo(accounts[1].userId)
            assertThat(accountItem.label).isEqualTo(accounts[1].label)
            assertThat(accountItem.email).isEqualTo(accounts[1].email)
            assertThat(accountItem.avatarUrl).isEqualTo(accounts[1].avatarUrl)

            assertIs<SwitchAccountUiModel.ManageAccountsItem>(state.accountsList[2])
        }

    @Test
    fun `should navigate to account details when see current account details intent is received`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(SwitchAccountIntent.SeeCurrentAccountDetails)

                val sideEffect = expectItem()
                assertIs<SwitchAccountSideEffect.NavigateToAccountDetails>(sideEffect)
            }
        }

    @Test
    fun `should sign out with confirmation dialog when sign out intent is received`() =
        runTest {
            viewModel = get()

            val initialState = viewModel.viewState.value
            assertThat(initialState.showSignOutDialog).isFalse()

            viewModel.onIntent(SwitchAccountIntent.Initialize(AppContext.APP))
            viewModel.onIntent(SwitchAccountIntent.SignOut)

            viewModel.viewState.test {
                val showDialogState = expectItem()
                assertThat(showDialogState.showSignOutDialog).isTrue()

                viewModel.onIntent(SwitchAccountIntent.SignOutConfirmed)

                val signOutState = expectItem()
                assertThat(signOutState.showSignOutDialog).isFalse()
                assertThat(signOutState.showProgress).isTrue()

                val doneSignOut = expectItem()
                assertThat(doneSignOut.showProgress).isFalse()

                val signOutUseCase = get<SignOutUseCase>()
                verify(signOutUseCase).execute(Unit)

                val fullDataRefreshExecutor = get<FullDataRefreshExecutor>()
                verify(fullDataRefreshExecutor).awaitFinish()
            }

            viewModel.sideEffect.test {
                val sideEffect = expectItem()
                assertIs<SwitchAccountSideEffect.NavigateToStartup>(sideEffect)
                assertThat(sideEffect.appContext).isEqualTo(AppContext.APP)
            }
        }

    @Test
    fun `should close sign out dialog when close sign out dialog intent is received`() =
        runTest {
            viewModel = get()

            viewModel.onIntent(SwitchAccountIntent.Initialize(AppContext.APP))
            viewModel.onIntent(SwitchAccountIntent.SignOut)

            assertThat(viewModel.viewState.value.showSignOutDialog).isTrue()

            viewModel.onIntent(SwitchAccountIntent.CloseSignOutDialog)

            assertThat(viewModel.viewState.value.showSignOutDialog).isFalse()
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should switch account and navigate to sign in when switch account intent is received`() =
        runTest {
            val getAllAccountsDataUseCase: GetAllAccountsDataUseCase = get()
            val accounts =
                listOf(
                    Account(
                        userId = "id1",
                        firstName = "firstName1",
                        lastName = "lastName1",
                        email = "email1",
                        avatarUrl = "avatarUrl1",
                        url = "url1",
                        serverId = "serverId1",
                        label = "label1",
                    ),
                    Account(
                        userId = "id2",
                        firstName = "firstName2",
                        lastName = "lastName2",
                        email = "email2",
                        avatarUrl = "avatarUrl2",
                        url = "url2",
                        serverId = "serverId2",
                        label = "label2",
                    ),
                )
            whenever(getAllAccountsDataUseCase.execute(Unit)) doReturn GetAllAccountsDataUseCase.Output(accounts)

            viewModel = get()
            viewModel.onIntent(SwitchAccountIntent.Initialize(AppContext.APP))

            viewModel.sideEffect.test {
                viewModel.onIntent(
                    SwitchAccountIntent.SwitchAccount(
                        SwitchAccountUiModel.AccountItem(
                            userId = accounts[1].userId,
                            label = accounts[1].label!!,
                            email = accounts[1].email!!,
                            avatarUrl = accounts[1].avatarUrl,
                        ),
                    ),
                )

                val sideEffect = expectItem()
                assertIs<SwitchAccountSideEffect.NavigateToSignInForAccount>(sideEffect)
                assertThat(sideEffect.appContext).isEqualTo(AppContext.APP)

                val saveSelectedAccountUseCase = get<SaveSelectedAccountUseCase>()
                verify(saveSelectedAccountUseCase).execute(UserIdInput(accounts[1].userId))
            }
        }

    @Test
    fun `should navigate to manage accounts when manage accounts intent is received`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(SwitchAccountIntent.ManageAccounts)

                val sideEffect = expectItem()
                assertIs<SwitchAccountSideEffect.NavigateToManageAccounts>(sideEffect)
            }
        }
}
