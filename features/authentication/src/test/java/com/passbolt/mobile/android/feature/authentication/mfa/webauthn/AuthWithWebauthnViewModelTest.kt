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

package com.passbolt.mobile.android.feature.authentication.mfa.webauthn

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.domain.auth.usecase.GetSessionUseCase
import com.passbolt.mobile.android.domain.mfa.usecase.GetPasskeyCeremonyUrlUseCase
import com.passbolt.mobile.android.domain.mfa.usecase.GetWebauthnDelegatedPromptUseCase
import com.passbolt.mobile.android.domain.mfa.usecase.VerifyWebauthnExchangeCodeUseCase
import com.passbolt.mobile.android.feature.authentication.auth.OneTimeSessionRefreshRetry
import com.passbolt.mobile.android.feature.authentication.auth.usecase.RefreshSessionUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.AuthenticateWithWebauthn
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.ChooseOtherProvider
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.Close
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.ConfirmSetupLeave
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.DismissSetupLeave
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.ToggleRememberMe
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnIntent.WebauthnCeremonyFinished
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.CloseAndNavigateToStartup
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.LaunchWebauthnCeremony
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.NavigateToLogin
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.NotifyOtherProviderClicked
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.NotifyVerificationSucceeded
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.SnackbarErrorType.BROWSER_UNAVAILABLE
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.SnackbarErrorType.CEREMONY_FAILED
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.SnackbarErrorType.GENERIC
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.SnackbarErrorType.NETWORK
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.SnackbarErrorType.PROVIDER_UNAVAILABLE
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.SnackbarErrorType.SESSION_EXPIRED
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.AuthWithWebauthnSideEffect.SnackbarErrorType.VERIFICATION_EXPIRED
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.BrowserUnavailable
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.Cancelled
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.Completed
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.Failed
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
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class AuthWithWebauthnViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<GetWebauthnDelegatedPromptUseCase>() }
                    single { mock<VerifyWebauthnExchangeCodeUseCase>() }
                    single { mock<GetPasskeyCeremonyUrlUseCase>() }
                    single { mock<GetSessionUseCase>() }
                    single { mock<RefreshSessionUseCase>() }
                    single { mock<SignOutUseCase>() }
                    single { OneTimeSessionRefreshRetry(refreshSessionUseCase = get(), getSessionUseCase = get()) }
                    factory { params ->
                        AuthWithWebauthnViewModel(
                            authToken = params[0],
                            hasOtherProvider = params[1],
                            isSetupFlow = params[2],
                            getWebauthnDelegatedPromptUseCase = get(),
                            verifyWebauthnExchangeCodeUseCase = get(),
                            getPasskeyCeremonyUrlUseCase = get(),
                            oneTimeSessionRefreshRetry = get(),
                            signOutUseCase = get(),
                        )
                    }
                },
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: AuthWithWebauthnViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        get<GetPasskeyCeremonyUrlUseCase>().stub {
            on { execute(GetPasskeyCeremonyUrlUseCase.Input(CEREMONY_TOKEN)) } doReturn
                GetPasskeyCeremonyUrlUseCase.Output(CEREMONY_URL)
        }
        get<GetSessionUseCase>().stub {
            on { execute(Unit) } doReturn GetSessionUseCase.Output(REFRESHED_AUTH_TOKEN, null, null)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `authenticate builds ceremony url from api url and ceremony token`() =
        runTest {
            get<GetWebauthnDelegatedPromptUseCase>().stub {
                on { execute(any()) } doReturn GetWebauthnDelegatedPromptUseCase.Output.Success(CEREMONY_TOKEN)
            }
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(AuthenticateWithWebauthn)

                val launch = awaitItem()
                assertIs<LaunchWebauthnCeremony>(launch)
                assertThat(launch.ceremonyUrl).isEqualTo(CEREMONY_URL)
            }

            verify(get<GetWebauthnDelegatedPromptUseCase>()).execute(GetWebauthnDelegatedPromptUseCase.Input(AUTH_TOKEN))
        }

    @Test
    fun `authenticate unauthorized refreshes session and retries with refreshed token`() =
        runTest {
            get<GetWebauthnDelegatedPromptUseCase>().stub {
                on { execute(any()) } doReturn
                    GetWebauthnDelegatedPromptUseCase.Output.Unauthorized doReturn
                    GetWebauthnDelegatedPromptUseCase.Output.Success(CEREMONY_TOKEN)
            }
            get<RefreshSessionUseCase>().stub {
                on { execute(Unit) } doReturn RefreshSessionUseCase.Output.Success
            }
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(AuthenticateWithWebauthn)

                assertIs<LaunchWebauthnCeremony>(awaitItem())
            }

            verify(get<RefreshSessionUseCase>()).execute(Unit)
            verify(get<GetWebauthnDelegatedPromptUseCase>()).execute(GetWebauthnDelegatedPromptUseCase.Input(AUTH_TOKEN))
            verify(get<GetWebauthnDelegatedPromptUseCase>()).execute(
                GetWebauthnDelegatedPromptUseCase.Input(REFRESHED_AUTH_TOKEN),
            )
        }

    @Test
    fun `authenticate unauthorized after successful refresh retries only once`() =
        runTest {
            get<GetWebauthnDelegatedPromptUseCase>().stub {
                on { execute(any()) } doReturn GetWebauthnDelegatedPromptUseCase.Output.Unauthorized
            }
            get<RefreshSessionUseCase>().stub {
                on { execute(Unit) } doReturn RefreshSessionUseCase.Output.Success
            }
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(AuthenticateWithWebauthn)

                val snackbar = awaitItem()
                assertIs<ShowErrorSnackbar>(snackbar)
                assertThat(snackbar.kind).isEqualTo(SESSION_EXPIRED)
                assertIs<NavigateToLogin>(awaitItem())
            }

            verify(get<RefreshSessionUseCase>(), times(1)).execute(Unit)
            verify(get<GetWebauthnDelegatedPromptUseCase>(), times(2)).execute(any())
        }

    @Test
    fun `authenticate unauthorized with failed refresh navigates to login`() =
        runTest {
            get<GetWebauthnDelegatedPromptUseCase>().stub {
                on { execute(any()) } doReturn GetWebauthnDelegatedPromptUseCase.Output.Unauthorized
            }
            get<RefreshSessionUseCase>().stub {
                on { execute(Unit) } doReturn RefreshSessionUseCase.Output.Failure
            }
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(AuthenticateWithWebauthn)

                val snackbar = awaitItem()
                assertIs<ShowErrorSnackbar>(snackbar)
                assertThat(snackbar.kind).isEqualTo(SESSION_EXPIRED)
                assertIs<NavigateToLogin>(awaitItem())
            }
        }

    @Test
    fun `authenticate bad request shows provider unavailable error`() =
        runTest {
            get<GetWebauthnDelegatedPromptUseCase>().stub {
                on { execute(any()) } doReturn GetWebauthnDelegatedPromptUseCase.Output.NetworkFailure(400)
            }
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(AuthenticateWithWebauthn)

                val snackbar = awaitItem()
                assertIs<ShowErrorSnackbar>(snackbar)
                assertThat(snackbar.kind).isEqualTo(PROVIDER_UNAVAILABLE)
            }
        }

    @Test
    fun `authenticate server error shows generic error`() =
        runTest {
            get<GetWebauthnDelegatedPromptUseCase>().stub {
                on { execute(any()) } doReturn GetWebauthnDelegatedPromptUseCase.Output.NetworkFailure(500)
            }
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(AuthenticateWithWebauthn)

                val snackbar = awaitItem()
                assertIs<ShowErrorSnackbar>(snackbar)
                assertThat(snackbar.kind).isEqualTo(GENERIC)
            }
        }

    @Test
    fun `authenticate offline shows network error`() =
        runTest {
            get<GetWebauthnDelegatedPromptUseCase>().stub {
                on { execute(any()) } doReturn
                    GetWebauthnDelegatedPromptUseCase.Output.Failure(
                        DomainResult.Incomplete.Error(DomainResult.Incomplete.Error.Reason.OFFLINE, null),
                    )
            }
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(AuthenticateWithWebauthn)

                val snackbar = awaitItem()
                assertIs<ShowErrorSnackbar>(snackbar)
                assertThat(snackbar.kind).isEqualTo(NETWORK)
            }
        }

    @Test
    fun `authenticate with null auth token shows generic error and does not call api`() =
        runTest {
            viewModel = get(parameters = { parametersOf(null, false, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(AuthenticateWithWebauthn)

                val snackbar = awaitItem()
                assertIs<ShowErrorSnackbar>(snackbar)
                assertThat(snackbar.kind).isEqualTo(GENERIC)
            }

            verifyNoInteractions(get<GetWebauthnDelegatedPromptUseCase>())
        }

    @Test
    fun `browser unavailable shows dedicated error`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(WebauthnCeremonyFinished(BrowserUnavailable))

                val snackbar = awaitItem()
                assertIs<ShowErrorSnackbar>(snackbar)
                assertThat(snackbar.kind).isEqualTo(BROWSER_UNAVAILABLE)
            }
        }

    @Test
    fun `completed ceremony exchanges code with remember flag and notifies success`() =
        runTest {
            get<VerifyWebauthnExchangeCodeUseCase>().stub {
                on { execute(any()) } doReturn VerifyWebauthnExchangeCodeUseCase.Output.Success(MFA_HEADER)
            }
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })
            viewModel.onIntent(ToggleRememberMe(false))

            viewModel.sideEffect.test {
                viewModel.onIntent(WebauthnCeremonyFinished(Completed(EXCHANGE_CODE)))

                val success = awaitItem()
                assertIs<NotifyVerificationSucceeded>(success)
                assertThat(success.mfaHeader).isEqualTo(MFA_HEADER)
            }

            verify(get<VerifyWebauthnExchangeCodeUseCase>()).execute(
                VerifyWebauthnExchangeCodeUseCase.Input(
                    jwtHeader = AUTH_TOKEN,
                    exchangeCode = EXCHANGE_CODE,
                    remember = false,
                ),
            )
        }

    @Test
    fun `completed ceremony without mfa cookie shows generic error`() =
        runTest {
            get<VerifyWebauthnExchangeCodeUseCase>().stub {
                on { execute(any()) } doReturn VerifyWebauthnExchangeCodeUseCase.Output.Success(null)
            }
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(WebauthnCeremonyFinished(Completed(EXCHANGE_CODE)))

                val snackbar = awaitItem()
                assertIs<ShowErrorSnackbar>(snackbar)
                assertThat(snackbar.kind).isEqualTo(GENERIC)
            }
        }

    @Test
    fun `completed ceremony unauthorized refreshes session and retries with refreshed token`() =
        runTest {
            get<VerifyWebauthnExchangeCodeUseCase>().stub {
                on { execute(any()) } doReturn
                    VerifyWebauthnExchangeCodeUseCase.Output.Unauthorized doReturn
                    VerifyWebauthnExchangeCodeUseCase.Output.Success(MFA_HEADER)
            }
            get<RefreshSessionUseCase>().stub {
                on { execute(Unit) } doReturn RefreshSessionUseCase.Output.Success
            }
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(WebauthnCeremonyFinished(Completed(EXCHANGE_CODE)))

                assertIs<NotifyVerificationSucceeded>(awaitItem())
            }

            verify(get<RefreshSessionUseCase>()).execute(Unit)
            verify(get<VerifyWebauthnExchangeCodeUseCase>()).execute(
                VerifyWebauthnExchangeCodeUseCase.Input(REFRESHED_AUTH_TOKEN, EXCHANGE_CODE, remember = true),
            )
        }

    @Test
    fun `completed ceremony unauthorized with failed refresh navigates to login`() =
        runTest {
            get<VerifyWebauthnExchangeCodeUseCase>().stub {
                on { execute(any()) } doReturn VerifyWebauthnExchangeCodeUseCase.Output.Unauthorized
            }
            get<RefreshSessionUseCase>().stub {
                on { execute(Unit) } doReturn RefreshSessionUseCase.Output.Failure
            }
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(WebauthnCeremonyFinished(Completed(EXCHANGE_CODE)))

                val snackbar = awaitItem()
                assertIs<ShowErrorSnackbar>(snackbar)
                assertThat(snackbar.kind).isEqualTo(SESSION_EXPIRED)
                assertIs<NavigateToLogin>(awaitItem())
            }
        }

    @Test
    fun `completed ceremony bad request shows verification expired error`() =
        runTest {
            get<VerifyWebauthnExchangeCodeUseCase>().stub {
                on { execute(any()) } doReturn VerifyWebauthnExchangeCodeUseCase.Output.NetworkFailure(400)
            }
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(WebauthnCeremonyFinished(Completed(EXCHANGE_CODE)))

                val snackbar = awaitItem()
                assertIs<ShowErrorSnackbar>(snackbar)
                assertThat(snackbar.kind).isEqualTo(VERIFICATION_EXPIRED)
            }
        }

    @Test
    fun `completed ceremony timeout shows network error`() =
        runTest {
            get<VerifyWebauthnExchangeCodeUseCase>().stub {
                on { execute(any()) } doReturn
                    VerifyWebauthnExchangeCodeUseCase.Output.Failure(
                        DomainResult.Incomplete.Error(DomainResult.Incomplete.Error.Reason.TIMEOUT, null),
                    )
            }
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(WebauthnCeremonyFinished(Completed(EXCHANGE_CODE)))

                val snackbar = awaitItem()
                assertIs<ShowErrorSnackbar>(snackbar)
                assertThat(snackbar.kind).isEqualTo(NETWORK)
            }
        }

    @Test
    fun `cancelled ceremony does nothing`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.onIntent(WebauthnCeremonyFinished(Cancelled))

            verifyNoInteractions(get<VerifyWebauthnExchangeCodeUseCase>())
            assertThat(viewModel.viewState.value.showProgress).isFalse()
        }

    @Test
    fun `failed ceremony shows ceremony failed error and does not exchange`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(WebauthnCeremonyFinished(Failed("ceremony_failed")))

                val snackbar = awaitItem()
                assertIs<ShowErrorSnackbar>(snackbar)
                assertThat(snackbar.kind).isEqualTo(CEREMONY_FAILED)
            }

            verifyNoInteractions(get<VerifyWebauthnExchangeCodeUseCase>())
        }

    @Test
    fun `toggle remember me updates state`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.onIntent(ToggleRememberMe(false))

            assertThat(viewModel.viewState.value.rememberMe).isFalse()
        }

    @Test
    fun `choose other provider emits bearer`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, true, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(ChooseOtherProvider)

                val effect = awaitItem()
                assertIs<NotifyOtherProviderClicked>(effect)
                assertThat(effect.bearer).isEqualTo(AUTH_TOKEN)
            }
        }

    @Test
    fun `close in setup flow shows leave confirmation and does not sign out`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, true) })

            viewModel.onIntent(Close)

            assertThat(viewModel.viewState.value.showSetupLeaveConfirmationDialog).isTrue()
            verifyNoInteractions(get<SignOutUseCase>())
        }

    @Test
    fun `confirm setup leave signs out and navigates to startup`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, true) })
            viewModel.onIntent(Close)

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmSetupLeave)
                assertIs<CloseAndNavigateToStartup>(awaitItem())
            }

            assertThat(viewModel.viewState.value.showSetupLeaveConfirmationDialog).isFalse()
            verify(get<SignOutUseCase>()).execute(Unit)
        }

    @Test
    fun `dismiss setup leave hides confirmation and does not sign out`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, true) })
            viewModel.onIntent(Close)

            viewModel.onIntent(DismissSetupLeave)

            assertThat(viewModel.viewState.value.showSetupLeaveConfirmationDialog).isFalse()
            verifyNoInteractions(get<SignOutUseCase>())
        }

    @Test
    fun `close outside setup flow signs out and navigates to startup`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(Close)
                assertIs<CloseAndNavigateToStartup>(awaitItem())
            }

            verify(get<SignOutUseCase>()).execute(Unit)
        }

    private companion object {
        const val AUTH_TOKEN = "test-auth-token"
        const val REFRESHED_AUTH_TOKEN = "refreshed-auth-token"
        const val CEREMONY_URL = "https://passbolt.example.com/mfa/verify/webauthn/delegated?ceremony_token=x"
        const val CEREMONY_TOKEN = "ceremony-token-123"
        const val EXCHANGE_CODE = "exchange-code-456"
        const val MFA_HEADER = "passbolt_mfa=jwt"
    }
}
