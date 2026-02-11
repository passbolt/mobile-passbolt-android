package com.passbolt.mobile.android.feature.setup.welcome

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
import com.passbolt.mobile.android.core.accounts.AccountKitParser
import com.passbolt.mobile.android.core.accounts.AccountsInteractor
import com.passbolt.mobile.android.core.accounts.AccountsInteractor.InjectAccountFailureType.ACCOUNT_ALREADY_LINKED
import com.passbolt.mobile.android.core.accounts.AccountsInteractor.InjectAccountFailureType.ERROR_NON_HTTPS_DOMAIN
import com.passbolt.mobile.android.core.accounts.AccountsInteractor.InjectAccountFailureType.ERROR_WHEN_SAVING_PRIVATE_KEY
import com.passbolt.mobile.android.core.navigation.AccountSetupDataModel
import com.passbolt.mobile.android.core.preferences.usecase.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.core.security.rootdetection.RootDetector
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.AccessLogs
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.AcknowledgeDeviceRooted
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.ConnectToExistingAccount
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.DismissHelpMenu
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.DismissNoAccountExplanation
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.GoUp
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.ImportProfileManually
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.Initialize
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.OpenHelpMenu
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.SeeNoAccountExplanation
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeIntent.SelectedAccountKit
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeSideEffect.NavigateToImportProfile
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeSideEffect.NavigateToLogs
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeSideEffect.NavigateToSummary
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeSideEffect.NavigateToTransferDetails
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeSideEffect.NavigateUp
import com.passbolt.mobile.android.ui.ResultStatus.AlreadyLinked
import com.passbolt.mobile.android.ui.ResultStatus.Failure
import com.passbolt.mobile.android.ui.ResultStatus.HttpNotSupported
import com.passbolt.mobile.android.ui.ResultStatus.Success
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
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class WelcomeViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<RootDetector>() }
                        single { mock<GetGlobalPreferencesUseCase>() }
                        single { mock<AccountsInteractor>() }
                        single { mock<AccountKitParser>() }
                        factoryOf(::WelcomeViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: WelcomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val rootDetector: RootDetector = get()
        whenever(rootDetector.isDeviceRooted()) doReturn false

        val getGlobalPreferencesUseCase = get<GetGlobalPreferencesUseCase>()
        whenever(getGlobalPreferencesUseCase.execute(Unit)) doReturn
            GetGlobalPreferencesUseCase.Output(
                areDebugLogsEnabled = false,
                debugLogFileCreationDateTime = null,
                isDeveloperModeEnabled = false,
                isHideRootDialogEnabled = false,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `initial state should have default values`() =
        runTest {
            viewModel = get()

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.showBackNavigation).isFalse()
                assertThat(state.showNoAccountExplanation).isFalse()
                assertThat(state.showHelpMenu).isFalse()
                assertThat(state.showDeviceRooted).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `initialize with isTaskRoot true should not show back navigation`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(isTaskRoot = true))

            viewModel.viewState.test {
                assertThat(awaitItem().showBackNavigation).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `initialize with isTaskRoot false should show back navigation`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(isTaskRoot = false))

            viewModel.viewState.test {
                assertThat(awaitItem().showBackNavigation).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `initialize should show device rooted when device is rooted and dialog not hidden`() =
        runTest {
            val rootDetector: RootDetector = get()
            whenever(rootDetector.isDeviceRooted()) doReturn true

            viewModel = get()
            viewModel.onIntent(Initialize(isTaskRoot = true))

            viewModel.viewState.test {
                assertThat(awaitItem().showDeviceRooted).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `initialize should not show device rooted when dialog is hidden`() =
        runTest {
            val rootDetector: RootDetector = get()
            whenever(rootDetector.isDeviceRooted()) doReturn true

            val getGlobalPreferencesUseCase = get<GetGlobalPreferencesUseCase>()
            whenever(getGlobalPreferencesUseCase.execute(Unit)) doReturn
                GetGlobalPreferencesUseCase.Output(
                    areDebugLogsEnabled = false,
                    debugLogFileCreationDateTime = null,
                    isDeveloperModeEnabled = false,
                    isHideRootDialogEnabled = true,
                )

            viewModel = get()
            viewModel.onIntent(Initialize(isTaskRoot = true))

            viewModel.viewState.test {
                assertThat(awaitItem().showDeviceRooted).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `go back intent should emit navigate back side effect`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoUp)
                assertIs<NavigateUp>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `see no account explanation should show explanation`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(SeeNoAccountExplanation)

            viewModel.viewState.test {
                assertThat(awaitItem().showNoAccountExplanation).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `dismiss no account explanation should hide explanation`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(SeeNoAccountExplanation)

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(DismissNoAccountExplanation)
                assertThat(awaitItem().showNoAccountExplanation).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `connect to existing account should emit navigate to transfer details`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(ConnectToExistingAccount)
                assertIs<NavigateToTransferDetails>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `open help menu should show help menu`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(OpenHelpMenu)

            viewModel.viewState.test {
                assertThat(awaitItem().showHelpMenu).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `dismiss help menu should hide help menu`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(OpenHelpMenu)
            viewModel.onIntent(DismissHelpMenu)

            viewModel.viewState.test {
                assertThat(awaitItem().showHelpMenu).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `import profile manually should emit navigate to import profile`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(ImportProfileManually)

            viewModel.sideEffect.test {
                assertIs<NavigateToImportProfile>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `access logs should emit navigate to logs`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(AccessLogs)

            viewModel.sideEffect.test {
                assertIs<NavigateToLogs>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `acknowledge device rooted should hide device rooted warning`() =
        runTest {
            val rootDetector: RootDetector = get()
            whenever(rootDetector.isDeviceRooted()) doReturn true

            viewModel = get()

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(Initialize(isTaskRoot = true))
                assertThat(awaitItem().showDeviceRooted).isTrue()

                viewModel.onIntent(AcknowledgeDeviceRooted)
                assertThat(awaitItem().showDeviceRooted).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `selected account kit with valid data should inject account and navigate to summary with success`() =
        runTest {
            val accountKitParser: AccountKitParser = get()
            val accountsInteractor: AccountsInteractor = get()

            val onSuccessCaptor = argumentCaptor<(AccountSetupDataModel) -> Unit>()
            val onFailureCaptor = argumentCaptor<(String) -> Unit>()
            val onSuccessInteractorCaptor = argumentCaptor<(String) -> Unit>()
            val onFailureInteractorCaptor = argumentCaptor<(AccountsInteractor.InjectAccountFailureType) -> Unit>()

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(SelectedAccountKit(ACCOUNT_KIT))

                testScheduler.advanceUntilIdle()

                verify(accountKitParser).parseAndVerify(
                    eq(ACCOUNT_KIT),
                    onSuccess = onSuccessCaptor.capture(),
                    onFailure = onFailureCaptor.capture(),
                )

                onSuccessCaptor.firstValue.invoke(ACCOUNT_SETUP_DATA)

                verify(accountsInteractor).injectPredefinedAccountData(
                    eq(ACCOUNT_SETUP_DATA),
                    onSuccess = onSuccessInteractorCaptor.capture(),
                    onFailure = onFailureInteractorCaptor.capture(),
                )

                onSuccessInteractorCaptor.firstValue.invoke(USER_ID)

                val effect = awaitItem()
                assertIs<NavigateToSummary>(effect)
                assertIs<Success>(effect.status)
                assertThat(effect.status.userId).isEqualTo(USER_ID)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `selected account kit with parsing failure should navigate to summary with failure`() =
        runTest {
            val accountKitParser: AccountKitParser = get()

            val onSuccessCaptor = argumentCaptor<(AccountSetupDataModel) -> Unit>()
            val onFailureCaptor = argumentCaptor<(String) -> Unit>()

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(SelectedAccountKit(ACCOUNT_KIT))

                testScheduler.advanceUntilIdle()

                verify(accountKitParser).parseAndVerify(
                    eq(ACCOUNT_KIT),
                    onSuccess = onSuccessCaptor.capture(),
                    onFailure = onFailureCaptor.capture(),
                )

                onFailureCaptor.firstValue.invoke("Parse error")

                val effect = awaitItem()
                assertIs<NavigateToSummary>(effect)
                assertIs<Failure>(effect.status)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `account already linked failure should navigate to summary with already linked status`() =
        runTest {
            val accountKitParser: AccountKitParser = get()
            val accountsInteractor: AccountsInteractor = get()

            val onSuccessCaptor = argumentCaptor<(AccountSetupDataModel) -> Unit>()
            val onFailureCaptor = argumentCaptor<(String) -> Unit>()
            val onSuccessInteractorCaptor = argumentCaptor<(String) -> Unit>()
            val onFailureInteractorCaptor = argumentCaptor<(AccountsInteractor.InjectAccountFailureType) -> Unit>()

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(SelectedAccountKit(ACCOUNT_KIT))

                testScheduler.advanceUntilIdle()

                verify(accountKitParser).parseAndVerify(
                    eq(ACCOUNT_KIT),
                    onSuccess = onSuccessCaptor.capture(),
                    onFailure = onFailureCaptor.capture(),
                )

                onSuccessCaptor.firstValue.invoke(ACCOUNT_SETUP_DATA)

                verify(accountsInteractor).injectPredefinedAccountData(
                    eq(ACCOUNT_SETUP_DATA),
                    onSuccess = onSuccessInteractorCaptor.capture(),
                    onFailure = onFailureInteractorCaptor.capture(),
                )

                onFailureInteractorCaptor.firstValue.invoke(ACCOUNT_ALREADY_LINKED)

                val effect = awaitItem()
                assertIs<NavigateToSummary>(effect)
                assertIs<AlreadyLinked>(effect.status)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `non https domain failure should navigate to summary with http not supported status`() =
        runTest {
            val accountKitParser: AccountKitParser = get()
            val accountsInteractor: AccountsInteractor = get()

            val onSuccessCaptor = argumentCaptor<(AccountSetupDataModel) -> Unit>()
            val onFailureCaptor = argumentCaptor<(String) -> Unit>()
            val onSuccessInteractorCaptor = argumentCaptor<(String) -> Unit>()
            val onFailureInteractorCaptor = argumentCaptor<(AccountsInteractor.InjectAccountFailureType) -> Unit>()

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(SelectedAccountKit(ACCOUNT_KIT))

                testDispatcher.scheduler.advanceUntilIdle()

                verify(accountKitParser).parseAndVerify(
                    eq(ACCOUNT_KIT),
                    onSuccess = onSuccessCaptor.capture(),
                    onFailure = onFailureCaptor.capture(),
                )

                onSuccessCaptor.firstValue.invoke(ACCOUNT_SETUP_DATA)

                verify(accountsInteractor).injectPredefinedAccountData(
                    eq(ACCOUNT_SETUP_DATA),
                    onSuccess = onSuccessInteractorCaptor.capture(),
                    onFailure = onFailureInteractorCaptor.capture(),
                )

                onFailureInteractorCaptor.firstValue.invoke(ERROR_NON_HTTPS_DOMAIN)

                val effect = awaitItem()
                assertIs<NavigateToSummary>(effect)
                assertIs<HttpNotSupported>(effect.status)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `error when saving private key failure should navigate to summary with failure status`() =
        runTest {
            val accountKitParser: AccountKitParser = get()
            val accountsInteractor: AccountsInteractor = get()

            val onSuccessCaptor = argumentCaptor<(AccountSetupDataModel) -> Unit>()
            val onFailureCaptor = argumentCaptor<(String) -> Unit>()
            val onSuccessInteractorCaptor = argumentCaptor<(String) -> Unit>()
            val onFailureInteractorCaptor = argumentCaptor<(AccountsInteractor.InjectAccountFailureType) -> Unit>()

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(SelectedAccountKit(ACCOUNT_KIT))

                testDispatcher.scheduler.advanceUntilIdle()

                verify(accountKitParser).parseAndVerify(
                    eq(ACCOUNT_KIT),
                    onSuccess = onSuccessCaptor.capture(),
                    onFailure = onFailureCaptor.capture(),
                )

                onSuccessCaptor.firstValue.invoke(ACCOUNT_SETUP_DATA)

                verify(accountsInteractor).injectPredefinedAccountData(
                    eq(ACCOUNT_SETUP_DATA),
                    onSuccess = onSuccessInteractorCaptor.capture(),
                    onFailure = onFailureInteractorCaptor.capture(),
                )

                onFailureInteractorCaptor.firstValue.invoke(ERROR_WHEN_SAVING_PRIVATE_KEY)

                val effect = awaitItem()
                assertIs<NavigateToSummary>(effect)
                assertIs<Failure>(effect.status)
            }
        }

    private companion object {
        private const val ACCOUNT_KIT = "account_kit_data"
        private const val USER_ID = "user_id_123"

        private val ACCOUNT_SETUP_DATA =
            AccountSetupDataModel(
                serverUserId = "server_user_id",
                domain = "https://passbolt.com",
                userName = "john.doe@passbolt.com",
                firstName = "John",
                lastName = "Doe",
                avatarUrl = "https://passbolt.com/avatar.jpg",
                keyFingerprint = "fingerprint",
                armoredKey = "armored_key_data",
            )
    }
}
