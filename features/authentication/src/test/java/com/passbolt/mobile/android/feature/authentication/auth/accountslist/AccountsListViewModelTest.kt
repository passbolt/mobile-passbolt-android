package com.passbolt.mobile.android.feature.authentication.auth.accountslist

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.accounts.usecase.accounts.GetAllAccountsDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.SaveCurrentApiUrlUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.SaveSelectedAccountUseCase
import com.passbolt.mobile.android.core.navigation.ActivityIntents.AuthConfig.ManageAccount
import com.passbolt.mobile.android.core.navigation.ActivityIntents.AuthConfig.Startup
import com.passbolt.mobile.android.database.DatabaseProvider
import com.passbolt.mobile.android.entity.account.Account
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListIntent.AddAccount
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListIntent.ConfirmRemoveAccount
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListIntent.DismissRemoveAccountDialog
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListIntent.EnterRemoveAccountMode
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListIntent.ExitRemoveAccountMode
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListIntent.GoBack
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListIntent.RemoveAccount
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListIntent.SelectAccount
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListSideEffect.Finish
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListSideEffect.FinishAffinity
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListSideEffect.NavigateToSetup
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListSideEffect.NavigateToStartUp
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListSideEffect.ShowSuccessSnackBar
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListViewModel
import com.passbolt.mobile.android.feature.authentication.auth.usecase.RemoveAllAccountDataUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.mappers.AccountModelMapper
import com.passbolt.mobile.android.ui.AccountModelUi
import com.passbolt.mobile.android.ui.AccountModelUi.AddNewAccount
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
class AccountsListViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<GetAllAccountsDataUseCase>() }
                    single { mock<GetSelectedAccountUseCase>() }
                    single { mock<SaveSelectedAccountUseCase>() }
                    single { mock<SaveCurrentApiUrlUseCase>() }
                    single { mock<RemoveAllAccountDataUseCase>() }
                    single { mock<SignOutUseCase>() }
                    single { mock<DatabaseProvider>() }
                    factoryOf(::AccountModelMapper)
                    factory { params ->
                        AccountsListViewModel(
                            authConfig = params.get(),
                            getAllAccountsDataUseCase = get(),
                            getSelectedAccountUseCase = get(),
                            saveSelectedAccountUseCase = get(),
                            accountModelMapper = get(),
                            removeAllAccountDataUseCase = get(),
                            signOutUseCase = get(),
                            saveCurrentApiUrlUseCase = get(),
                            databaseProvider = get(),
                        )
                    }
                },
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: AccountsListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val getSelectedAccountUseCase = get<GetSelectedAccountUseCase>()
        whenever(getSelectedAccountUseCase.execute(Unit)) doReturn GetSelectedAccountUseCase.Output(CURRENT_USER_ID)

        val getAllAccountsDataUseCase = get<GetAllAccountsDataUseCase>()
        whenever(getAllAccountsDataUseCase.execute(Unit)) doReturn GetAllAccountsDataUseCase.Output(SAVED_ACCOUNT)

        val databaseProvider = get<DatabaseProvider>()
        databaseProvider.stub {
            onBlocking { delete(any()) } doReturn Unit
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `account list is displayed with add new account at start`() =
        runTest {
            viewModel = get(parameters = { parametersOf(Startup) })

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.accounts.size).isEqualTo(SAVED_ACCOUNT.size + 1)
                assertThat(state.accounts).contains(AddNewAccount)
            }
        }

    @Test
    fun `current user id is set from selected account`() =
        runTest {
            viewModel = get(parameters = { parametersOf(Startup) })

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.currentUserId).isEqualTo(CURRENT_USER_ID)
            }
        }

    @Test
    fun `turning on remove mode sets remove mode`() =
        runTest {
            viewModel = get(parameters = { parametersOf(Startup) })
            viewModel.onIntent(EnterRemoveAccountMode)

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.isRemoveMode).isTrue()
            }
        }

    @Test
    fun `turning off remove mode clears remove mode`() =
        runTest {
            viewModel = get(parameters = { parametersOf(Startup) })
            viewModel.onIntent(EnterRemoveAccountMode)
            viewModel.onIntent(ExitRemoveAccountMode)

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.isRemoveMode).isFalse()
            }
        }

    @Test
    fun `remove account click shows confirmation dialog`() =
        runTest {
            viewModel = get(parameters = { parametersOf(Startup) })
            val accountToRemove = get<AccountModelMapper>().map(SAVED_ACCOUNT)[0] as AccountModelUi.AccountModel

            viewModel.onIntent(RemoveAccount(accountToRemove))

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.accountToRemove).isEqualTo(accountToRemove)
                assertThat(state.showAccountRemovalConfirmation).isTrue()
            }
        }

    @Test
    fun `dismiss remove account dialog clears confirmation`() =
        runTest {
            viewModel = get(parameters = { parametersOf(Startup) })
            val accountToRemove = get<AccountModelMapper>().map(SAVED_ACCOUNT)[0] as AccountModelUi.AccountModel

            viewModel.onIntent(RemoveAccount(accountToRemove))
            viewModel.onIntent(DismissRemoveAccountDialog)

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.showAccountRemovalConfirmation).isFalse()
            }
        }

    @Test
    fun `account removal updates list and shows snackbar`() =
        runTest {
            val mutableAccountList = SAVED_ACCOUNTS.toMutableList()
            val getAllAccountsDataUseCase = get<GetAllAccountsDataUseCase>()
            whenever(getAllAccountsDataUseCase.execute(Unit)) doReturn
                GetAllAccountsDataUseCase.Output(mutableAccountList)
            val removeAllAccountDataUseCase = get<RemoveAllAccountDataUseCase>()
            removeAllAccountDataUseCase.stub {
                onBlocking { execute(any()) }.then { mutableAccountList.removeAt(0) }
            }

            viewModel = get(parameters = { parametersOf(Startup) })
            val accountToRemove = get<AccountModelMapper>().map(SAVED_ACCOUNT)[0] as AccountModelUi.AccountModel

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmRemoveAccount(accountToRemove))
                assertIs<ShowSuccessSnackBar>(awaitItem())
            }
            viewModel.viewState.test {
                val state = awaitItem()
                val accountModels = state.accounts.filterIsInstance<AccountModelUi.AccountModel>()
                assertThat(accountModels.size).isEqualTo(1)
            }
        }

    @Test
    fun `navigate to startup when last account is removed`() =
        runTest {
            val mutableAccountList = SAVED_ACCOUNT.toMutableList()
            val getAllAccountsDataUseCase = get<GetAllAccountsDataUseCase>()
            whenever(getAllAccountsDataUseCase.execute(Unit)) doReturn GetAllAccountsDataUseCase.Output(mutableAccountList)
            val removeAllAccountDataUseCase = get<RemoveAllAccountDataUseCase>()
            removeAllAccountDataUseCase.stub {
                onBlocking { execute(any()) }.then { mutableAccountList.removeAt(0) }
            }

            viewModel = get(parameters = { parametersOf(Startup) })
            val accountToRemove = get<AccountModelMapper>().map(SAVED_ACCOUNT)[0] as AccountModelUi.AccountModel

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmRemoveAccount(accountToRemove))
                assertIs<ShowSuccessSnackBar>(awaitItem())
                assertIs<NavigateToStartUp>(awaitItem())
            }
        }

    @Test
    fun `auth mode shows header and hides toolbar`() =
        runTest {
            viewModel = get(parameters = { parametersOf(Startup) })

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.showHeader).isTrue()
                assertThat(state.showManageAccountsTopBar).isFalse()
            }
        }

    @Test
    fun `manage mode shows toolbar and hides header`() =
        runTest {
            viewModel = get(parameters = { parametersOf(ManageAccount) })

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.showManageAccountsTopBar).isTrue()
                assertThat(state.showHeader).isFalse()
            }
        }

    @Test
    fun `add account emits navigate to setup`() =
        runTest {
            viewModel = get(parameters = { parametersOf(Startup) })

            viewModel.sideEffect.test {
                viewModel.onIntent(AddAccount)

                assertIs<NavigateToSetup>(awaitItem())
            }
        }

    @Test
    fun `back in auth mode emits finish affinity`() =
        runTest {
            viewModel = get(parameters = { parametersOf(Startup) })

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)

                assertIs<FinishAffinity>(awaitItem())
            }
        }

    @Test
    fun `back in manage mode without changes emits finish`() =
        runTest {
            viewModel = get(parameters = { parametersOf(ManageAccount) })

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)

                assertIs<Finish>(awaitItem())
            }
        }

    @Test
    fun `back in manage mode after account switch emits finish affinity`() =
        runTest {
            val accounts =
                listOf(
                    Account(userId = "1", null, null, null, null, "dev.test", "server_id", "label"),
                    Account(userId = "2", null, null, null, null, "dev.test", "server_id", "label"),
                )
            val getAllAccountsDataUseCase = get<GetAllAccountsDataUseCase>()
            whenever(getAllAccountsDataUseCase.execute(Unit)) doReturn
                GetAllAccountsDataUseCase.Output(accounts)

            val signOutUseCase = get<SignOutUseCase>()
            signOutUseCase.stub {
                onBlocking { execute(Unit) } doReturn Unit
            }

            viewModel = get(parameters = { parametersOf(ManageAccount) })
            val otherAccount = get<AccountModelMapper>().map(accounts)[1] as AccountModelUi.AccountModel

            viewModel.sideEffect.test {
                viewModel.onIntent(SelectAccount(otherAccount))
                awaitItem() // NavigateToNewAccountSignIn

                viewModel.onIntent(GoBack)

                assertIs<FinishAffinity>(awaitItem())
            }
        }

    private companion object {
        private const val CURRENT_USER_ID = "1"

        private val SAVED_ACCOUNT =
            listOf(
                Account(userId = "1", null, null, null, null, "dev.test", "server_id", "label"),
            )

        private val SAVED_ACCOUNTS =
            listOf(
                Account(userId = "1", null, null, null, null, "dev.test", "server_id", "label"),
                Account(userId = "2", null, null, null, null, "dev.test", "server_id", "label"),
            )
    }
}
