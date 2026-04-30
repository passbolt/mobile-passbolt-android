package com.passbolt.mobile.android.feature.authentication.mfa.duo

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetDuoPromptUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.RefreshSessionUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyDuoCallbackUseCase
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoIntent.AuthenticateWithDuo
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoIntent.DuoAuthFinished
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
                    single { mock<RefreshSessionUseCase>() }
                    single { mock<SignOutUseCase>() }
                    factory { params ->
                        AuthWithDuoViewModel(
                            authToken = params[0],
                            hasOtherProvider = params[1],
                            getDuoPromptUseCase = get(),
                            verifyDuoCallbackUseCase = get(),
                            refreshSessionUseCase = get(),
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
                onBlocking { execute(any()) } doReturn
                    GetDuoPromptUseCase.Output.Success(
                        duoPromptUrl = "https://duo.example.com/prompt",
                        passboltDuoCookieUuid = "duo-cookie-123",
                    )
            }
            val verifyDuoCallbackUseCase: VerifyDuoCallbackUseCase = get()
            verifyDuoCallbackUseCase.stub {
                onBlocking { execute(any()) } doReturn
                    VerifyDuoCallbackUseCase.Output.Success(
                        mfaHeader = "mfa-token-abc",
                    )
            }
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false) })

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
    fun `duo auth finished when auth token is null does not call verify`() =
        runTest {
            val verifyDuoCallbackUseCase: VerifyDuoCallbackUseCase = get()

            viewModel = get(parameters = { parametersOf(null, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(DuoAuthFinished(DuoState("state", "code")))

                verifyNoInteractions(verifyDuoCallbackUseCase)
                viewModel.viewState.drop(1).test {
                    assertThat(awaitItem().showProgress).isFalse()
                }
            }
        }

    private companion object {
        const val AUTH_TOKEN = "test-auth-token"
    }
}
