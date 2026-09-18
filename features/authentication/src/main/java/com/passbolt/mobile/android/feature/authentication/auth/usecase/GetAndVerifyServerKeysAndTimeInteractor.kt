package com.passbolt.mobile.android.feature.authentication.auth.usecase

import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.architecture.result.DomainResult.Incomplete.Error.Reason.OFFLINE
import com.passbolt.mobile.android.core.architecture.result.DomainResult.Incomplete.Error.Reason.TIMEOUT
import com.passbolt.mobile.android.domain.accounts.usecase.GetAccountDataUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.IsServerFingerprintCorrectUseCase
import com.passbolt.mobile.android.domain.auth.usecase.FetchServerPublicPgpKeyUseCase
import com.passbolt.mobile.android.domain.auth.usecase.FetchServerPublicRsaKeyUseCase
import com.passbolt.mobile.android.domain.auth.usecase.SaveServerPublicRsaKeyUseCase
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
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
class GetAndVerifyServerKeysAndTimeInteractor(
    private val serverKeysWarmup: ServerKeysWarmup,
    private val saveServerPublicRsaKeyUseCase: SaveServerPublicRsaKeyUseCase,
    private val isServerFingerprintCorrectUseCase: IsServerFingerprintCorrectUseCase,
    private val getAccountDataUseCase: GetAccountDataUseCase,
    private val gopenPgpTimeUpdater: GopenPgpTimeUpdater,
    private val openPgp: OpenPgp,
) {
    suspend fun getAndVerifyServerKeys(
        userId: String,
        onError: (Error) -> Unit,
        onSuccess: suspend (Success) -> Unit,
    ) {
        Timber.d("Getting server pgp and rsa keys")
        val serverKeys = serverKeysWarmup.fetchOrAwait(userId)
        val (pgpKey, getTimeRequestDuration) = serverKeys.timedPgp
        val rsaKey = serverKeys.rsa

        if (pgpKey !is FetchServerPublicPgpKeyUseCase.Output.Success ||
            rsaKey !is FetchServerPublicRsaKeyUseCase.Output.Success
        ) {
            onError(mapKeysFetchFailure(userId, pgpKey, rsaKey))
            return
        }

        if (isServerTimeOutOfSync(pgpKey, serverKeys.deviceTimeAtFetchMillis, getTimeRequestDuration.inWholeMilliseconds)) {
            onError(Error.TimeIsOutOfSync)
            return
        }

        verifyServerFingerprint(userId, pgpKey, rsaKey, onError, onSuccess)
    }

    private suspend fun verifyServerFingerprint(
        userId: String,
        pgpKey: FetchServerPublicPgpKeyUseCase.Output.Success,
        rsaKey: FetchServerPublicRsaKeyUseCase.Output.Success,
        onError: (Error) -> Unit,
        onSuccess: suspend (Success) -> Unit,
    ) {
        val computedFingerprint =
            computeServerKeyFingerprint(pgpKey.publicKey) ?: run {
                onError(Error.Generic)
                return
            }

        val rejection = calculateFingerprintRejectionReason(userId, computedFingerprint, pgpKey.fingerprint)
        if (rejection != null) {
            Timber.e(rejection.logMessage)
            onError(Error.IncorrectServerFingerprint(computedFingerprint))
        } else {
            Timber.d("Server key fingerprint is valid")
            saveServerPublicRsaKeyUseCase.execute(SaveServerPublicRsaKeyUseCase.Input(userId, rsaKey.rsaKey))
            onSuccess(Success(pgpKey.publicKey, computedFingerprint, rsaKey.rsaKey))
        }
    }

    private fun isServerTimeOutOfSync(
        pgpKey: FetchServerPublicPgpKeyUseCase.Output.Success,
        deviceTimeAtFetchMillis: Long,
        getTimeRequestDurationMillis: Long,
    ): Boolean =
        gopenPgpTimeUpdater.updateTimeIfNeeded(
            pgpKey.serverTime,
            deviceTimeAtFetchMillis,
            getTimeRequestDurationMillis,
        ) == GopenPgpTimeUpdater.Result.TIME_DELTA_TOO_BIG_FOR_SYNC

    private suspend fun computeServerKeyFingerprint(publicKey: String): String? =
        when (val result = openPgp.getKeyFingerprint(publicKey)) {
            is OpenPgpResult.Result -> result.result.uppercase()
            is OpenPgpResult.Error -> {
                Timber.e("Unable to compute server key fingerprint from key data: ${result.error.message}")
                null
            }
        }

    private fun isServerFingerprintTrusted(
        userId: String,
        fingerprint: String,
    ): Boolean = isServerFingerprintCorrectUseCase.execute(IsServerFingerprintCorrectUseCase.Input(userId, fingerprint)).isCorrect

    private fun calculateFingerprintRejectionReason(
        userId: String,
        computedFingerprint: String,
        reportedFingerprint: String,
    ): FingerprintRejection? {
        if (!computedFingerprint.equals(reportedFingerprint, ignoreCase = true)) {
            return FingerprintRejection.FINGERPRINT_NOT_MATCHING_KEY
        }
        return if (isServerFingerprintTrusted(userId, computedFingerprint)) {
            null
        } else {
            FingerprintRejection.FINGERPRINT_CHANGED
        }
    }

    private fun mapKeysFetchFailure(
        userId: String,
        pgpKey: FetchServerPublicPgpKeyUseCase.Output,
        rsaKey: FetchServerPublicRsaKeyUseCase.Output,
    ): Error {
        val pgpIncomplete = (pgpKey as? FetchServerPublicPgpKeyUseCase.Output.Failure)?.incomplete
        val rsaIncomplete = (rsaKey as? FetchServerPublicRsaKeyUseCase.Output.Failure)?.incomplete
        return when {
            pgpIncomplete.isNoNetwork() || rsaIncomplete.isNoNetwork() -> {
                Timber.d("No network connection")
                Error.NoNetwork
            }
            pgpIncomplete.isServerNotReachable() || rsaIncomplete.isServerNotReachable() -> {
                Timber.d("Server is not reachable")
                Error.ServerNotReachable(getAccountDataUseCase.execute(UserIdInput(userId)).url)
            }
            else -> {
                Timber.d("Generic error occurred")
                Error.Generic
            }
        }
    }

    private enum class FingerprintRejection(
        val logMessage: String,
    ) {
        FINGERPRINT_NOT_MATCHING_KEY("Server-reported fingerprint does not match its key data"),
        FINGERPRINT_CHANGED("Server key fingerprint changed from the saved value"),
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

        data object NoNetwork : Error()

        data object TimeIsOutOfSync : Error()

        data object Generic : Error()
    }
}

private fun DomainResult.Incomplete?.isServerNotReachable(): Boolean = this is DomainResult.Incomplete.Error && reason == TIMEOUT

private fun DomainResult.Incomplete?.isNoNetwork(): Boolean = this is DomainResult.Incomplete.Error && reason == OFFLINE
