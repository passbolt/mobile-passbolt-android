package com.passbolt.mobile.android.feature.autofill.resources

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.database.usecase.AddLocalResourcesUseCase
import com.passbolt.mobile.android.database.usecase.RemoveLocalResourcesUseCase
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase

class FetchAndUpdateDatabaseUseCase(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val removeLocalResourcesUseCase: RemoveLocalResourcesUseCase,
    private val addLocalResourcesUseCase: AddLocalResourcesUseCase
) : AsyncUseCase<FetchAndUpdateDatabaseUseCase.Input, Unit> {

    override suspend fun execute(input: Input) {
        val selectedAccount = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        removeLocalResourcesUseCase.execute(UserIdInput(selectedAccount))
        addLocalResourcesUseCase.execute(AddLocalResourcesUseCase.Input(input.list, selectedAccount))
    }

    class Input(
        val list: List<ResourceModel>
    )
}
