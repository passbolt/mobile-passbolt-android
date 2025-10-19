package com.passbolt.mobile.android.core.resources.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.AddLocalResourcesUseCase
import com.passbolt.mobile.android.ui.ResourceModel

// TODO MOB-3051 do not delete existing when rebuilding
class RebuildResourceTablesUseCase(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val addLocalResourcesUseCase: AddLocalResourcesUseCase,
) : AsyncUseCase<RebuildResourceTablesUseCase.Input, Unit> {
    override suspend fun execute(input: Input) {
        val selectedAccount = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
//        removeLocalResourcesUseCase.execute(UserIdInput(selectedAccount))
        addLocalResourcesUseCase.execute(AddLocalResourcesUseCase.Input(input.resources, selectedAccount))
    }

    data class Input(
        val resources: List<ResourceModel>,
    )
}
