package com.passbolt.mobile.android.core.commongroups.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.database.impl.groups.AddLocalGroupsUseCase
import com.passbolt.mobile.android.database.impl.groups.RemoveLocalGroupsUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.ui.GroupModel

class RebuildGroupsTablesUseCase(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val removeLocalGroupsUseCase: RemoveLocalGroupsUseCase,
    private val addLocalGroupsUseCase: AddLocalGroupsUseCase
) : AsyncUseCase<RebuildGroupsTablesUseCase.Input, Unit> {

    override suspend fun execute(input: Input) {
        val selectedAccount = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        removeLocalGroupsUseCase.execute(UserIdInput(selectedAccount))
        addLocalGroupsUseCase.execute(AddLocalGroupsUseCase.Input(input.list))
    }

    class Input(
        val list: List<GroupModel>
    )
}
