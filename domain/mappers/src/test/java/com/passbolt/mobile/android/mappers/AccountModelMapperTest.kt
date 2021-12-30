package com.passbolt.mobile.android.mappers

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.passbolt.mobile.android.entity.account.Account
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
class AccountModelMapperTest : KoinTest {
    private val mapper: AccountModelMapper by inject()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testMappersModule)
    }

    @Before
    fun setUp() {
        whenever(mockGetSelectedAccountUseCase.execute(Unit)).doReturn(GetSelectedAccountUseCase.Output("id1"))
    }

    @Test
    fun `Mapping accounts entities should return proper models with add account`() {
        val accountsList = listOf(
            Account(
                userId = "id1",
                firstName = "firstName1",
                lastName = "lastName1",
                email = "email1",
                avatarUrl = "avatarUrl1",
                url = "url1",
                serverId = "serverId1",
                label = "label"
            ),
            Account(
                userId = "id2",
                firstName = "firstName2",
                lastName = "lastName2",
                email = "email2",
                avatarUrl = "avatarUrl2",
                url = "url2",
                serverId = "serverId2",
                label = "label"
            )
        )
        val result = mapper.map(accountsList)
        val expected = listOf(
            AccountModelUi.AccountModel(
                userId = "id1",
                email = "email1",
                title = "label",
                isFirstItem = true,
                isTrashIconVisible = false,
                avatar = "avatarUrl1",
                url = "url1",
                isCurrentUser = true,
            ),
            AccountModelUi.AccountModel(
                userId = "id2",
                email = "email2",
                title = "label",
                isFirstItem = false,
                isTrashIconVisible = false,
                avatar = "avatarUrl2",
                url = "url2"
            ),
            AccountModelUi.AddNewAccount
        )
        assertThat(result).isEqualTo(expected)
    }

}

