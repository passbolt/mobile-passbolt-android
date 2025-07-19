package com.passbolt.mobile.android.core.tags

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.tags.usecase.db.AddLocalTagsUseCase
import com.passbolt.mobile.android.core.tags.usecase.db.RemoveLocalTagsUseCase
import com.passbolt.mobile.android.ui.ResourceModelWithAttributes

class RebuildTagsTablesUseCase(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val removeLocalTagsUseCase: RemoveLocalTagsUseCase,
    private val addLocalTagsUseCase: AddLocalTagsUseCase,
) : AsyncUseCase<RebuildTagsTablesUseCase.Input, Unit> {
    override suspend fun execute(input: Input) {
        val selectedAccount = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        removeLocalTagsUseCase.execute(UserIdInput(selectedAccount))
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
