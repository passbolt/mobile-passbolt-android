package com.passbolt.mobile.android.feature.accountdetails.screen

import com.passbolt.mobile.android.storage.usecase.accountdata.UpdateAccountDataUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

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
class AccountDetailsTest : KoinTest {

    private val presenter: AccountDetailsContract.Presenter by inject()
    private val view: AccountDetailsContract.View = mock()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testAccountDetailsModule)
    }

    @Test
    fun `account details should be shown`() {
        presenter.attach(view)

        verify(view).showLabel(LABEL)
        verify(view).showAvatar(AVATAR_URL)
        verify(view).showOrgUrl(SERVER_URL)
        verify(view).showEmail(EMAIL)
        verify(view).showName("$FIRST_NAME $LAST_NAME")
        verify(view).showRole(ROLE)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `label should be updated on save click`() {
        val newLabel = "NEW LABEL"

        presenter.attach(view)
        presenter.labelInputChanged(newLabel)
        presenter.saveClick()

        verify(mockUpdateAccountDataUseCase).execute(
            UpdateAccountDataUseCase.Input(
                userId = SELECTED_ACCOUNT_ID,
                label = newLabel
            )
        )
        verify(view).showLabelChanged()
    }

    @Test
    fun `transfer account to another device clicked should navigate to transfer account onboarding`() {
        presenter.attach(view)
        presenter.transferAccountClick()

        verify(view).navigateToTransferAccountOnboarding()
    }
}
