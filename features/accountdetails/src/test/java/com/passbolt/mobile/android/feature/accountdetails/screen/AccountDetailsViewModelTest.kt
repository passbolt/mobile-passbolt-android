package com.passbolt.mobile.android.feature.accountdetails.screen

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
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.UpdateAccountDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.accountdetails.screen.AccountDetailsIntent.SaveChanges
import com.passbolt.mobile.android.feature.accountdetails.screen.AccountDetailsIntent.StartTransferAccount
import com.passbolt.mobile.android.feature.accountdetails.screen.AccountDetailsIntent.UpdateLabel
import com.passbolt.mobile.android.feature.accountdetails.screen.AccountDetailsScreenSideEffect.NavigateUp
import com.passbolt.mobile.android.feature.accountdetails.screen.AccountDetailsValidationError.MaxLengthExceeded
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

@OptIn(ExperimentalCoroutinesApi::class)
class AccountDetailsViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<GetSelectedAccountDataUseCase>() }
                        single { mock<GetSelectedAccountUseCase>() }
                        single { mock<UpdateAccountDataUseCase>() }
                        singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                        factoryOf(::AccountDetailsViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: AccountDetailsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val getSelectedAccountDataUseCase = get<GetSelectedAccountDataUseCase>()
        whenever(getSelectedAccountDataUseCase.execute(Unit)) doReturn selectedAccountData

        val getSelectedAccountUseCase = get<GetSelectedAccountUseCase>()
        whenever(getSelectedAccountUseCase.execute(Unit)) doReturn GetSelectedAccountUseCase.Output(SELECTED_ACCOUNT_ID)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `account details should be shown initially`() =
        runTest {
            viewModel = get()

            viewModel.viewState.test {
                val state = expectItem()
                assertThat(state.label).isEqualTo(LABEL)
                assertThat(state.avatarUrl).isEqualTo(AVATAR_URL)
                assertThat(state.organizationUrl).isEqualTo(SERVER_URL)
                assertThat(state.email).isEqualTo(EMAIL)
                assertThat(state.name).isEqualTo("$FIRST_NAME $LAST_NAME")
                assertThat(state.role).isEqualTo(ROLE)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `label should be updated on save click`() =
        runTest {
            val newLabel = "New Label"
            val updateAccountDataUseCase = get<UpdateAccountDataUseCase>()

            viewModel = get()

            viewModel.viewState.test {
                // default
                expectItem()

                viewModel.onIntent(UpdateLabel(newLabel))

                val updatedState = expectItem()
                assertThat(updatedState.label).isEqualTo(newLabel)

                viewModel.onIntent(SaveChanges)
            }

            verify(updateAccountDataUseCase).execute(
                UpdateAccountDataUseCase.Input(
                    userId = SELECTED_ACCOUNT_ID,
                    label = newLabel,
                ),
            )

            viewModel.sideEffect.test {
                assertIs<NavigateUp>(expectItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `transfer account to another device clicked should navigate to transfer account onboarding`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(StartTransferAccount)

                assertIs<AccountDetailsScreenSideEffect.NavigateToTransferAccount>(expectItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `when label exceeds max length then validation error is shown`() =
        runTest {
            viewModel = get()

            val tooLongLabel = "a".repeat(AccountDetailsViewModel.LABEL_MAX_LENGTH + 1)

            viewModel.viewState.test {
                // default
                expectItem()

                viewModel.onIntent(UpdateLabel(tooLongLabel))
                assertThat(expectItem().label).isEqualTo(tooLongLabel)

                viewModel.onIntent(SaveChanges)

                assertThat(expectItem().labelValidationErrors).contains(MaxLengthExceeded(64))
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `when label is valid then save succeeds and navigates up`() =
        runTest {
            val validLabel = "Valid Label"
            val updateAccountDataUseCase = get<UpdateAccountDataUseCase>()

            viewModel = get()

            viewModel.onIntent(UpdateLabel(validLabel))
            viewModel.onIntent(SaveChanges)

            verify(updateAccountDataUseCase).execute(
                UpdateAccountDataUseCase.Input(
                    userId = SELECTED_ACCOUNT_ID,
                    label = validLabel,
                ),
            )

            viewModel.sideEffect.test {
                assertIs<NavigateUp>(expectItem())
            }
        }

    private companion object Companion {
        private const val FIRST_NAME = "first"
        private const val LAST_NAME = "last"
        private const val EMAIL = "email"
        private const val AVATAR_URL = "avatarUrl"
        private const val SERVER_URL = "serverUrl"
        private const val SERVER_ID = "serverId"
        private const val LABEL = "label"
        private const val ROLE = "user"
        private const val SELECTED_ACCOUNT_ID = "selected"

        private val selectedAccountData =
            GetSelectedAccountDataUseCase.Output(
                firstName = FIRST_NAME,
                lastName = LAST_NAME,
                email = EMAIL,
                avatarUrl = AVATAR_URL,
                url = SERVER_URL,
                serverId = SERVER_ID,
                label = LABEL,
                role = ROLE,
            )
    }
}
