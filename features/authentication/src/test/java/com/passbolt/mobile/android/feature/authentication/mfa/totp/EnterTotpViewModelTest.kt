package com.passbolt.mobile.android.feature.authentication.mfa.totp

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.feature.authentication.auth.usecase.RefreshSessionUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyTotpUseCase
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpIntent.ValidateOtp
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.ClearOtp
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.NavigateToLogin
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.SnackbarErrorType.GENERIC
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.SnackbarErrorType.SESSION_EXPIRED
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpSideEffect.SnackbarErrorType.WRONG_CODE
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpState.OtpTextColor.ERROR
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
import org.mockito.kotlin.whenever
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class EnterTotpViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<SignOutUseCase>() }
                    single { mock<VerifyTotpUseCase>() }
                    single { mock<RefreshSessionUseCase>() }
                    factory { params ->
                        EnterTotpViewModel(
                            authToken = params[0],
                            hasOtherProvider = params[1],
                            signOutUseCase = get(),
                            verifyTotpUseCase = get(),
                            refreshSessionUseCase = get(),
                        )
                    }
                },
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: EnterTotpViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `unauthorized triggers session refresh retry and on failure navigates to login`() =
        runTest {
            val verifyTotpUseCase: VerifyTotpUseCase = get()
            verifyTotpUseCase.stub {
                onBlocking { execute(any()) } doReturn VerifyTotpUseCase.Output.Unauthorized
            }

            val refreshSessionUseCase: RefreshSessionUseCase = get()
            whenever(refreshSessionUseCase.execute(any())) doReturn RefreshSessionUseCase.Output.Failure

            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(ValidateOtp("123456"))
                val error = awaitItem()
                assertIs<ShowErrorSnackbar>(error)
                assertThat(error.kind).isEqualTo(SESSION_EXPIRED)
                assertIs<NavigateToLogin>(awaitItem())
            }
        }

    @Test
    fun `wrong code sets error color and clears otp`() =
        runTest {
            val verifyTotpUseCase: VerifyTotpUseCase = get()
            verifyTotpUseCase.stub {
                onBlocking { execute(any()) } doReturn VerifyTotpUseCase.Output.WrongCode
            }

            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(ValidateOtp("000000"))
                val error = awaitItem()
                assertIs<ShowErrorSnackbar>(error)
                assertThat(error.kind).isEqualTo(WRONG_CODE)

                assertThat(viewModel.viewState.value.otpTextColor).isEqualTo(ERROR)

                assertIs<ClearOtp>(awaitItem())
            }
        }

    @Test
    fun `success with null mfaHeader emits generic error`() =
        runTest {
            val verifyTotpUseCase: VerifyTotpUseCase = get()
            verifyTotpUseCase.stub {
                onBlocking { execute(any()) } doReturn VerifyTotpUseCase.Output.Success(mfaHeader = null)
            }

            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(ValidateOtp("123456"))
                val error = awaitItem()
                assertIs<ShowErrorSnackbar>(error)
                assertThat(error.kind).isEqualTo(GENERIC)
            }
        }

    private companion object {
        const val AUTH_TOKEN = "test-auth-token"
    }
}
