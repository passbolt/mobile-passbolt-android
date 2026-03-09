package com.passbolt.mobile.android.feature.authentication

import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.GetAccountDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.SaveCurrentApiUrlUseCase
import com.passbolt.mobile.android.core.navigation.ActivityIntents.AuthConfig

class AuthenticationStartUpResolver(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val getAccountDataUseCase: GetAccountDataUseCase,
    private val saveCurrentApiUrlUseCase: SaveCurrentApiUrlUseCase,
) {
    data class Result(
        val skipAccountsList: Boolean,
        val initialUserId: String?,
    )

    fun resolve(
        authConfig: AuthConfig,
        userId: String?,
    ): Result {
        val currentAccount = userId ?: getSelectedAccountUseCase.execute(Unit).selectedAccount
        val skipAccountsList = authConfig is AuthConfig.Setup && currentAccount != null

        if (!skipAccountsList && authConfig !is AuthConfig.ManageAccount && currentAccount != null) {
            val account = getAccountDataUseCase.execute(UserIdInput(currentAccount))
            saveCurrentApiUrlUseCase.execute(SaveCurrentApiUrlUseCase.Input(account.url))
        }

        return Result(
            skipAccountsList = skipAccountsList,
            initialUserId = if (authConfig !is AuthConfig.ManageAccount) currentAccount else null,
        )
    }
}
