package com.passbolt.mobile.android.groupdetails.groupmemberdetails

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
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.users.usecase.db.GetLocalUserUseCase
import com.passbolt.mobile.android.groupdetails.groupmemberdetails.GroupMemberDetailsIntent.GoBack
import com.passbolt.mobile.android.groupdetails.groupmemberdetails.GroupMemberDetailsIntent.Initialize
import com.passbolt.mobile.android.groupdetails.groupmemberdetails.GroupMemberDetailsSideEffect.NavigateUp
import com.passbolt.mobile.android.ui.GpgKeyModel
import com.passbolt.mobile.android.ui.UserModel
import com.passbolt.mobile.android.ui.UserProfileModel
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
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class GroupMemberDetailsViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<GetLocalUserUseCase>() }
                        singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                        factoryOf(::GroupMemberDetailsViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: GroupMemberDetailsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val getLocalUserUseCase = get<GetLocalUserUseCase>()
        getLocalUserUseCase.stub {
            onBlocking { execute(any()) } doReturn GetLocalUserUseCase.Output(testUser)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `user data should be loaded and displayed when initialized`() =
        runTest {
            viewModel = get()

            viewModel.viewState.test {
                val initialState = expectItem()
                assertThat(initialState).isEqualTo(GroupMemberDetailsState())

                viewModel.onIntent(Initialize(testUser.id))

                val updatedState = expectItem()
                assertThat(updatedState.userName).isEqualTo(testUser.userName)
                assertThat(updatedState.firstName).isEqualTo(testUser.profile.firstName)
                assertThat(updatedState.lastName).isEqualTo(testUser.profile.lastName)
                assertThat(updatedState.avatarUrl).isEqualTo(testUser.profile.avatarUrl)
                assertThat(updatedState.fingerprint).isEqualTo(testUser.gpgKey.fingerprint)
                assertThat(updatedState.fullName).isEqualTo("John Doe")
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should handle empty optional fields correctly`() =
        runTest {
            val userWithEmptyFields =
                testUser.copy(
                    profile =
                        testUser.profile.copy(
                            firstName = null,
                            lastName = null,
                            avatarUrl = null,
                        ),
                )

            val getLocalUserUseCase = get<GetLocalUserUseCase>()
            getLocalUserUseCase.stub {
                onBlocking { execute(any()) } doReturn GetLocalUserUseCase.Output(userWithEmptyFields)
            }

            viewModel = get()

            viewModel.viewState.test {
                assertThat(expectItem()).isEqualTo(GroupMemberDetailsState())

                viewModel.onIntent(Initialize(testUser.id))

                val updatedState = expectItem()
                assertThat(updatedState.userName).isEqualTo(testUser.userName)
                assertThat(updatedState.firstName).isEqualTo("")
                assertThat(updatedState.lastName).isEqualTo("")
                assertThat(updatedState.avatarUrl).isNull()
                assertThat(updatedState.fingerprint).isEqualTo(testUser.gpgKey.fingerprint)
                assertThat(updatedState.fullName).isEqualTo("")
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `go back intent should emit navigate up side effect`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)
                assertThat(expectItem()).isEqualTo(NavigateUp)
            }
        }

    private companion object {
        private val testUser =
            UserModel(
                id = UUID.randomUUID().toString(),
                userName = "john.doe@passbolt.com",
                disabled = false,
                gpgKey =
                    GpgKeyModel(
                        armoredKey = "test-armored-key",
                        fingerprint = "ABCD1234EFGH5678IJKL9012MNOP3456QRST7890",
                        bits = 4096,
                        uid = "John Doe <john.doe@passbolt.com>",
                        keyId = "test-key-id",
                        type = "RSA",
                        keyExpirationDate = ZonedDateTime.now().plusYears(1),
                        keyCreationDate = ZonedDateTime.now().minusYears(1),
                        id = UUID.randomUUID().toString(),
                    ),
                profile =
                    UserProfileModel(
                        username = "john.doe",
                        firstName = "John",
                        lastName = "Doe",
                        avatarUrl = "https://passbolt.com/avatar/john-doe.jpg",
                    ),
            )
    }
}
