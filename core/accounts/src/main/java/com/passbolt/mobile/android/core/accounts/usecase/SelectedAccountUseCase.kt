package com.passbolt.mobile.android.core.accounts.usecase

import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

interface SelectedAccountUseCase : KoinComponent {
    val selectedAccountId: String
        get() =
            requireNotNull(get<GetSelectedAccountUseCase>().execute(Unit).selectedAccount) {
                "${javaClass.name} is a selected account use case, but no account is selected"
            }

    val selectedAccountUserIdInput: UserIdInput
        get() = UserIdInput(selectedAccountId)
}
