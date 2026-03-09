package com.passbolt.mobile.android.feature.authentication.mfa.yubikey

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.feature.authentication.auth.usecase.RefreshSessionUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyYubikeyUseCase
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeyIntent.ValidateYubikeyOtp
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.SnackbarErrorType.EMPTY_OTP
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
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ScanYubikeyViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<SignOutUseCase>() }
                    single { mock<VerifyYubikeyUseCase>() }
                    single { mock<RefreshSessionUseCase>() }
                    factory { params ->
                        ScanYubikeyViewModel(
                            authToken = params[0],
                            hasOtherProvider = params[1],
                            signOutUseCase = get(),
                            verifyYubikeyUseCase = get(),
                            refreshSessionUseCase = get(),
                        )
                    }
                },
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: ScanYubikeyViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `null otp emits empty otp error`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(ValidateYubikeyOtp(null))
                val error = awaitItem()
                assertIs<ShowErrorSnackbar>(error)
                assertThat(error.kind).isEqualTo(EMPTY_OTP)
            }
        }

    @Test
    fun `yubikey not from current user shows dialog`() =
        runTest {
            val verifyYubikeyUseCase: VerifyYubikeyUseCase = get()
            verifyYubikeyUseCase.stub {
                onBlocking { execute(any()) } doReturn VerifyYubikeyUseCase.Output.YubikeyNotFromCurrentUser
            }

            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false) })

            viewModel.viewState.test {
                awaitItem() // initial state
                viewModel.onIntent(ValidateYubikeyOtp("yubikey-otp-value"))
                val progressState = awaitItem()
                assertThat(progressState.showProgress).isTrue()
                val dialogState = awaitItem()
                assertThat(dialogState.showNotFromCurrentUserDialog).isTrue()
                val finalState = awaitItem()
                assertThat(finalState.showProgress).isFalse()
            }
        }

    private companion object {
        const val AUTH_TOKEN = "test-auth-token"
    }
}
