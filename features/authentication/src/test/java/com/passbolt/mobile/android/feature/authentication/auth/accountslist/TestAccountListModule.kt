package com.passbolt.mobile.android.feature.authentication.auth.accountslist

import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.accounts.usecase.accounts.GetAllAccountsDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.SaveCurrentApiUrlUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.SaveSelectedAccountUseCase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.database.DatabaseProvider
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListContract
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListPresenter
import com.passbolt.mobile.android.feature.authentication.auth.usecase.RemoveAllAccountDataUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.mappers.AccountModelMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.dsl.module
import org.mockito.kotlin.mock

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

internal val mockGetAllAccountsDataUseCase = mock<GetAllAccountsDataUseCase>()
internal val mockGetSelectedAccountUseCase = mock<GetSelectedAccountUseCase>()
internal val mockRemoveAllAccountsDataUseCase = mock<RemoveAllAccountDataUseCase>()
internal val saveCurrentApiUrlUseCase = mock<SaveCurrentApiUrlUseCase>()
internal val mockSignOutUseCase = mock<SignOutUseCase>()
internal val mockSaveSelectedAccountUseCase = mock<SaveSelectedAccountUseCase>()
internal val mockDatapaseProvider = mock<DatabaseProvider>()

@ExperimentalCoroutinesApi
val testAccountListModule =
    module {
        factory<AccountsListContract.Presenter> {
            AccountsListPresenter(
                getAllAccountsDataUseCase = mockGetAllAccountsDataUseCase,
                getSelectedAccountUseCase = mockGetSelectedAccountUseCase,
                accountModelMapper = get(),
                removeAllAccountDataUseCase = mockRemoveAllAccountsDataUseCase,
                signOutUseCase = mockSignOutUseCase,
                coroutineLaunchContext = get(),
                saveCurrentApiUrlUseCase = get(),
                saveSelectedAccountUseCase = get(),
                databaseProvider = mockDatapaseProvider,
            )
        }
        factory<CoroutineLaunchContext> { TestCoroutineLaunchContext() }
        factory {
            AccountModelMapper()
        }
        factory { saveCurrentApiUrlUseCase }
        factory { mockSaveSelectedAccountUseCase }
    }
