package com.passbolt.mobile.android.feature.secrets.usecase.decrypt

import com.passbolt.mobile.android.common.extension.erase
import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.cache.passphrase.PotentialPassphrase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.privatekey.GetPrivateKeyUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
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
class DecryptSecretUseCase(
    private val gopenPgp: OpenPgp,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val getPrivateKeyUseCase: GetPrivateKeyUseCase
) : AsyncUseCase<DecryptSecretUseCase.Input, DecryptSecretUseCase.Output> {

    override suspend fun execute(input: Input): Output {
        val account = UserIdInput(
            requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        )

        return try {
            val potentialPassphrase = passphraseMemoryCache.get()
            if (potentialPassphrase is PotentialPassphrase.Passphrase) {
                val passphraseCopy = potentialPassphrase.passphrase.copyOf()
                val decrypted = gopenPgp.decryptMessageArmored(
                    getPrivateKeyUseCase.execute(account).privateKey,
                    passphraseCopy,
                    input.encryptedSecret
                )
                passphraseCopy.erase()
                Output.DecryptedSecret(decrypted)
            } else {
                Output.Unauthorized
            }
        } catch (exception: Exception) {
            Timber.e(exception)
            Output.Failure(exception)
        }
    }

    data class Input(
        val encryptedSecret: String
    )

    sealed class Output {

        object Unauthorized : Output()

        data class Failure(val exception: Exception) : Output()

        data class DecryptedSecret(val decryptedSecret: ByteArray) : Output() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as DecryptedSecret

                if (!decryptedSecret.contentEquals(other.decryptedSecret)) return false

                return true
            }

            override fun hashCode(): Int {
                return decryptedSecret.contentHashCode()
            }
        }
    }
}
