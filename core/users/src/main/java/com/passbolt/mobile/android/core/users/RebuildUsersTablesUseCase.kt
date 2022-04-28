package com.passbolt.mobile.android.core.users

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.database.impl.users.AddLocalUsersUseCase
import com.passbolt.mobile.android.database.impl.users.RemoveLocalUsersUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.ui.UserModel

class RebuildUsersTablesUseCase(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val removeLocalUsersUseCase: RemoveLocalUsersUseCase,
    private val addLocalUsersUseCase: AddLocalUsersUseCase
) : AsyncUseCase<RebuildUsersTablesUseCase.Input, Unit> {

    override suspend fun execute(input: Input) {
        val selectedAccount = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        removeLocalUsersUseCase.execute(UserIdInput(selectedAccount))
        addLocalUsersUseCase.execute(AddLocalUsersUseCase.Input(input.users))
    }

    class Input(
        val users: List<UserModel>
    )
}
