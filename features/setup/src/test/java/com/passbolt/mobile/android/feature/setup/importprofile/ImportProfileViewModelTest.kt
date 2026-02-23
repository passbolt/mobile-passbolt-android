package com.passbolt.mobile.android.feature.setup.importprofile

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
import com.passbolt.mobile.android.core.accounts.AccountsInteractor
import com.passbolt.mobile.android.core.accounts.AccountsInteractor.InjectAccountFailureType.ACCOUNT_ALREADY_LINKED
import com.passbolt.mobile.android.core.accounts.AccountsInteractor.InjectAccountFailureType.ERROR_NON_HTTPS_DOMAIN
import com.passbolt.mobile.android.core.accounts.AccountsInteractor.InjectAccountFailureType.ERROR_WHEN_SAVING_PRIVATE_KEY
import com.passbolt.mobile.android.core.navigation.AccountSetupDataModel
import com.passbolt.mobile.android.feature.setup.importprofile.ImportProfileIntent.ChangeAccountUrl
import com.passbolt.mobile.android.feature.setup.importprofile.ImportProfileIntent.ChangePrivateKey
import com.passbolt.mobile.android.feature.setup.importprofile.ImportProfileIntent.ChangeUserId
import com.passbolt.mobile.android.feature.setup.importprofile.ImportProfileIntent.GoBack
import com.passbolt.mobile.android.feature.setup.importprofile.ImportProfileIntent.Import
import com.passbolt.mobile.android.feature.setup.importprofile.ImportProfileSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.setup.importprofile.ImportProfileSideEffect.NavigateToSummary
import com.passbolt.mobile.android.ui.ResultStatus.AlreadyLinked
import com.passbolt.mobile.android.ui.ResultStatus.Failure
import com.passbolt.mobile.android.ui.ResultStatus.HttpNotSupported
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class ImportProfileViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<AccountsInteractor>() }
                        factoryOf(::ImportProfileViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: ImportProfileViewModel

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
                assertThat(state.userId).isEmpty()
                assertThat(state.accountUrl).isEmpty()
                assertThat(state.privateKey).isEmpty()
                assertThat(state.hasUserIdValidationError).isFalse()
                assertThat(state.hasAccountUrlValidationError).isFalse()
                assertThat(state.hasPrivateKeyValidationError).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `go back intent should emit navigate back side effect`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)
                assertIs<NavigateBack>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `change user id should update state and trim whitespace`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(ChangeUserId("  test-user-id  "))

            viewModel.viewState.test {
                assertThat(awaitItem().userId).isEqualTo("test-user-id")
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `change account url should update state and trim whitespace`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(ChangeAccountUrl("  https://passbolt.com  "))

            viewModel.viewState.test {
                assertThat(awaitItem().accountUrl).isEqualTo("https://passbolt.com")
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `change private key should update state and trim whitespace`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(ChangePrivateKey("  private-key-data  "))

            viewModel.viewState.test {
                assertThat(awaitItem().privateKey).isEqualTo("private-key-data")
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `import with blank user id should show validation error`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(ChangeAccountUrl("https://passbolt.com"))
            viewModel.onIntent(ChangePrivateKey("private-key"))

            viewModel.viewState.test {
                viewModel.onIntent(Import)
                awaitItem() // clear validation
                assertThat(awaitItem().hasUserIdValidationError).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `import with invalid user id uuid should show validation error`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(ChangeUserId("not-a-uuid"))
            viewModel.onIntent(ChangeAccountUrl("https://passbolt.com"))
            viewModel.onIntent(ChangePrivateKey("private-key"))

            viewModel.viewState.test {
                viewModel.onIntent(Import)
                awaitItem() // clear validation
                assertThat(awaitItem().hasUserIdValidationError).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `import with blank account url should show validation error`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(ChangeUserId(VALID_UUID))
            viewModel.onIntent(ChangePrivateKey("private-key"))

            viewModel.viewState.test {
                viewModel.onIntent(Import)
                awaitItem() // clear validation
                assertThat(awaitItem().hasAccountUrlValidationError).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `import with non https url should show validation error`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(ChangeUserId(VALID_UUID))
            viewModel.onIntent(ChangeAccountUrl("http://passbolt.com"))
            viewModel.onIntent(ChangePrivateKey("private-key"))

            viewModel.viewState.test {
                viewModel.onIntent(Import)
                awaitItem() // clear validation
                assertThat(awaitItem().hasAccountUrlValidationError).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `import with blank private key should show validation error`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(ChangeUserId(VALID_UUID))
            viewModel.onIntent(ChangeAccountUrl("https://passbolt.com"))

            viewModel.viewState.test {
                viewModel.onIntent(Import)
                awaitItem() // clear validation
                assertThat(awaitItem().hasPrivateKeyValidationError).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `import with multiple validation errors should show all errors`() =
        runTest {
            viewModel = get()

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(Import)
                awaitItem() // clear validation

                skipItems(1) // fields are validated one by one with state updates
                val state = awaitItem()
                assertThat(state.hasUserIdValidationError).isTrue()
                assertThat(state.hasAccountUrlValidationError).isTrue()
                assertThat(state.hasPrivateKeyValidationError).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `import with valid data should inject account and navigate to summary with success`() =
        runTest {
            val accountsInteractor: AccountsInteractor = get()

            val onSuccessCaptor = argumentCaptor<(String) -> Unit>()
            val onFailureCaptor = argumentCaptor<(AccountsInteractor.InjectAccountFailureType) -> Unit>()

            viewModel = get()
            viewModel.onIntent(ChangeUserId(VALID_UUID))
            viewModel.onIntent(ChangeAccountUrl(VALID_URL))
            viewModel.onIntent(ChangePrivateKey(VALID_PRIVATE_KEY))

            viewModel.sideEffect.test {
                viewModel.onIntent(Import)

                testScheduler.advanceUntilIdle()

                verify(accountsInteractor).injectPredefinedAccountData(
                    eq(
                        AccountSetupDataModel(
                            serverUserId = VALID_UUID,
                            domain = VALID_URL,
                            armoredKey = VALID_PRIVATE_KEY,
                            firstName = "",
                            lastName = "",
                            avatarUrl = "",
                            userName = "",
                            keyFingerprint = "",
                        ),
                    ),
                    onSuccess = onSuccessCaptor.capture(),
                    onFailure = onFailureCaptor.capture(),
                )

                onSuccessCaptor.firstValue.invoke(USER_ID)

                val effect = awaitItem()
                assertIs<NavigateToSummary>(effect)
                assertIs<Success>(effect.status)
                assertThat(effect.status.userId).isEqualTo(USER_ID)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `import with account already linked failure should navigate to summary with already linked status`() =
        runTest {
            val accountsInteractor: AccountsInteractor = get()

            val onSuccessCaptor = argumentCaptor<(String) -> Unit>()
            val onFailureCaptor = argumentCaptor<(AccountsInteractor.InjectAccountFailureType) -> Unit>()

            viewModel = get()
            viewModel.onIntent(ChangeUserId(VALID_UUID))
            viewModel.onIntent(ChangeAccountUrl(VALID_URL))
            viewModel.onIntent(ChangePrivateKey(VALID_PRIVATE_KEY))

            viewModel.sideEffect.test {
                viewModel.onIntent(Import)

                testScheduler.advanceUntilIdle()

                verify(accountsInteractor).injectPredefinedAccountData(
                    eq(
                        AccountSetupDataModel(
                            serverUserId = VALID_UUID,
                            domain = VALID_URL,
                            armoredKey = VALID_PRIVATE_KEY,
                            firstName = "",
                            lastName = "",
                            avatarUrl = "",
                            userName = "",
                            keyFingerprint = "",
                        ),
                    ),
                    onSuccess = onSuccessCaptor.capture(),
                    onFailure = onFailureCaptor.capture(),
                )

                onFailureCaptor.firstValue.invoke(ACCOUNT_ALREADY_LINKED)

                val effect = awaitItem()
                assertIs<NavigateToSummary>(effect)
                assertIs<AlreadyLinked>(effect.status)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `import with non https domain failure should navigate to summary with http not supported status`() =
        runTest {
            val accountsInteractor: AccountsInteractor = get()

            val onSuccessCaptor = argumentCaptor<(String) -> Unit>()
            val onFailureCaptor = argumentCaptor<(AccountsInteractor.InjectAccountFailureType) -> Unit>()

            viewModel = get()
            viewModel.onIntent(ChangeUserId(VALID_UUID))
            viewModel.onIntent(ChangeAccountUrl(VALID_URL))
            viewModel.onIntent(ChangePrivateKey(VALID_PRIVATE_KEY))

            viewModel.sideEffect.test {
                viewModel.onIntent(Import)

                testDispatcher.scheduler.advanceUntilIdle()

                verify(accountsInteractor).injectPredefinedAccountData(
                    eq(
                        AccountSetupDataModel(
                            serverUserId = VALID_UUID,
                            domain = VALID_URL,
                            armoredKey = VALID_PRIVATE_KEY,
                            firstName = "",
                            lastName = "",
                            avatarUrl = "",
                            userName = "",
                            keyFingerprint = "",
                        ),
                    ),
                    onSuccess = onSuccessCaptor.capture(),
                    onFailure = onFailureCaptor.capture(),
                )

                onFailureCaptor.firstValue.invoke(ERROR_NON_HTTPS_DOMAIN)

                val effect = awaitItem()
                assertIs<NavigateToSummary>(effect)
                assertIs<HttpNotSupported>(effect.status)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `import with error when saving private key failure should navigate to summary with failure status`() =
        runTest {
            val accountsInteractor: AccountsInteractor = get()

            val onSuccessCaptor = argumentCaptor<(String) -> Unit>()
            val onFailureCaptor = argumentCaptor<(AccountsInteractor.InjectAccountFailureType) -> Unit>()

            viewModel = get()
            viewModel.onIntent(ChangeUserId(VALID_UUID))
            viewModel.onIntent(ChangeAccountUrl(VALID_URL))
            viewModel.onIntent(ChangePrivateKey(VALID_PRIVATE_KEY))

            viewModel.sideEffect.test {
                viewModel.onIntent(Import)

                testDispatcher.scheduler.advanceUntilIdle()

                verify(accountsInteractor).injectPredefinedAccountData(
                    eq(
                        AccountSetupDataModel(
                            serverUserId = VALID_UUID,
                            domain = VALID_URL,
                            armoredKey = VALID_PRIVATE_KEY,
                            firstName = "",
                            lastName = "",
                            avatarUrl = "",
                            userName = "",
                            keyFingerprint = "",
                        ),
                    ),
                    onSuccess = onSuccessCaptor.capture(),
                    onFailure = onFailureCaptor.capture(),
                )

                onFailureCaptor.firstValue.invoke(ERROR_WHEN_SAVING_PRIVATE_KEY)

                val effect = awaitItem()
                assertIs<NavigateToSummary>(effect)
                assertIs<Failure>(effect.status)
            }
        }

    private companion object {
        private const val VALID_UUID = "123e4567-e89b-12d3-a456-426614174000"
        private const val VALID_URL = "https://passbolt.com"
        private const val VALID_PRIVATE_KEY = "-----BEGIN PGP PRIVATE KEY BLOCK-----"
        private const val USER_ID = "user_id_123"
    }
}
