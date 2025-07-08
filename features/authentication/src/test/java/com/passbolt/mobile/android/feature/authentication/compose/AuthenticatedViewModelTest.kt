package com.passbolt.mobile.android.feature.authentication.compose

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
import com.passbolt.mobile.android.core.authenticationcore.session.GetSessionUseCase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider.DUO
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider.TOTP
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider.YUBIKEY
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Passphrase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Session
import com.passbolt.mobile.android.core.mvp.authentication.MfaProvidersHandler
import com.passbolt.mobile.android.core.navigation.ActivityIntents.AuthConfig.RefreshPassphrase
import com.passbolt.mobile.android.core.navigation.ActivityIntents.AuthConfig.SignIn
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticatedIntent.AuthenticationRefreshed
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticatedIntent.OtherProviderClick
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationSideEffect.ShowAuth
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationSideEffect.ShowDuoDialog
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationSideEffect.ShowMfaAuth
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationSideEffect.ShowTotpDialog
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationSideEffect.ShowUnknownProvider
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationSideEffect.ShowYubikeyDialog
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
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class AuthenticatedViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<GetSessionUseCase>() }
                        single { mock<MfaProvidersHandler>() }
                        factoryOf(::TestAuthenticatedViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: TestAuthenticatedViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val getSessionUseCase: GetSessionUseCase = get()
        whenever(getSessionUseCase.execute(Unit)) doReturn
            GetSessionUseCase.Output(
                accessToken = TEST_ACCESS_TOKEN,
                refreshToken = TEST_REFRESH_TOKEN,
                mfaToken = TEST_MFA_TOKEN,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should emit session side effect when session refresh reason is session`() =
        runTest {
            viewModel = get()

            viewModel.authenticationSideEffect.test {
                viewModel.needSessionRefreshFlow.value = Session

                val sideEffect = expectItem()
                assertThat(sideEffect).isInstanceOf(ShowAuth::class.java)
                val showAuthEffect = sideEffect as ShowAuth
                assertThat(showAuthEffect.type).isEqualTo(SignIn)
            }
        }

    @Test
    fun `should emit passphrase side effect when session refresh reason is passphrase`() =
        runTest {
            viewModel = get()

            viewModel.authenticationSideEffect.test {
                viewModel.needSessionRefreshFlow.value = Passphrase

                val sideEffect = expectItem()
                assertThat(sideEffect).isInstanceOf(ShowAuth::class.java)
                val showAuthEffect = sideEffect as ShowAuth
                assertThat(showAuthEffect.type).isEqualTo(RefreshPassphrase)
            }
        }

    @Test
    fun `should emit MFA side effect when session refresh reason is MFA`() =
        runTest {
            val mfaProvidersHandler: MfaProvidersHandler = get()
            whenever(mfaProvidersHandler.firstMfaProvider()) doReturn TOTP
            whenever(mfaProvidersHandler.hasMultipleProviders()) doReturn false

            viewModel = get()

            viewModel.authenticationSideEffect.test {
                viewModel.needSessionRefreshFlow.value = Mfa(providers = listOf(TOTP))

                val sideEffect = expectItem()
                assertThat(sideEffect).isInstanceOf(ShowMfaAuth::class.java)
                val showMfaEffect = sideEffect as ShowMfaAuth
                assertThat(showMfaEffect.mfaReason).isEqualTo(TOTP)
                assertThat(showMfaEffect.hasMultipleProviders).isFalse()
                assertThat(showMfaEffect.sessionAccessToken).isEqualTo(TEST_ACCESS_TOKEN)
            }
        }

    @Test
    fun `should emit session refreshed flow when authentication refreshed intent is received`() =
        runTest {
            viewModel = get()

            viewModel.sessionRefreshedFlow.test {
                viewModel.onAuthenticationIntent(AuthenticationRefreshed)

                val refreshEvent = expectItem()
                assertThat(refreshEvent).isEqualTo(Unit)
            }
        }

    @Test
    fun `should emit TOTP dialog side effect when other provider click intent with YUBIKEY current provider`() =
        runTest {
            val mfaProvidersHandler: MfaProvidersHandler = get()
            whenever(mfaProvidersHandler.nextMfaProvider(YUBIKEY)) doReturn TOTP
            whenever(mfaProvidersHandler.hasMultipleProviders()) doReturn true

            viewModel = get()

            viewModel.authenticationSideEffect.test {
                viewModel.onAuthenticationIntent(OtherProviderClick(YUBIKEY))

                val sideEffect = expectItem()
                assertThat(sideEffect).isInstanceOf(ShowTotpDialog::class.java)
                val showTotpEffect = sideEffect as ShowTotpDialog
                assertThat(showTotpEffect.hasOtherProviders).isTrue()
                assertThat(showTotpEffect.sessionAccessToken).isEqualTo(TEST_ACCESS_TOKEN)
            }
        }

    @Test
    fun `should emit YUBIKEY dialog side effect when other provider click intent with TOTP current provider`() =
        runTest {
            val mfaProvidersHandler: MfaProvidersHandler = get()
            whenever(mfaProvidersHandler.nextMfaProvider(TOTP)) doReturn YUBIKEY
            whenever(mfaProvidersHandler.hasMultipleProviders()) doReturn true

            viewModel = get()

            viewModel.authenticationSideEffect.test {
                viewModel.onAuthenticationIntent(OtherProviderClick(TOTP))

                val sideEffect = expectItem()
                assertThat(sideEffect).isInstanceOf(ShowYubikeyDialog::class.java)
                val showYubikeyEffect = sideEffect as ShowYubikeyDialog
                assertThat(showYubikeyEffect.hasOtherProviders).isTrue()
                assertThat(showYubikeyEffect.sessionAccessToken).isEqualTo(TEST_ACCESS_TOKEN)
            }
        }

    @Test
    fun `should emit DUO dialog side effect when other provider click intent with TOTP current provider`() =
        runTest {
            val mfaProvidersHandler: MfaProvidersHandler = get()
            whenever(mfaProvidersHandler.nextMfaProvider(TOTP)) doReturn DUO
            whenever(mfaProvidersHandler.hasMultipleProviders()) doReturn true

            viewModel = get()

            viewModel.authenticationSideEffect.test {
                viewModel.onAuthenticationIntent(OtherProviderClick(TOTP))

                val sideEffect = expectItem()
                assertThat(sideEffect).isInstanceOf(ShowDuoDialog::class.java)
                val showDuoEffect = sideEffect as ShowDuoDialog
                assertThat(showDuoEffect.hasOtherProviders).isTrue()
                assertThat(showDuoEffect.sessionAccessToken).isEqualTo(TEST_ACCESS_TOKEN)
            }
        }

    @Test
    fun `should emit unknown provider side effect when other provider click intent with null next provider`() =
        runTest {
            val mfaProvidersHandler: MfaProvidersHandler = get()
            whenever(mfaProvidersHandler.nextMfaProvider(TOTP)) doReturn null
            whenever(mfaProvidersHandler.hasMultipleProviders()) doReturn false

            viewModel = get()

            viewModel.authenticationSideEffect.test {
                viewModel.onAuthenticationIntent(OtherProviderClick(TOTP))

                val sideEffect = expectItem()
                assertThat(sideEffect).isInstanceOf(ShowUnknownProvider::class.java)
                val showUnknownEffect = sideEffect as ShowUnknownProvider
                assertThat(showUnknownEffect.hasOtherProviders).isFalse()
            }
        }

    // Need to create a concrete class since AuthenticatedViewModel is an abstract
    class TestAuthenticatedViewModel : AuthenticatedViewModel<Unit, Unit>(Unit)

    private companion object {
        private const val TEST_ACCESS_TOKEN = "test_access_token"
        private const val TEST_REFRESH_TOKEN = "test_refresh_token"
        private const val TEST_MFA_TOKEN = "test_mfa_token"
    }
}
