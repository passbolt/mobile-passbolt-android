package com.passbolt.mobile.android.feature.setup.summary

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
import com.passbolt.mobile.android.common.UuidProvider
import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.accounts.usecase.account.SaveAccountUseCase
import com.passbolt.mobile.android.database.usecase.SaveResourcesDatabasePassphraseUseCase
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.AccessLogs
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.AuthenticationSuccess
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.ConfirmSetupLeave
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.DismissHelpMenu
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.DismissSetupLeave
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.GoBack
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.Initialize
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.OpenHelpMenu
import com.passbolt.mobile.android.feature.setup.summary.SummaryIntent.PrimaryButtonAction
import com.passbolt.mobile.android.feature.setup.summary.SummarySideEffect.NavigateToAppStart
import com.passbolt.mobile.android.feature.setup.summary.SummarySideEffect.NavigateToFingerprintSetup
import com.passbolt.mobile.android.feature.setup.summary.SummarySideEffect.NavigateToLogs
import com.passbolt.mobile.android.feature.setup.summary.SummarySideEffect.NavigateToManageAccounts
import com.passbolt.mobile.android.feature.setup.summary.SummarySideEffect.NavigateToSignIn
import com.passbolt.mobile.android.feature.setup.summary.SummarySideEffect.NavigateToWelcome
import com.passbolt.mobile.android.ui.ResultStatus.AlreadyLinked
import com.passbolt.mobile.android.ui.ResultStatus.Failure
import com.passbolt.mobile.android.ui.ResultStatus.HttpNotSupported
import com.passbolt.mobile.android.ui.ResultStatus.NoNetwork
import com.passbolt.mobile.android.ui.ResultStatus.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
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
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<SaveAccountUseCase>() }
                        single { mock<SaveResourcesDatabasePassphraseUseCase>() }
                        single { mock<UuidProvider>() }
                        factoryOf(::SummaryViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: SummaryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `initial state should have default values`() =
        runTest {
            viewModel = get()

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.status).isNull()
                assertThat(state.showSetupLeaveConfirmationDialog).isFalse()
                assertThat(state.showHelpMenu).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `initialize with success status should update state`() =
        runTest {
            viewModel = get()
            val successStatus = Success(USER_ID)

            viewModel.onIntent(Initialize(successStatus))

            viewModel.viewState.test {
                assertThat(awaitItem().status).isEqualTo(successStatus)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `initialize with failure status should update state`() =
        runTest {
            viewModel = get()
            val failureStatus = Failure("Error message")

            viewModel.onIntent(Initialize(failureStatus))

            viewModel.viewState.test {
                assertThat(awaitItem().status).isEqualTo(failureStatus)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `go back with success status should show confirmation dialog`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(Success(USER_ID)))

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(GoBack)
                assertThat(awaitItem().showSetupLeaveConfirmationDialog).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `go back with failure status should navigate to welcome`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(Failure("Error message")))

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)
                assertIs<NavigateToWelcome>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `go back with no status should navigate to welcome`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)
                assertIs<NavigateToWelcome>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `confirm setup leave should hide dialog and navigate to app start`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(Success(USER_ID)))
            viewModel.onIntent(GoBack)

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmSetupLeave)
                assertIs<NavigateToAppStart>(awaitItem())
            }

            viewModel.viewState.test {
                assertThat(awaitItem().showSetupLeaveConfirmationDialog).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `dismiss setup leave should hide confirmation dialog`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(Success(USER_ID)))
            viewModel.onIntent(GoBack)

            viewModel.viewState.test {
                assertThat(awaitItem().showSetupLeaveConfirmationDialog).isTrue()
                viewModel.onIntent(DismissSetupLeave)
                assertThat(awaitItem().showSetupLeaveConfirmationDialog).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `open help menu should show help menu`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(OpenHelpMenu)

            viewModel.viewState.test {
                assertThat(awaitItem().showHelpMenu).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `dismiss help menu should hide help menu`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(OpenHelpMenu)
            viewModel.onIntent(DismissHelpMenu)

            viewModel.viewState.test {
                assertThat(awaitItem().showHelpMenu).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `access logs should emit navigate to logs`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(AccessLogs)

            viewModel.sideEffect.test {
                assertIs<NavigateToLogs>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `primary button action with already linked status should navigate to manage accounts`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(AlreadyLinked()))

            viewModel.sideEffect.test {
                viewModel.onIntent(PrimaryButtonAction)
                assertIs<NavigateToManageAccounts>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `primary button action with success status should navigate to sign in`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(Success(USER_ID)))

            viewModel.sideEffect.test {
                viewModel.onIntent(PrimaryButtonAction)
                val effect = awaitItem()
                assertIs<NavigateToSignIn>(effect)
                assertThat(effect.userId).isEqualTo(USER_ID)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `primary button action with failure status should navigate to welcome`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(Failure("Error message")))

            viewModel.sideEffect.test {
                viewModel.onIntent(PrimaryButtonAction)
                assertIs<NavigateToWelcome>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `primary button action with http not supported status should navigate to welcome`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(HttpNotSupported()))

            viewModel.sideEffect.test {
                viewModel.onIntent(PrimaryButtonAction)
                assertIs<NavigateToWelcome>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `primary button action with no network status should navigate to welcome`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(NoNetwork()))

            viewModel.sideEffect.test {
                viewModel.onIntent(PrimaryButtonAction)
                assertIs<NavigateToWelcome>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `primary button action with no status should not emit side effect`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(PrimaryButtonAction)
                expectNoEvents()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `authentication success with success status should save account and navigate to fingerprint setup`() =
        runTest {
            val saveAccountUseCase: SaveAccountUseCase = get()
            val saveResourcesDatabasePassphraseUseCase: SaveResourcesDatabasePassphraseUseCase = get()
            val uuidProvider: UuidProvider = get()
            whenever(uuidProvider.get()) doReturn DATABASE_PASSPHRASE

            viewModel = get()
            viewModel.onIntent(Initialize(Success(USER_ID)))

            viewModel.sideEffect.test {
                viewModel.onIntent(AuthenticationSuccess)

                testScheduler.advanceUntilIdle()

                verify(saveAccountUseCase).execute(UserIdInput(USER_ID))
                verify(saveResourcesDatabasePassphraseUseCase).execute(
                    SaveResourcesDatabasePassphraseUseCase.Input(DATABASE_PASSPHRASE),
                )
                assertIs<NavigateToFingerprintSetup>(awaitItem())
            }
        }

    private companion object {
        private const val USER_ID = "user_id_123"
        private const val DATABASE_PASSPHRASE = "database_passphrase_uuid"
    }
}
