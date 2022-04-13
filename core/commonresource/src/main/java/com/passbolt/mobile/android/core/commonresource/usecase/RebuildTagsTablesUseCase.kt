package com.passbolt.mobile.android.core.commonresource.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.database.usecase.AddLocalTagsUseCase
import com.passbolt.mobile.android.database.usecase.RemoveLocalTagsUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.ui.ResourceModelWithTags

class RebuildTagsTablesUseCase(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val removeLocalTagsUseCase: RemoveLocalTagsUseCase,
    private val addLocalTagsUseCase: AddLocalTagsUseCase
) : AsyncUseCase<RebuildTagsTablesUseCase.Input, Unit> {

    override suspend fun execute(input: Input) {
        val selectedAccount = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        removeLocalTagsUseCase.execute(UserIdInput(selectedAccount))
        addLocalTagsUseCase.execute(
            AddLocalTagsUseCase.Input(
                input.list,
                selectedAccount
            )
        )
    }

    data class Input(
        val list: List<ResourceModelWithTags>
    )
}