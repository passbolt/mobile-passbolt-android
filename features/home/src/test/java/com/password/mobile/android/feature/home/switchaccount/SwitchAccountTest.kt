package com.password.mobile.android.feature.home.switchaccount

import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountContract
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject

@ExperimentalCoroutinesApi
class SwitchAccountTest : KoinTest {

    private val presenter: SwitchAccountContract.Presenter by inject()
    private val view: SwitchAccountContract.View = mock()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testSwitchAccountModule)
    }

    @Test
    fun `sign out click should sign out the user`() = runBlockingTest {
        presenter.attach(view)
        presenter.signOutClick()
        presenter.signOutConfirmed()

        verify(view).showSignOutDialog()
        verify(mockSignOutUseCase).execute(Unit)
        verify(view).showProgress()
        verify(view).hideProgress()
        verify(view).navigateToStartup()
    }

    @Test
    fun `account list should be shown on ui`() {
        val uiMapped = switchAccountModelMapper.map(accountsList)

        presenter.attach(view)

        verify(view).showAccountsList(uiMapped)
    }

    @Test
    fun `see details should show account details`() {
        presenter.attach(view)
        presenter.seeDetailsClick()

        verify(view).navigateToAccountDetails()
    }
}
