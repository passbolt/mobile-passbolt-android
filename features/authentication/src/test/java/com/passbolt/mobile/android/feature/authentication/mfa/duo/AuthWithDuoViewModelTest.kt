package com.passbolt.mobile.android.feature.authentication.mfa.duo

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetDuoPromptUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyDuoCallbackUseCase
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoIntent.AuthenticateWithDuo
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoIntent.Close
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoIntent.ConfirmSetupLeave
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoIntent.DismissSetupLeave
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoIntent.DuoAuthFinished
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoSideEffect.CloseAndNavigateToStartup
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoSideEffect.NavigateToLogin
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoSideEffect.NotifyVerificationSucceeded
import com.passbolt.mobile.android.feature.authentication.mfa.duo.duowebviewsheet.DuoState
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
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class AuthWithDuoViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<GetDuoPromptUseCase>() }
                    single { mock<VerifyDuoCallbackUseCase>() }
                    single { mock<SignOutUseCase>() }
                    factory { params ->
                        AuthWithDuoViewModel(
                            authToken = params[0],
                            hasOtherProvider = params[1],
                            isSetupFlow = params[2],
                            getDuoPromptUseCase = get(),
                            verifyDuoCallbackUseCase = get(),
                            signOutUseCase = get(),
                        )
                    }
                },
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: AuthWithDuoViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `full duo flow stores cookie and verifies with it`() =
        runTest {
            val getDuoPromptUseCase: GetDuoPromptUseCase = get()
            getDuoPromptUseCase.stub {
                on { execute(any()) } doReturn
                    GetDuoPromptUseCase.Output.Success(
                        duoPromptUrl = "https://duo.example.com/prompt",
                        passboltDuoCookieUuid = "duo-cookie-123",
                    )
            }
            val verifyDuoCallbackUseCase: VerifyDuoCallbackUseCase = get()
            verifyDuoCallbackUseCase.stub {
                on { execute(any()) } doReturn
                    VerifyDuoCallbackUseCase.Output.Success(
                        mfaHeader = "mfa-token-abc",
                    )
            }
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.onIntent(AuthenticateWithDuo)

            viewModel.viewState.drop(2).test {
                val state = awaitItem()
                assertThat(state.showDuoWebViewSheet).isTrue()
                assertThat(state.duoPromptUrl).isEqualTo("https://duo.example.com/prompt")
            }

            viewModel.sideEffect.test {
                viewModel.onIntent(DuoAuthFinished(DuoState("state-val", "duo-code-val")))

                val success = awaitItem()
                assertIs<NotifyVerificationSucceeded>(success)
                assertThat(success.mfaHeader).isEqualTo("mfa-token-abc")
            }

            verify(verifyDuoCallbackUseCase).execute(
                VerifyDuoCallbackUseCase.Input(
                    jwtHeader = AUTH_TOKEN,
                    passboltDuoCookieUuid = "duo-cookie-123",
                    duoState = "state-val",
                    duoCode = "duo-code-val",
                ),
            )
        }

    @Test
    fun `unauthorized prompt navigates to login`() =
        runTest {
            get<GetDuoPromptUseCase>().stub {
                on { execute(any()) } doReturn GetDuoPromptUseCase.Output.Unauthorized
            }
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(AuthenticateWithDuo)
                assertIs<NavigateToLogin>(awaitItem())
            }
        }

    @Test
    fun `duo auth finished when auth token is null does not call verify`() =
        runTest {
            val verifyDuoCallbackUseCase: VerifyDuoCallbackUseCase = get()

            viewModel = get(parameters = { parametersOf(null, false, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(DuoAuthFinished(DuoState("state", "code")))

                verifyNoInteractions(verifyDuoCallbackUseCase)
                viewModel.viewState.drop(1).test {
                    assertThat(awaitItem().showProgress).isFalse()
                }
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
    }
}
