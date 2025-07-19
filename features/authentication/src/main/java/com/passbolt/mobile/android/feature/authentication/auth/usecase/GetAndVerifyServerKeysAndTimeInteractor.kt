package com.passbolt.mobile.android.feature.authentication.auth.usecase

import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.GetAccountDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.IsServerFingerprintCorrectUseCase
import timber.log.Timber
import kotlin.time.measureTimedValue

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
class GetAndVerifyServerKeysAndTimeInteractor(
    private val fetchServerPublicPgpKeyUseCase: FetchServerPublicPgpKeyUseCase,
    private val fetchServerPublicRsaKeyUseCase: FetchServerPublicRsaKeyUseCase,
    private val saveServerPublicRsaKeyUseCase: SaveServerPublicRsaKeyUseCase,
    private val isServerFingerprintCorrectUseCase: IsServerFingerprintCorrectUseCase,
    private val getAccountDataUseCase: GetAccountDataUseCase,
    private val gopenPgpTimeUpdater: GopenPgpTimeUpdater,
) {
    suspend fun getAndVerifyServerKeys(
        userId: String,
        onError: (Error) -> Unit,
        onSuccess: suspend (Success) -> Unit,
    ) {
        Timber.d("Getting server pgp and rsa keys")
        val (pgpKey, getTimeRequestDuration) = measureTimedValue { fetchServerPublicPgpKeyUseCase.execute(Unit) }
        val rsaKey = fetchServerPublicRsaKeyUseCase.execute(Unit)

        if (pgpKey is FetchServerPublicPgpKeyUseCase.Output.Success &&
            rsaKey is FetchServerPublicRsaKeyUseCase.Output.Success
        ) {
            saveServerPublicRsaKeyUseCase.execute(SaveServerPublicRsaKeyUseCase.Input(userId, rsaKey.rsaKey))
            Timber.d("Getting server pgp and rsa keys succeeded")
            Timber.d("Checking if time adjustment is needed")
            val timeUpdateResult =
                gopenPgpTimeUpdater.updateTimeIfNeeded(pgpKey.serverTime, getTimeRequestDuration.inWholeSeconds)
            if (timeUpdateResult == GopenPgpTimeUpdater.Result.TIME_DELTA_TOO_BIG_FOR_SYNC) {
                onError(Error.TimeIsOutOfSync)
                return
            }
            Timber.d("Verifying server fingerprint")
            val input = IsServerFingerprintCorrectUseCase.Input(userId, pgpKey.fingerprint)
            if (!isServerFingerprintCorrectUseCase.execute(input).isCorrect) {
                Timber.d("Server key fingerprint has changed")
                onError(Error.IncorrectServerFingerprint(pgpKey.fingerprint))
            } else {
                Timber.d("Server key fingerprint is valid")
                onSuccess(Success(pgpKey.publicKey, pgpKey.fingerprint, rsaKey.rsaKey))
            }
        } else {
            if ((pgpKey as? FetchServerPublicPgpKeyUseCase.Output.Failure)
                    ?.error
                    ?.isServerNotReachable == true ||
                (rsaKey as? FetchServerPublicRsaKeyUseCase.Output.Failure)
                    ?.error
                    ?.isServerNotReachable == true
            ) {
                Timber.d("Server is not reachable")
                val accountData = getAccountDataUseCase.execute(UserIdInput(userId))
                onError(Error.ServerNotReachable(accountData.url))
            } else {
                Timber.d("Generic error occurred")
                onError(Error.Generic)
            }
        }
    }

    data class Success(
        val pgpKey: String,
        val pgpKeyFingerprint: String,
        val rsaKey: String,
    )

    sealed class Error {
        data class IncorrectServerFingerprint(
            val fingerprint: String,
        ) : Error()

        data class ServerNotReachable(
            val serverUrl: String,
        ) : Error()

        data object TimeIsOutOfSync : Error()

        data object Generic : Error()
    }
}
