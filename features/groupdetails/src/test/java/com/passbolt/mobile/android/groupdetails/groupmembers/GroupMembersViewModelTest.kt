package com.passbolt.mobile.android.groupdetails.groupmembers

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
import com.passbolt.mobile.android.core.commongroups.usecase.db.GetGroupWithUsersUseCase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.groupdetails.groupmembers.GroupMembersIntent.GoBack
import com.passbolt.mobile.android.groupdetails.groupmembers.GroupMembersIntent.GoToMemberDetails
import com.passbolt.mobile.android.groupdetails.groupmembers.GroupMembersIntent.Initialize
import com.passbolt.mobile.android.groupdetails.groupmembers.GroupMembersSideEffect.NavigateToMemberDetails
import com.passbolt.mobile.android.groupdetails.groupmembers.GroupMembersSideEffect.NavigateUp
import com.passbolt.mobile.android.ui.GpgKeyModel
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.GroupWithUsersModel
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
class GroupMembersViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<GetGroupWithUsersUseCase>() }
                        singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                        factoryOf(::GroupMembersViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: GroupMembersViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val getGroupWithUsersUseCase = get<GetGroupWithUsersUseCase>()
        getGroupWithUsersUseCase.stub {
            onBlocking { execute(any()) } doReturn GetGroupWithUsersUseCase.Output(testGroupWithUsers)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `group data should be loaded and displayed when initialized`() =
        runTest {
            viewModel = get()

            viewModel.viewState.test {
                assertThat(expectItem()).isEqualTo(GroupMembersState())

                viewModel.onIntent(Initialize(testGroup.groupId))

                val updatedState = expectItem()
                assertThat(updatedState.groupName).isEqualTo(testGroup.groupName)
                assertThat(updatedState.members).isEqualTo(testUsers)
                assertThat(updatedState.members).hasSize(2)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should handle empty group members list correctly`() =
        runTest {
            val emptyGroupWithUsers =
                GroupWithUsersModel(
                    group = testGroup,
                    users = emptyList(),
                )

            val getGroupWithUsersUseCase = get<GetGroupWithUsersUseCase>()
            getGroupWithUsersUseCase.stub {
                onBlocking { execute(any()) } doReturn GetGroupWithUsersUseCase.Output(emptyGroupWithUsers)
            }

            viewModel = get()

            viewModel.viewState.test {
                assertThat(expectItem()).isEqualTo(GroupMembersState())

                viewModel.onIntent(Initialize(testGroup.groupId))

                val updatedState = expectItem()
                assertThat(updatedState.groupName).isEqualTo(testGroup.groupName)
                assertThat(updatedState.members).isEmpty()
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

    @OptIn(ExperimentalTime::class)
    @Test
    fun `go to member details intent should emit navigate to member details side effect`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                val userId = "test-user-id"
                viewModel.onIntent(GoToMemberDetails(userId))
                assertThat(expectItem()).isEqualTo(NavigateToMemberDetails(userId))
            }
        }

    private companion object {
        private val testGroup =
            GroupModel(
                groupId = UUID.randomUUID().toString(),
                groupName = "Test Development Team",
            )

        private val testUser1 =
            UserModel(
                id = UUID.randomUUID().toString(),
                userName = "john.doe@passbolt.com",
                disabled = false,
                gpgKey =
                    GpgKeyModel(
                        armoredKey = "test-armored-key-1",
                        fingerprint = "ABCD1234EFGH5678IJKL9012MNOP3456QRST7890",
                        bits = 4096,
                        uid = "John Doe <john.doe@passbolt.com>",
                        keyId = "test-key-id-1",
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

        private val testUser2 =
            UserModel(
                id = UUID.randomUUID().toString(),
                userName = "jane.smith@passbolt.com",
                disabled = false,
                gpgKey =
                    GpgKeyModel(
                        armoredKey = "test-armored-key-2",
                        fingerprint = "1234ABCD5678EFGH9012IJKL3456MNOP7890QRST",
                        bits = 4096,
                        uid = "Jane Smith <jane.smith@passbolt.com>",
                        keyId = "test-key-id-2",
                        type = "RSA",
                        keyExpirationDate = ZonedDateTime.now().plusYears(2),
                        keyCreationDate = ZonedDateTime.now().minusYears(1),
                        id = UUID.randomUUID().toString(),
                    ),
                profile =
                    UserProfileModel(
                        username = "jane.smith",
                        firstName = "Jane",
                        lastName = "Smith",
                        avatarUrl = "https://passbolt.com/avatar/jane-smith.jpg",
                    ),
            )

        private val testUsers = listOf(testUser1, testUser2)

        private val testGroupWithUsers =
            GroupWithUsersModel(
                group = testGroup,
                users = testUsers,
            )
    }
}
