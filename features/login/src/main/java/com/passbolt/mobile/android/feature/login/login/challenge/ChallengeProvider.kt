package com.passbolt.mobile.android.feature.login.login.challenge

import com.google.gson.Gson
import com.passbolt.mobile.android.dto.request.ChallengeDto
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.storage.usecase.GetPrivateKeyUseCase
import java.lang.Exception
import java.time.Instant
import java.util.UUID

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
class ChallengeProvider(
    private val gson: Gson,
    private val openPgp: OpenPgp,
    private val privateKeyUseCase: GetPrivateKeyUseCase
) {

    suspend fun get(
        version: String,
        domain: String,
        serverPublicKey: String,
        passphrase: ByteArray,
        userId: String
    ): Output {
        val privateKey = requireNotNull(privateKeyUseCase.execute(GetPrivateKeyUseCase.Input(userId)).privateKey)

        val tokenExpiry = getVerifyTokenExpiry()

        val challengeJson = ChallengeDto(version, domain, UUID.randomUUID().toString(), tokenExpiry)
            .run { gson.toJson(this) }

        return try {
            val encryptedChallenge = openPgp.encryptSignMessageArmored(
                publicKey = serverPublicKey,
                privateKey = privateKey,
                passphrase = passphrase,
                message = challengeJson
            )
            Output.Success(encryptedChallenge)
        } catch (e: Exception) {
            Output.WrongPassphrase
        }
    }

    private fun getVerifyTokenExpiry() =
        Instant.now().epochSecond + TOKEN_VALIDATION_TIME

    sealed class Output {
        class Success(
            val challenge: String
        ) : Output()

        object WrongPassphrase : Output()
    }

    companion object {
        private const val TOKEN_VALIDATION_TIME = 120
    }
}
