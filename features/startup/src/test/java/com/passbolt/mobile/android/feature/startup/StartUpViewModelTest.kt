package com.passbolt.mobile.android.feature.startup

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.accounts.usecase.accounts.GetAccountsUseCase
import com.passbolt.mobile.android.core.navigation.AccountSetupDataModel
import com.passbolt.mobile.android.feature.startup.StartUpSideEffect.NavigateToSetup
import com.passbolt.mobile.android.feature.startup.StartUpSideEffect.NavigateToSignIn
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class StartUpViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<GetAccountsUseCase>() }
                    factory { (accountSetupDataModel: AccountSetupDataModel?) ->
                        StartUpViewModel(
                            accountSetupDataModel = accountSetupDataModel,
                            getAccountsUseCase = get(),
                        )
                    }
                },
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should navigate to setup when no accounts exist`() =
        runTest {
            val getAccountsUseCase: GetAccountsUseCase = get()
            whenever(getAccountsUseCase.execute(Unit)) doReturn GetAccountsUseCase.Output(emptySet())

            val viewModel: StartUpViewModel = get { parametersOf(null) }

            viewModel.sideEffect.test {
                val effect = assertIs<NavigateToSetup>(awaitItem())
                assertThat(effect.accountSetupDataModel).isNull()
            }
        }

    @Test
    fun `should navigate to sign in when accounts exist`() =
        runTest {
            val getAccountsUseCase: GetAccountsUseCase = get()
            whenever(getAccountsUseCase.execute(Unit)) doReturn GetAccountsUseCase.Output(setOf("userId"))

            val viewModel: StartUpViewModel = get { parametersOf(null) }

            viewModel.sideEffect.test {
                assertIs<NavigateToSignIn>(awaitItem())
            }
        }

    @Test
    fun `should navigate to setup with data when account setup data is provided`() =
        runTest {
            val accountSetupData =
                AccountSetupDataModel(
                    serverUserId = "userId",
                    userName = "user",
                    domain = "https://passbolt.com",
                    firstName = "John",
                    lastName = "Doe",
                    avatarUrl = "https://passbolt.com/avatar.jpg",
                    keyFingerprint = "fingerprint",
                    armoredKey = "key",
                )

            val getAccountsUseCase: GetAccountsUseCase = get()
            whenever(getAccountsUseCase.execute(Unit)) doReturn GetAccountsUseCase.Output(setOf("existingUser"))

            val viewModel: StartUpViewModel = get { parametersOf(accountSetupData) }

            viewModel.sideEffect.test {
                val effect = assertIs<NavigateToSetup>(awaitItem())
                assertThat(effect.accountSetupDataModel).isEqualTo(accountSetupData)
            }
        }
}
