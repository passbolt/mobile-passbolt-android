package com.password.mobile.android.feature.home.switchaccount

import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.DataRefreshStatus
import com.passbolt.mobile.android.core.fulldatarefresh.HomeDataInteractor
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountContract
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class SwitchAccountTest : KoinTest {
    private val presenter: SwitchAccountContract.Presenter by inject()
    private val view: SwitchAccountContract.View = mock()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testSwitchAccountModule)
        }

    @Before
    fun setUp() {
        whenever(mockFullDataRefreshExecutor.dataRefreshStatusFlow).doReturn(
            flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success)),
        )
    }

    @Test
    fun `sign out click should sign out the user`() =
        runTest {
            presenter.attach(view)
            presenter.signOutClick()
            presenter.signOutConfirmed()

            verify(view).showSignOutDialog()
            verify(mockSignOutUseCase).execute(Unit)
            verify(view, times(2)).showProgress()
            verify(view, times(2)).hideProgress()
            verify(view).navigateToStartup()
        }

    @Test
    fun `account list should be shown on ui`() {
        whenever(mockGetSelectedAccountUseCase.execute(Unit)).doReturn(GetSelectedAccountUseCase.Output("selected"))
        val appContext = AppContext.APP
        val uiMapped = switchAccountModelMapper.map(accountsList, currentAccount = "selected", appContext)

        presenter.attach(view)
        presenter.argsRetrieved(appContext)

        verify(view).showAccountsList(uiMapped)
    }

    @Test
    fun `see details should show account details`() {
        presenter.attach(view)
        presenter.seeDetailsClick()

        verify(view).navigateToAccountDetails()
    }
}
