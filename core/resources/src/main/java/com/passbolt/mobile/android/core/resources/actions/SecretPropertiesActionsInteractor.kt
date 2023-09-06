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

package com.passbolt.mobile.android.core.resources.actions

import androidx.annotation.VisibleForTesting
import com.passbolt.mobile.android.core.mvp.authentication.UnauthenticatedReason
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.DecryptedSecret
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretParser
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.ui.DecryptedSecretOrError
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.transform
import timber.log.Timber

class SecretPropertiesActionsInteractor(
    private val needSessionRefreshFlow: MutableStateFlow<UnauthenticatedReason?>,
    private val sessionRefreshedFlow: StateFlow<Unit?>,
    private val resource: ResourceModel,
    private val secretParser: SecretParser,
    private val secretInteractor: SecretInteractor
) {

    suspend fun provideDescription(): Flow<SecretPropertyActionResult<String>> =
        fetchAndDecrypt()
            .mapSuccess {
                when (val description = secretParser.extractDescription(resource.resourceTypeId, it.secret)) {
                    is DecryptedSecretOrError.DecryptedSecret ->
                        SecretPropertyActionResult.Success(
                            DESCRIPTION_LABEL,
                            isSecret = true,
                            description.secret
                        )

                    is DecryptedSecretOrError.Error ->
                        SecretPropertyActionResult.DecryptionFailure()
                }
            }

    suspend fun providePassword(): Flow<SecretPropertyActionResult<String>> =
        fetchAndDecrypt()
            .mapSuccess {
                when (val password = secretParser.extractPassword(resource.resourceTypeId, it.secret)) {
                    is DecryptedSecretOrError.DecryptedSecret ->
                        SecretPropertyActionResult.Success(
                            SECRET_LABEL,
                            isSecret = true,
                            password.secret
                        )
                    is DecryptedSecretOrError.Error ->
                        SecretPropertyActionResult.DecryptionFailure()
                }
            }

    suspend fun provideOtp(): Flow<SecretPropertyActionResult<DecryptedSecret.StandaloneTotp.Totp>> =
        fetchAndDecrypt()
            .mapSuccess {
                when (val totp = secretParser.extractTotpData(resource.resourceTypeId, it.secret)) {
                    is DecryptedSecretOrError.DecryptedSecret ->
                        SecretPropertyActionResult.Success(
                            OTP_LABEL,
                            isSecret = true,
                            totp.secret
                        )
                    is DecryptedSecretOrError.Error ->
                        SecretPropertyActionResult.DecryptionFailure()
                }
            }

    // this method extracts description from secret (cannot be used for "simple password" resource type)
    suspend fun providePasswordAndSecretDescription():
            Flow<SecretPropertyActionResult<DecryptedSecret.PasswordWithDescription>> =
        fetchAndDecrypt()
            .mapSuccess {
                val password = secretParser.extractPassword(resource.resourceTypeId, it.secret)
                val description = secretParser.extractDescription(resource.resourceTypeId, it.secret)

                if (password is DecryptedSecretOrError.DecryptedSecret &&
                    description is DecryptedSecretOrError.DecryptedSecret
                ) {
                    SecretPropertyActionResult.Success(
                        PASSWORD_AND_DESCRIPTION,
                        isSecret = true,
                        DecryptedSecret.PasswordWithDescription(
                            description = description.secret,
                            password = password.secret
                        )
                    )
                } else {
                    SecretPropertyActionResult.DecryptionFailure()
                }
            }

    private suspend fun fetchAndDecrypt(): Flow<SecretFetchAndDecryptResult> = flowOf(
        when (val output =
            runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                secretInteractor.fetchAndDecrypt(resource.resourceId)
            }
        ) {
            is SecretInteractor.Output.DecryptFailure ->
                SecretFetchAndDecryptResult.DecryptFailure
            is SecretInteractor.Output.FetchFailure ->
                SecretFetchAndDecryptResult.FetchFailure
            is SecretInteractor.Output.Success ->
                SecretFetchAndDecryptResult.Success(output.decryptedSecret)
            is SecretInteractor.Output.Unauthorized ->
                SecretFetchAndDecryptResult.Unauthorized
        }
    )

    private inline fun <T> Flow<SecretFetchAndDecryptResult>.mapSuccess(
        crossinline transform: suspend (value: SecretFetchAndDecryptResult.Success) -> SecretPropertyActionResult<T>
    ): Flow<SecretPropertyActionResult<T>> =
        transform {
            emit(
                when (it) {
                    is SecretFetchAndDecryptResult.DecryptFailure ->
                        SecretPropertyActionResult.DecryptionFailure()
                    is SecretFetchAndDecryptResult.FetchFailure ->
                        SecretPropertyActionResult.FetchFailure()
                    is SecretFetchAndDecryptResult.Unauthorized ->
                        SecretPropertyActionResult.Unauthorized()
                    is SecretFetchAndDecryptResult.Success -> {
                        transform(it)
                    }
                }
            )
        }

    private sealed class SecretFetchAndDecryptResult {

        object FetchFailure : SecretFetchAndDecryptResult()

        object DecryptFailure : SecretFetchAndDecryptResult()

        object Unauthorized : SecretFetchAndDecryptResult()

        class Success(val secret: ByteArray) : SecretFetchAndDecryptResult()
    }

    companion object {
        @VisibleForTesting
        const val SECRET_LABEL = "Secret"

        @VisibleForTesting
        const val DESCRIPTION_LABEL = "Description"

        @VisibleForTesting
        const val OTP_LABEL = "TOTP"

        private const val PASSWORD_AND_DESCRIPTION = "PasswordAndDescription"
    }
}

suspend fun <T> performSecretPropertyAction(
    action: suspend () -> Flow<SecretPropertyActionResult<T>>,
    doOnFetchFailure: () -> Unit,
    doOnDecryptionFailure: () -> Unit,
    doOnSuccess: (SecretPropertyActionResult.Success<T>) -> Unit
) {
    action().single().let {
        when (it) {
            is SecretPropertyActionResult.DecryptionFailure -> {
                doOnDecryptionFailure()
            }
            is SecretPropertyActionResult.FetchFailure -> {
                doOnFetchFailure()
            }
            is SecretPropertyActionResult.Success -> {
                doOnSuccess(it)
            }
            is SecretPropertyActionResult.Unauthorized -> {
                // can be ignored - runAuthenticatedOperation handles it
                Timber.d("Unauthorized during decrypting secret")
            }
        }
    }
}
