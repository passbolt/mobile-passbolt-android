package com.passbolt.mobile.android.feature.authentication.auth.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.idlingresource.SignOutIdlingResource
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.RemoveSelectedAccountUseCase
import com.passbolt.mobile.android.domain.auth.AuthRepository
import com.passbolt.mobile.android.domain.auth.usecase.GetSessionUseCase
import com.passbolt.mobile.android.domain.secrets.usecase.offline.ClearOfflineSecretsUseCase
import timber.log.Timber

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */
class SignOutUseCase(
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val removeSelectedAccountUseCase: RemoveSelectedAccountUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val authRepository: AuthRepository,
    private val getSessionUseCase: GetSessionUseCase,
    private val signOutIdlingResource: SignOutIdlingResource,
    private val clearOfflineSecretsUseCase: ClearOfflineSecretsUseCase,
) : AsyncUseCase<Unit, Unit> {
    override suspend fun execute(input: Unit) {
        Timber.d("Signing out")
        signOutIdlingResource.setIdle(false)
        getSessionUseCase.execute(Unit).refreshToken?.let {
            authRepository.signOut(it)
        }
        passphraseMemoryCache.clear()
        // an explicit sign-out ends offline access: drop the cached ciphertext
        clearOfflineSecretsUseCase.execute(Unit)
        getSelectedAccountUseCase.execute(Unit).selectedAccount?.let {
            removeSelectedAccountUseCase.execute(Unit)
        }
        signOutIdlingResource.setIdle(true)
    }
}
