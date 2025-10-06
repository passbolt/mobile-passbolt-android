package com.passbolt.mobile.android.feature.settings.accounts.keyinspector

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
import com.passbolt.mobile.android.core.mvp.authentication.SessionRefreshTrackingFlow
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.ui.formatter.DateFormatter
import com.passbolt.mobile.android.core.ui.formatter.FingerprintFormatter
import com.passbolt.mobile.android.core.users.user.FetchCurrentUserUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetSessionExpiryUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetSessionExpiryUseCase.Output.JwtWillExpire
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorIntent.CopyFingerprint
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorIntent.CopyUid
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorScreenSideEffect.AddFingerprintToClipboard
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorScreenSideEffect.AddUidToClipboard
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorScreenSideEffect.ErrorSnackbarType.FAILED_TO_FETCH_KEY
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorScreenSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorViewModel
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
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import java.net.UnknownHostException
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class KeyInspectorViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<FetchCurrentUserUseCase>() }
                        single { mock<GetSelectedAccountDataUseCase>() }
                        single { mock<DateFormatter>() }
                        single { mock<FingerprintFormatter>() }
                        single { mock<GetSessionExpiryUseCase>() }
                        single { mock<PassphraseMemoryCache>() }
                        singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                        factoryOf(::KeyInspectorViewModel)
                        singleOf(::SessionRefreshTrackingFlow)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: KeyInspectorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val passphraseMemoryCache: PassphraseMemoryCache = get()
        whenever(passphraseMemoryCache.getSessionDurationSeconds()) doReturn 5 * 60

        val getSessionExpiryUseCase: GetSessionExpiryUseCase = get()
        whenever(getSessionExpiryUseCase.execute(Unit)) doReturn JwtWillExpire(ZonedDateTime.now().plusMinutes(5))

        val getSelectedAccountDataUseCase = get<GetSelectedAccountDataUseCase>()
        whenever(getSelectedAccountDataUseCase.execute(Unit)) doReturn selectedAccountData

        val fetchCurrentUserUseCase = get<FetchCurrentUserUseCase>()
        fetchCurrentUserUseCase.stub {
            onBlocking { execute(Unit) } doReturn user
        }

        val fingerprintFormatter: FingerprintFormatter = get()
        whenever(fingerprintFormatter.format(any(), any())) doAnswer { it.getArgument(0) }

        val dateFormatter: DateFormatter = get()
        whenever(dateFormatter.format(any())) doAnswer { it.getArgument<ZonedDateTime>(0).toString() }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `key data and account data should be shown initially`() =
        runTest {
            viewModel = get()

            viewModel.viewState.test {
                val state = expectItem()
                assertThat(state.avatarUrl).isEqualTo(selectedAccountData.avatarUrl)
                assertThat(state.label).isEqualTo(selectedAccountData.label)
                assertThat(state.fingerprint).isEqualTo(user.userModel.gpgKey.fingerprint)
                assertThat(state.keyLength).isEqualTo(user.userModel.gpgKey.bits)
                assertThat(state.uid).isEqualTo(user.userModel.gpgKey.uid)
                assertThat(state.created).isEqualTo(
                    user.userModel.gpgKey.keyCreationDate
                        .toString(),
                )
                assertThat(state.expires).isEqualTo(
                    user.userModel.gpgKey.keyExpirationDate
                        .toString(),
                )
                assertThat(state.algorithm).isEqualTo(user.userModel.gpgKey.type)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `error should be shown when key data fails to fetch`() =
        runTest {
            val errorMessage = "errorMessage"
            val fetchCurrentUserUseCase: FetchCurrentUserUseCase = get()
            fetchCurrentUserUseCase.stub {
                onBlocking { execute(Unit) }.thenReturn(
                    FetchCurrentUserUseCase.Output.Failure(
                        NetworkResult.Failure.NetworkError(
                            UnknownHostException(),
                            errorMessage,
                        ),
                        errorMessage,
                    ),
                )
            }

            viewModel = get()

            viewModel.sideEffect.test {
                val effect = expectItem()
                assertThat(effect).isInstanceOf(ShowErrorSnackbar::class.java)
                assertThat((effect as ShowErrorSnackbar).type).isEqualTo(FAILED_TO_FETCH_KEY)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `copy actions should copy correct data`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(CopyUid)
                val copyUidEffect = expectItem()
                assertThat(copyUidEffect).isInstanceOf(AddUidToClipboard::class.java)
                assertThat((copyUidEffect as AddUidToClipboard).uid)
                    .isEqualTo(user.userModel.gpgKey.uid)

                viewModel.onIntent(CopyFingerprint)
                val copyFingerprintEffect = expectItem()
                assertThat(copyFingerprintEffect).isInstanceOf(AddFingerprintToClipboard::class.java)
                assertThat((copyFingerprintEffect as AddFingerprintToClipboard).fingerprint)
                    .isEqualTo(user.userModel.gpgKey.fingerprint)
            }
        }

    private companion object {
        private val selectedAccountData =
            GetSelectedAccountDataUseCase.Output(
                firstName = "John",
                lastName = "Doe",
                avatarUrl = "https://passbolt.com/avatar.jpg",
                label = "John Doe",
                email = "john.doe@passbolt.com",
                url = "https://passbolt.com",
                serverId = "123e4567-e89b-12d3-a456-426614174000",
                role = "admin",
            )

        private val user =
            FetchCurrentUserUseCase.Output.Success(
                UserModel(
                    id = "newUserId",
                    userName = "newUserName",
                    disabled = false,
                    gpgKey =
                        GpgKeyModel(
                            armoredKey = "keyData",
                            fingerprint = "fingerprint",
                            bits = 1,
                            uid = "uid",
                            keyId = "keyid",
                            type = "rsa",
                            keyExpirationDate = ZonedDateTime.now(),
                            keyCreationDate = ZonedDateTime.now(),
                            id = UUID.randomUUID().toString(),
                        ),
                    profile =
                        UserProfileModel(
                            username = "username",
                            firstName = "first",
                            lastName = "last",
                            avatarUrl = "avatar_url",
                        ),
                ),
            )
    }
}
