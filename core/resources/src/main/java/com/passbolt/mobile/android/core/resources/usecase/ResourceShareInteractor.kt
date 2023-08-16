package com.passbolt.mobile.android.core.resources.usecase

import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.mvp.authentication.UnauthenticatedReason
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.core.users.usecase.db.GetLocalUserUseCase
import com.passbolt.mobile.android.dto.response.ShareRecipientDto
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.mappers.SharePermissionsModelMapper
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.cache.passphrase.PotentialPassphrase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.privatekey.GetPrivateKeyUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.ui.EncryptedSecretOrError
import com.passbolt.mobile.android.ui.PermissionModelUi
import retrofit2.HttpException
import timber.log.Timber
import java.net.HttpURLConnection

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
class ResourceShareInteractor(
    private val getLocalResourcePermissionsUseCase: GetLocalResourcePermissionsUseCase,
    private val getLocalUserUseCase: GetLocalUserUseCase,
    private val simulateShareUseCase: SimulateShareResourceUseCase,
    private val shareResourceUseCase: ShareResourceUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val getPrivateKeyUseCase: GetPrivateKeyUseCase,
    private val secretInteractor: SecretInteractor,
    private val openPgp: OpenPgp,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val sharePermissionsModelMapper: SharePermissionsModelMapper
) {

    suspend fun simulateAndShareResource(resourceId: String, recipients: List<PermissionModelUi>): Output {
        val existingResourcePermissions = getLocalResourcePermissionsUseCase
            .execute(GetLocalResourcePermissionsUseCase.Input(resourceId))
            .permissions

        val simulateSharePermissions = sharePermissionsModelMapper
            .mapForSimulation(
                SharePermissionsModelMapper.ShareItem.Resource(resourceId),
                recipients,
                existingResourcePermissions
            )

        Timber.d("Starting share simulation")
        return when (val simulateShareOutput = simulateShareUseCase.execute(
            SimulateShareResourceUseCase.Input(resourceId, simulateSharePermissions)
        )) {
            is SimulateShareResourceUseCase.Output.Success -> {
                Timber.d("Share simulation success; Starting to share resource")
                shareResource(resourceId, recipients, existingResourcePermissions, simulateShareOutput.value.added)
            }
            is SimulateShareResourceUseCase.Output.Failure<*> -> {
                Timber.e(
                    simulateShareOutput.response.exception,
                    "Share simulation failure: %s",
                    simulateShareOutput.response.headerMessage
                )
                Output.SimulateShareFailure(simulateShareOutput.response.exception)
            }
        }
    }

    private suspend fun shareResource(
        resourceId: String,
        recipients: List<PermissionModelUi>,
        existingPermissions: List<PermissionModelUi>,
        newUsers: List<ShareRecipientDto>
    ): Output {
        return when (val secretOutput = secretInteractor.fetchAndDecrypt(resourceId)) {
            is SecretInteractor.Output.DecryptFailure -> {
                Timber.e("Secret decrypt failure: %s", secretOutput.error.message)
                Output.SecretDecryptFailure(secretOutput.error.message)
            }
            is SecretInteractor.Output.FetchFailure -> {
                Timber.e(secretOutput.exception, "Secret fetch failure")
                Output.SecretFetchFailure(secretOutput.exception)
            }
            is SecretInteractor.Output.Unauthorized -> {
                Timber.d("Unauthorized during secret fetch")
                Output.Unauthorized(secretOutput.reason)
            }
            is SecretInteractor.Output.Success -> {
                Timber.d("Secret fetched")
                val passphrase = passphraseMemoryCache.get()
                if (passphrase is PotentialPassphrase.Passphrase) {
                    Timber.d("Using passphrase from cache")
                    val sharePermissions = sharePermissionsModelMapper
                        .mapForShare(
                            SharePermissionsModelMapper.ShareItem.Resource(resourceId),
                            recipients,
                            existingPermissions
                        )
                    val secretsData = prepareEncryptedSecretsData(
                        passphrase.passphrase, secretOutput.decryptedSecret, newUsers
                    )
                    if (secretsData.any { it is EncryptedSecretOrError.Error }) {
                        return Output.SecretEncryptFailure(
                            secretsData.filterIsInstance<EncryptedSecretOrError.Error>().first().message
                        )
                    }
                    val secrets = secretsData.filterIsInstance<EncryptedSecretOrError.EncryptedSecret>()
                    Timber.d("Executing share request")
                    when (val shareOutput = shareResourceUseCase.execute(
                        ShareResourceUseCase.Input(resourceId, sharePermissions, secrets)
                    )) {
                        is ShareResourceUseCase.Output.Failure<*> -> {
                            Timber.e(
                                shareOutput.response.exception,
                                "Share resource failure: %s",
                                shareOutput.response.headerMessage
                            )
                            Output.ShareFailure(shareOutput.response.exception)
                        }
                        is ShareResourceUseCase.Output.Success -> {
                            Timber.d("Share request success")
                            Output.Success
                        }
                    }
                } else {
                    Timber.d("Passphrase not in cache")
                    Output.Unauthorized(AuthenticationState.Unauthenticated.Reason.Passphrase)
                }
            }
        }
    }

    private suspend fun prepareEncryptedSecretsData(
        passphrase: ByteArray,
        decryptedSecret: ByteArray,
        addedUsers: List<ShareRecipientDto>
    ): List<EncryptedSecretOrError> {
        val encryptedSecretsForAddedUsers = mutableListOf<EncryptedSecretOrError>()
        addedUsers
            .map { getLocalUserUseCase.execute(GetLocalUserUseCase.Input(it.user.id.toString())).user }
            .forEach { user ->
                val currentUserId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
                val privateKey = getPrivateKeyUseCase.execute(UserIdInput(currentUserId)).privateKey
                val publicKey = user.gpgKey.armoredKey

                val encryptedSecret = openPgp.encryptSignMessageArmored(
                    publicKey,
                    privateKey,
                    passphrase,
                    String(decryptedSecret)
                )

                encryptedSecretsForAddedUsers.add(
                    when (encryptedSecret) {
                        is OpenPgpResult.Error -> EncryptedSecretOrError.Error(encryptedSecret.error.message)
                        is OpenPgpResult.Result -> EncryptedSecretOrError.EncryptedSecret(
                            user.id,
                            encryptedSecret.result
                        )
                    }
                )
            }
        return encryptedSecretsForAddedUsers
    }

    sealed class Output : AuthenticatedUseCaseOutput {

        @Suppress("ComplexCondition")
        override val authenticationState: AuthenticationState
            get() = if (
                (this is SecretFetchFailure &&
                        (this.exception as? HttpException)?.code() == HttpURLConnection.HTTP_UNAUTHORIZED) ||
                (this is ShareFailure &&
                        (this.exception as? HttpException)?.code() == HttpURLConnection.HTTP_UNAUTHORIZED) ||
                (this is SimulateShareFailure &&
                        (this.exception as? HttpException)?.code() == HttpURLConnection.HTTP_UNAUTHORIZED)
            ) {
                AuthenticationState.Unauthenticated(AuthenticationState.Unauthenticated.Reason.Session)
            } else if (this is Unauthorized) {
                AuthenticationState.Unauthenticated(this.reason)
            } else {
                AuthenticationState.Authenticated
            }

        data class SecretFetchFailure(val exception: Exception) : Output()

        data class SecretDecryptFailure(val message: String) : Output()

        data class SecretEncryptFailure(val message: String) : Output()

        data class ShareFailure(val exception: Exception) : Output()

        data class SimulateShareFailure(val exception: Exception) : Output()

        class Unauthorized(val reason: UnauthenticatedReason) : Output()

        object Success : Output()
    }
}
