package com.passbolt.mobile.android.core.tags

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.tags.usecase.db.AddLocalTagsUseCase
import com.passbolt.mobile.android.ui.ResourceModelWithAttributes

// TODO MOB-3051 do not delete existing when rebuilding
class RebuildTagsTablesUseCase(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val addLocalTagsUseCase: AddLocalTagsUseCase,
) : AsyncUseCase<RebuildTagsTablesUseCase.Input, Unit> {
    override suspend fun execute(input: Input) {
        val selectedAccount = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
//        removeLocalTagsUseCase.execute(UserIdInput(selectedAccount))
        addLocalTagsUseCase.execute(
            AddLocalTagsUseCase.Input(
                input.tags,
                selectedAccount,
            ),
        )
    }

    data class Input(
        val tags: List<ResourceModelWithAttributes>,
    )
}
