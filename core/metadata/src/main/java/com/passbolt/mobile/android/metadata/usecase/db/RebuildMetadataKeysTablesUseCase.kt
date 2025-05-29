package com.passbolt.mobile.android.metadata.usecase.db

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.accounts.usecase.SelectedAccountUseCase
import com.passbolt.mobile.android.ui.ParsedMetadataKeyModel

class RebuildMetadataKeysTablesUseCase(
    private val removeLocalMetadataKeysUseCase: RemoveLocalMetadataKeysUseCase,
    private val addLocalMetadataKeys: AddLocalMetadataKeysUseCase,
) : AsyncUseCase<RebuildMetadataKeysTablesUseCase.Input, Unit>,
    SelectedAccountUseCase {
    override suspend fun execute(input: Input) {
        removeLocalMetadataKeysUseCase.execute(UserIdInput(selectedAccountId))
        addLocalMetadataKeys.execute(AddLocalMetadataKeysUseCase.Input(input.metadataKeys))
    }

    data class Input(
        val metadataKeys: List<ParsedMetadataKeyModel>,
    )
}
