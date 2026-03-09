package com.passbolt.mobile.android.feature.authentication.auth

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.GetAccountDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.SaveServerFingerprintUseCase
import com.passbolt.mobile.android.core.accounts.usecase.privatekey.GetPrivateKeyUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.SaveSelectedAccountUseCase
import com.passbolt.mobile.android.core.authenticationcore.passphrase.GetPassphraseUseCase
import com.passbolt.mobile.android.core.authenticationcore.session.SaveSessionUseCase
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.inappreview.InAppReviewInteractor
import com.passbolt.mobile.android.core.mvp.authentication.MfaProvidersHandler
import com.passbolt.mobile.android.core.navigation.ActivityIntents.AuthConfig
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.passphrasememorycache.PotentialPassphrase
import com.passbolt.mobile.android.core.preferences.usecase.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.core.security.rootdetection.RootDetector
import com.passbolt.mobile.android.core.security.runtimeauth.RuntimeAuthenticatedFlag
import com.passbolt.mobile.android.encryptedstorage.biometric.BiometricCipher
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.BiometricAuthenticationSuccess
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.ConnectToExistingAccount
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.DismissNoAccountExplanation
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.ForgotPassword
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.GoBack
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.MfaSucceeded
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.OpenHelpMenu
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.PassphraseInputChanged
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.SignIn
import com.passbolt.mobile.android.feature.authentication.auth.AuthIntent.SignOut
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.AuthSuccess
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.HideKeyboard
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.NavigateToAccountList
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.authentication.auth.AuthSideEffect.SnackbarErrorType.WRONG_PASSPHRASE
import com.passbolt.mobile.android.feature.authentication.auth.AuthState.RefreshAuthReason.PASSPHRASE
import com.passbolt.mobile.android.feature.authentication.auth.AuthState.RefreshAuthReason.SESSION
import com.passbolt.mobile.android.feature.authentication.auth.challenge.MfaStatusProvider
import com.passbolt.mobile.android.feature.authentication.auth.usecase.BiometryInteractor
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetAndVerifyServerKeysAndTimeInteractor
import com.passbolt.mobile.android.feature.authentication.auth.usecase.PostSignInActionsInteractor
import com.passbolt.mobile.android.feature.authentication.auth.usecase.RefreshSessionUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignInVerifyInteractor
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyPassphraseUseCase
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
import org.koin.core.module.dsl.singleOf
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
import javax.crypto.Cipher
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<GetAccountDataUseCase>() }
                    single { mock<GetPrivateKeyUseCase>() }
                    single { mock<VerifyPassphraseUseCase>() }
                    single { mock<BiometricCipher>() }
                    single { mock<GetPassphraseUseCase>() }
                    single { mock<PassphraseMemoryCache>() }
                    single { mock<RootDetector>() }
                    single { mock<BiometryInteractor>() }
                    single { mock<GetGlobalPreferencesUseCase>() }
                    single { mock<SaveSessionUseCase>() }
                    single { mock<SaveSelectedAccountUseCase>() }
                    single { mock<SignOutUseCase>() }
                    single { mock<SaveServerFingerprintUseCase>() }
                    single { mock<MfaStatusProvider>() }
                    single { mock<GetAndVerifyServerKeysAndTimeInteractor>() }
                    single { mock<SignInVerifyInteractor>() }
                    single { mock<InAppReviewInteractor>() }
                    single { mock<PostSignInActionsInteractor>() }
                    single { mock<RefreshSessionUseCase>() }
                    single { RuntimeAuthenticatedFlag() }
                    singleOf(::SignInIdlingResource)
                    factoryOf(::MfaProvidersHandler)
                    factory { params ->
                        AuthViewModel(
                            authConfig = params.get(),
                            userId = params.get(),
                            appContext = params.get(),
                            getAccountDataUseCase = get(),
                            getPrivateKeyUseCase = get(),
                            verifyPassphraseUseCase = get(),
                            biometricCipher = get(),
                            getPassphraseUseCase = get(),
                            passphraseMemoryCache = get(),
                            rootDetector = get(),
                            biometryInteractor = get(),
                            getGlobalPreferencesUseCase = get(),
                            runtimeAuthenticatedFlag = get(),
                            saveSessionUseCase = get(),
                            saveSelectedAccountUseCase = get(),
                            signOutUseCase = get(),
                            saveServerFingerprintUseCase = get(),
                            mfaStatusProvider = get(),
                            getAndVerifyServerKeysInteractor = get(),
                            signInVerifyInteractor = get(),
                            inAppReviewInteractor = get(),
                            signInIdlingResource = get(),
                            postSignInActionsInteractor = get(),
                            refreshSessionUseCase = get(),
                            mfaProvidersHandler = get(),
                        )
                    }
                },
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase = get()
        whenever(getGlobalPreferencesUseCase.execute(any())) doReturn
            GetGlobalPreferencesUseCase.Output(
                areDebugLogsEnabled = false,
                debugLogFileCreationDateTime = null,
                debugLogLastAppVersion = null,
                isDeveloperModeEnabled = false,
                isHideRootDialogEnabled = true,
            )

        val getAccountDataUseCase: GetAccountDataUseCase = get()
        whenever(getAccountDataUseCase.execute(UserIdInput(USER_ID))) doReturn accountData

        val getPrivateKeyUseCase: GetPrivateKeyUseCase = get()
        whenever(getPrivateKeyUseCase.execute(any())) doReturn GetPrivateKeyUseCase.Output("privateKey")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `account data is loaded on init`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AuthConfig.Startup, USER_ID, AppContext.APP) })

            viewModel.viewState.drop(1).test {
                val state = awaitItem()
                assertThat(state.accountData.label).isEqualTo("Test User")
                assertThat(state.accountData.email).isEqualTo("test@passbolt.com")
                assertThat(state.accountData.domain).isEqualTo("https://passbolt.com")
                assertThat(state.accountData.avatarUrl).isEqualTo("https://avatar.url")
            }
        }

    @Test
    fun `startup config has no auth reason`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AuthConfig.Startup, USER_ID, AppContext.APP) })

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.authReason).isNull()
            }
        }

    @Test
    fun `refresh passphrase config sets passphrase auth reason`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AuthConfig.RefreshPassphrase, USER_ID, AppContext.APP) })

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.authReason).isEqualTo(PASSPHRASE)
            }
        }

    @Test
    fun `sign in config sets session auth reason`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AuthConfig.SignIn, USER_ID, AppContext.APP) })

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.authReason).isEqualTo(SESSION)
            }
        }

    @Test
    fun `auth button disabled when passphrase empty`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AuthConfig.Startup, USER_ID, AppContext.APP) })

            viewModel.onIntent(PassphraseInputChanged(ByteArray(0)))

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.isAuthButtonEnabled).isFalse()
            }
        }

    @Test
    fun `auth button enabled when passphrase not empty`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AuthConfig.Startup, USER_ID, AppContext.APP) })

            viewModel.onIntent(PassphraseInputChanged("test".toByteArray()))

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.isAuthButtonEnabled).isTrue()
            }
        }

    @Test
    fun `wrong passphrase shows error snackbar`() =
        runTest {
            val verifyPassphraseUseCase: VerifyPassphraseUseCase = get()
            whenever(verifyPassphraseUseCase.execute(any())) doReturn
                VerifyPassphraseUseCase.Output(isCorrect = false)

            viewModel = get(parameters = { parametersOf(AuthConfig.Startup, USER_ID, AppContext.APP) })

            viewModel.sideEffect.test {
                viewModel.onIntent(PassphraseInputChanged("wrong".toByteArray()))
                viewModel.onIntent(SignIn)
                assertIs<HideKeyboard>(awaitItem())
                val error = awaitItem()
                assertIs<ShowErrorSnackbar>(error)
                assertThat(error.kind)
                    .isEqualTo(WRONG_PASSPHRASE)
            }
        }

    @Test
    fun `correct passphrase with refresh passphrase config emits auth success`() =
        runTest {
            val verifyPassphraseUseCase: VerifyPassphraseUseCase = get()
            whenever(verifyPassphraseUseCase.execute(any())) doReturn
                VerifyPassphraseUseCase.Output(isCorrect = true)

            viewModel = get(parameters = { parametersOf(AuthConfig.RefreshPassphrase, USER_ID, AppContext.APP) })

            viewModel.sideEffect.test {
                viewModel.onIntent(PassphraseInputChanged("correct".toByteArray()))
                viewModel.onIntent(SignIn)
                assertIs<HideKeyboard>(awaitItem())
                assertIs<AuthSuccess>(awaitItem())
            }
        }

    @Test
    fun `setup config shows leave confirmation flag`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AuthConfig.Setup, USER_ID, AppContext.APP) })

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.canShowLeaveConfirmation).isTrue()
            }
        }

    @Test
    fun `non-setup config does not show leave confirmation flag`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AuthConfig.SignIn, USER_ID, AppContext.APP) })

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.canShowLeaveConfirmation).isFalse()
            }
        }

    @Test
    fun `back click emits navigate back`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AuthConfig.SignIn, USER_ID, AppContext.APP) })

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)
                assertIs<NavigateBack>(awaitItem())
            }
        }

    @Test
    fun `help click shows help menu`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AuthConfig.Startup, USER_ID, AppContext.APP) })

            viewModel.onIntent(OpenHelpMenu)

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.showHelpMenu).isTrue()
            }
        }

    @Test
    fun `connect to existing account emits navigate to account list`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AuthConfig.Startup, USER_ID, AppContext.APP) })

            viewModel.sideEffect.test {
                viewModel.onIntent(ConnectToExistingAccount)
                assertIs<NavigateToAccountList>(awaitItem())
            }
        }

    @Test
    fun `mfa succeeded with passphrase config emits auth success`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AuthConfig.RefreshPassphrase, USER_ID, AppContext.APP) })

            viewModel.sideEffect.test {
                viewModel.onIntent(MfaSucceeded(null))
                assertIs<AuthSuccess>(awaitItem())
            }
        }

    @Test
    fun `refresh session tries refresh first on passphrase verified`() =
        runTest {
            val verifyPassphraseUseCase: VerifyPassphraseUseCase = get()
            whenever(verifyPassphraseUseCase.execute(any())) doReturn
                VerifyPassphraseUseCase.Output(isCorrect = true)

            val refreshSessionUseCase: RefreshSessionUseCase = get()
            whenever(refreshSessionUseCase.execute(any())) doReturn
                RefreshSessionUseCase.Output.Success

            viewModel = get(parameters = { parametersOf(AuthConfig.RefreshSession, USER_ID, AppContext.APP) })

            viewModel.sideEffect.test {
                viewModel.onIntent(PassphraseInputChanged("passphrase".toByteArray()))
                viewModel.onIntent(SignIn)
                assertIs<HideKeyboard>(awaitItem())
                assertIs<AuthSuccess>(awaitItem())
            }
        }

    @Test
    fun `forgot password click shows dialog`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AuthConfig.Startup, USER_ID, AppContext.APP) })

            viewModel.onIntent(ForgotPassword)

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.showForgotPasswordDialog).isTrue()
            }
        }

    @Test
    fun `dismiss forgot password dialog hides dialog`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AuthConfig.Startup, USER_ID, AppContext.APP) })

            viewModel.onIntent(ForgotPassword)
            viewModel.onIntent(DismissNoAccountExplanation)

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.showForgotPasswordDialog).isFalse()
            }
        }

    @Test
    fun `biometric auth success with passphrase config emits auth success`() =
        runTest {
            val mockCipher = mock<Cipher>()
            whenever(mockCipher.iv) doReturn ByteArray(0)

            val getPassphraseUseCase: GetPassphraseUseCase = get()
            whenever(getPassphraseUseCase.execute(any())) doReturn
                GetPassphraseUseCase.Output(
                    PotentialPassphrase.Passphrase("passphrase".toByteArray()),
                )

            viewModel = get(parameters = { parametersOf(AuthConfig.RefreshPassphrase, USER_ID, AppContext.APP) })

            viewModel.sideEffect.test {
                viewModel.onIntent(BiometricAuthenticationSuccess(mockCipher))
                assertIs<AuthSuccess>(awaitItem())
            }
        }

    @Test
    fun `reject changed server fingerprint emits finish affinity`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AuthConfig.Startup, USER_ID, AppContext.APP) })

            viewModel.sideEffect.test {
                viewModel.onIntent(AuthIntent.RejectChangedServerFingerprint)
                assertIs<AuthSideEffect.FinishAffinity>(awaitItem())
            }
        }

    @Test
    fun `feature flags sign out emits navigate back`() =
        runTest {
            val signOutUseCase: SignOutUseCase = get()
            signOutUseCase.stub {
                onBlocking { execute(any()) } doReturn Unit
            }

            viewModel = get(parameters = { parametersOf(AuthConfig.Startup, USER_ID, AppContext.APP) })

            viewModel.sideEffect.test {
                viewModel.onIntent(SignOut)
                assertIs<NavigateBack>(awaitItem())
            }
        }

    private companion object {
        const val USER_ID = "test-user-id"

        private val accountData =
            GetAccountDataUseCase.Output(
                firstName = "First",
                lastName = "Last",
                email = "test@passbolt.com",
                avatarUrl = "https://avatar.url",
                url = "https://passbolt.com",
                serverId = "server-id",
                label = "Test User",
                role = "user",
            )
    }
}
