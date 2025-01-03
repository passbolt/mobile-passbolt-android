package com.passbolt.mobile.android.accountinit

import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.accounts.usecase.accounts.GetAccountsUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.RemoveAllAccountDataUseCase
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class AccountDataCleaner(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
) : KoinComponent {

    fun clearAccountData() {
        runBlocking {
            get<GetAccountsUseCase>().execute(Unit).users.forEach {
                get<RemoveAllAccountDataUseCase>().execute(UserIdInput(it))
            }
        }
    }
}
