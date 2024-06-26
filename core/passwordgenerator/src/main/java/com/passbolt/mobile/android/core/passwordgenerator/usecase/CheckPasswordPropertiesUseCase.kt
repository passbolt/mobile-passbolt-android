package com.passbolt.mobile.android.core.passwordgenerator.usecase

import com.passbolt.mobile.android.common.hash.MessageDigestHash
import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.core.passwordgenerator.entropy.EntropyCalculator
import com.passbolt.mobile.android.pwnedpasswordsapi.range.PwnedPasswordRepository
import com.passbolt.mobile.android.pwnedpasswordsapi.range.PwnedPasswordsApi

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
class CheckPasswordPropertiesUseCase(
    private val pwnedPasswordRepository: PwnedPasswordRepository,
    private val messageDigestHash: MessageDigestHash,
    private val entropyCalculator: EntropyCalculator
) : AsyncUseCase<CheckPasswordPropertiesUseCase.Input, CheckPasswordPropertiesUseCase.Output> {

    override suspend fun execute(input: Input): Output {
        return if (entropyCalculator.getSecretEntropy(input.password) < Output.Weak.WEAK_ENTROPY_THRESHOLD) {
            Output.Weak
        } else {
            if (input.password.length > PwnedPasswordsApi.PARTIAL_HASH_LENGTH) {
                checkPwnedPassword(input)
            } else {
                Output.Fine
            }
        }
    }

    private suspend fun checkPwnedPassword(input: Input): Output {
        val passwordHash = messageDigestHash.sha1(input.password)
        val passwordHashPrefix = passwordHash.substring(0, PwnedPasswordsApi.PARTIAL_HASH_LENGTH)
        val passwordHashSuffix = passwordHash.substring(PwnedPasswordsApi.PARTIAL_HASH_LENGTH)

        return when (val response = pwnedPasswordRepository.getPwnedPasswordsSuffixes(passwordHashPrefix)) {
            is NetworkResult.Failure -> Output.Failure(response)
            is NetworkResult.Success -> {
                response.value.lines().forEach {
                    val (suffix, count) = it.split(":")
                    if (suffix.equals(passwordHashSuffix, ignoreCase = true)) {
                        return Output.Pwned(count.toInt())
                    }
                }
                return Output.Fine
            }
        }
    }

    sealed class Output {

        data object Fine : Output()

        data class Pwned(
            val dataBreachesCount: Int
        ) : Output()

        data object Weak : Output() {
            const val WEAK_ENTROPY_THRESHOLD = 60
        }

        data class Failure<T : Any>(
            val response: NetworkResult.Failure<T>
        ) : Output()
    }

    data class Input(
        val password: String
    )
}
