package com.passbolt.mobile.android.accountinit

import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.SaveCurrentApiUrlUseCase
import com.passbolt.mobile.android.database.usecase.SaveResourcesDatabasePassphraseUseCase
import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.SaveSelectedAccountUseCase
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.UpdateAccountDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.privatekey.SavePrivateKeyUseCase
import com.passbolt.mobile.android.core.accounts.usecase.account.SaveAccountUseCase
import org.koin.core.component.KoinComponent

class AccountInitializer(
    private val saveCurrentApiUrlUseCase: SaveCurrentApiUrlUseCase,
    private val saveResourcesDatabasePassphraseUseCase: SaveResourcesDatabasePassphraseUseCase,
    private val saveSelectedAccountUseCase: SaveSelectedAccountUseCase,
    private val updateAccountDataUseCase: UpdateAccountDataUseCase,
    private val savePrivateKeyUseCase: SavePrivateKeyUseCase,
    private val managedAccountIntentCreator: ManagedAccountIntentCreator,
    private val saveAccountUseCase: SaveAccountUseCase
) : KoinComponent {

    fun initializeAccount() {
        saveCurrentApiUrlUseCase.execute(
            SaveCurrentApiUrlUseCase.Input(managedAccountIntentCreator.getDomain())
        )
        saveSelectedAccountUseCase.execute(
            UserIdInput(managedAccountIntentCreator.getUserLocalId())
        )
        saveAccountUseCase.execute(
            UserIdInput(managedAccountIntentCreator.getUserLocalId())
        )
        saveResourcesDatabasePassphraseUseCase.execute(
            SaveResourcesDatabasePassphraseUseCase.Input(TEST_DATABASE_PASSWORD)
        )
        updateAccountDataUseCase.execute(
            UpdateAccountDataUseCase.Input(
                userId = managedAccountIntentCreator.getUserLocalId(),
                url = managedAccountIntentCreator.getDomain(),
                firstName = managedAccountIntentCreator.getFirstName(),
                lastName = managedAccountIntentCreator.getLastName(),
                email = managedAccountIntentCreator.getUsername(),
                serverId = managedAccountIntentCreator.getUserServerId()
            )
        )
        savePrivateKeyUseCase.execute(
            SavePrivateKeyUseCase.Input(
                managedAccountIntentCreator.getUserLocalId(),
                managedAccountIntentCreator.getArmoredPrivateKey()
            )
        )
    }

    private companion object {
        private const val TEST_DATABASE_PASSWORD = "TEST_DB_PASS"
    }
}
