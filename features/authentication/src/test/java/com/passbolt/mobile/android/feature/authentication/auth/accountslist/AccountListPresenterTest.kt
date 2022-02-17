package com.passbolt.mobile.android.feature.authentication.auth.accountslist

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.entity.account.Account
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListContract
import com.passbolt.mobile.android.mappers.AccountModelMapper
import com.passbolt.mobile.android.storage.usecase.accounts.GetAllAccountsDataUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.ui.AccountModelUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

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
@ExperimentalCoroutinesApi
class AccountListPresenterTest : KoinTest {

    private val presenter: AccountsListContract.Presenter by inject()
    private val view = mock<AccountsListContract.View>()
    private val accountEntityToUiMapper: AccountModelMapper by inject()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testAccountListModule)
    }

    @Before
    fun setUp() {
        whenever(mockGetSelectedAccountUseCase.execute(Unit)).doReturn(GetSelectedAccountUseCase.Output("id"))
    }

    @Test
    fun `test if account list is displayed with add new account at start`() {
        whenever(mockGetAllAccountsDataUseCase.execute(Unit)).doReturn(GetAllAccountsDataUseCase.Output(SAVED_ACCOUNT))

        presenter.attach(view)

        argumentCaptor<List<AccountModelUi>>().apply {
            verify(view).showAccounts(capture())
            // verify list content
            assertThat(firstValue.size).isEqualTo(SAVED_ACCOUNT.size + 1)
            assertThat(firstValue).isEqualTo(accountEntityToUiMapper.map(SAVED_ACCOUNT))

            // verify if add new account button is added
            assertThat(firstValue).contains(AccountModelUi.AddNewAccount)
        }
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `test if turing on remove mode updates view correct`() {
        whenever(mockGetAllAccountsDataUseCase.execute(Unit)).doReturn(GetAllAccountsDataUseCase.Output(SAVED_ACCOUNT))

        presenter.attach(view)
        presenter.removeAnAccountClick()

        argumentCaptor<List<AccountModelUi>>().apply {
            verify(view, times(2)).showAccounts(capture())
            // verify if add new account button is hid
            assertThat(secondValue).doesNotContain(AccountModelUi.AddNewAccount)
        }
        verify(view).hideRemoveAccounts()
        verify(view).showDoneRemovingAccounts()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `test if turing off remove mode updates view correct`() {
        whenever(mockGetAllAccountsDataUseCase.execute(Unit)).doReturn(GetAllAccountsDataUseCase.Output(SAVED_ACCOUNT))

        presenter.attach(view)
        presenter.doneRemovingAccountsClick()

        argumentCaptor<List<AccountModelUi>>().apply {
            verify(view, times(2)).showAccounts(capture())
            // verify if add new account button is shown
            assertThat(secondValue).contains(AccountModelUi.AddNewAccount)
        }
        verify(view).showRemoveAccounts()
        verify(view).hideDoneRemovingAccounts()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `test view shows confirm remove account dialog`() {
        whenever(mockGetAllAccountsDataUseCase.execute(Unit)).doReturn(GetAllAccountsDataUseCase.Output(SAVED_ACCOUNT))

        presenter.attach(view)
        val accountToRemove = accountEntityToUiMapper.map(SAVED_ACCOUNT)[0] as AccountModelUi.AccountModel
        presenter.removeAccountClick(accountToRemove)

        verify(view).showAccounts(any())
        verify(view).showRemoveAccountConfirmationDialog(accountToRemove)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `test view shows updated account list after removal`() {
        val mutableAccountList = SAVED_ACCOUNTS.toMutableList()
        whenever(mockGetAllAccountsDataUseCase.execute(Unit)).doReturn(
            GetAllAccountsDataUseCase.Output(mutableAccountList)
        )
        mockRemoveAllAccountsDataUseCase.stub {
            onBlocking { execute(any()) }.then { mutableAccountList.removeAt(0) }
        }

        presenter.attach(view)
        val accountToRemove = accountEntityToUiMapper.map(SAVED_ACCOUNT)[0]
        presenter.confirmRemoveAccountClick(accountToRemove as AccountModelUi.AccountModel)

        argumentCaptor<List<AccountModelUi>>().apply {
            verify(view, times(3)).showAccounts(capture())
            assertThat(thirdValue.size).isEqualTo(1)
        }
        verify(view).showAccountRemovedSnackbar()
        verify(view).hideRemoveAccounts()
        verify(view).showDoneRemovingAccounts()
        verify(view, never()).navigateToStartUp()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `test view show navigate to startup after last account is removed`() {
        val mutableAccountList = SAVED_ACCOUNT.toMutableList()
        whenever(mockGetAllAccountsDataUseCase.execute(Unit)).doReturn(
            GetAllAccountsDataUseCase.Output(mutableAccountList)
        )
        mockRemoveAllAccountsDataUseCase.stub {
            onBlocking { execute(any()) }.then { mutableAccountList.removeAt(0) }
        }

        presenter.attach(view)
        val accountToRemove = accountEntityToUiMapper.map(SAVED_ACCOUNT)[0]
        presenter.confirmRemoveAccountClick(accountToRemove as AccountModelUi.AccountModel)

        verify(view).navigateToStartUp()
    }

    private companion object {

        private val SAVED_ACCOUNT = listOf(
            Account(userId = "1", null, null, null, null, "dev.test", "server_id", "label")
        )

        private val SAVED_ACCOUNTS = listOf(
            Account(userId = "1", null, null, null, null, "dev.test", "server_id", "label"),
            Account(userId = "2", null, null, null, null, "dev.test", "server_id", "label")
        )
    }

}
