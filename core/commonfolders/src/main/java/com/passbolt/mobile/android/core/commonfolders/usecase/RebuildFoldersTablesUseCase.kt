package com.passbolt.mobile.android.core.commonfolders.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.database.impl.folders.AddLocalFoldersUseCase
import com.passbolt.mobile.android.database.impl.folders.RemoveLocalFoldersUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.ui.FolderModel

class RebuildFoldersTablesUseCase(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val removeLocalFoldersUseCase: RemoveLocalFoldersUseCase,
    private val addLocalFoldersUseCase: AddLocalFoldersUseCase
) : AsyncUseCase<RebuildFoldersTablesUseCase.Input, Unit> {

    override suspend fun execute(input: Input) {
        val selectedAccount = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        removeLocalFoldersUseCase.execute(UserIdInput(selectedAccount))
        addLocalFoldersUseCase.execute(AddLocalFoldersUseCase.Input(input.list))
    }

    class Input(
        val list: List<FolderModel>
    )
}
