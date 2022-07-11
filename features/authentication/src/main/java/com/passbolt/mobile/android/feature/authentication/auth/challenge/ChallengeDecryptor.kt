package com.passbolt.mobile.android.feature.authentication.auth.challenge

import com.google.gson.Gson
import com.passbolt.mobile.android.common.extension.erase
import com.passbolt.mobile.android.dto.response.ChallengeResponseDto
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.privatekey.GetPrivateKeyUseCase

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
class ChallengeDecryptor(
    private val openPgp: OpenPgp,
    private val getPrivateKeyUseCase: GetPrivateKeyUseCase,
    private val gson: Gson
) {

    suspend fun decrypt(
        serverPublicKey: String,
        passphrase: ByteArray,
        userId: String,
        challenge: String
    ): Output {
        val passphraseCopy = passphrase.copyOf()
        val privateKey = getPrivateKeyUseCase.execute(UserIdInput(userId)).privateKey
        return when (val decryptedChallenge = openPgp.decryptVerifyMessageArmored(
            publicKey = serverPublicKey,
            privateKey = privateKey,
            passphrase = passphrase,
            cipherText = challenge
        )) {
            is OpenPgpResult.Error -> Output.DecryptionError(decryptedChallenge.error.message)
            is OpenPgpResult.Result -> {
                passphraseCopy.erase()
                Output.DecryptedChallenge(
                    gson.fromJson(
                        String(decryptedChallenge.result),
                        ChallengeResponseDto::class.java
                    )
                )
            }
        }
    }

    sealed class Output {

        data class DecryptedChallenge(val challenge: ChallengeResponseDto) : Output()

        data class DecryptionError(val message: String?) : Output()
    }
}
