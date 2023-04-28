package com.passbolt.mobile.android.core.resources.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.networking.MfaTypeProvider
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.dto.request.CreateResourceDto
import com.passbolt.mobile.android.dto.request.EncryptedSecret
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.mappers.CreateResourceMapper
import com.passbolt.mobile.android.mappers.PermissionsModelMapper
import com.passbolt.mobile.android.mappers.ResourceModelMapper
import com.passbolt.mobile.android.passboltapi.resource.ResourceRepository
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.cache.passphrase.PotentialPassphrase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.privatekey.GetPrivateKeyUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.ui.EncryptedSecretOrError
import com.passbolt.mobile.android.ui.ResourceModelWithAttributes

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
class CreateStandaloneTotpResourceUseCase(
    private val resourceRepository: ResourceRepository,
    private val openPgp: OpenPgp,
    private val createResourceMapper: CreateResourceMapper,
    private val getPrivateKeyUseCase: GetPrivateKeyUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val resourceModelMapper: ResourceModelMapper,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val permissionsModelMapper: PermissionsModelMapper
) : AsyncUseCase<CreateStandaloneTotpResourceUseCase.Input, CreateStandaloneTotpResourceUseCase.Output> {

    override suspend fun execute(input: Input): Output {
        val passphrase = when (val result = passphraseMemoryCache.get()) {
            is PotentialPassphrase.Passphrase -> result.passphrase
            is PotentialPassphrase.PassphraseNotPresent -> return Output.PasswordExpired
        }

        return when (val secret =
            createSecret(input.period, input.digits, input.algorithm, input.secretKey, passphrase)) {
            is EncryptedSecretOrError.Error -> Output.OpenPgpError(secret.message)
            is EncryptedSecretOrError.EncryptedSecret -> {
                // from API documentation: An array of secrets in object format - exactly one secret must be provided.
                when (val response = resourceRepository.createResource(
                    CreateResourceDto(
                        name = input.label,
                        resourceTypeId = input.resourceTypeId,
                        secrets = listOf(EncryptedSecret(secret.userId, secret.data)),
                        uri = input.issuer,
                        username = null,
                        description = null,
                        folderParentId = null
                    )
                )) {
                    is NetworkResult.Failure -> Output.Failure(response)
                    is NetworkResult.Success -> Output.Success(
                        ResourceModelWithAttributes(
                            resourceModelMapper.map(response.value.body),
                            emptyList(), // cannot add tags during creation
                            listOf(permissionsModelMapper.mapToUserPermission(response.value.body.permission)),
                            response.value.body.favorite?.id
                        )
                    )
                }
            }
        }
    }

    private suspend fun createSecret(
        period: Long,
        digits: Int,
        algorithm: String,
        secretKey: String,
        passphrase: ByteArray
    ): EncryptedSecretOrError {
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        val privateKey = getPrivateKeyUseCase.execute(UserIdInput(userId)).privateKey
        val secret = createResourceMapper.map(digits, period, algorithm, secretKey)

        return when (val publicKey = openPgp.generatePublicKey(privateKey)) {
            is OpenPgpResult.Error ->
                EncryptedSecretOrError.Error(publicKey.error.message)
            is OpenPgpResult.Result -> {
                when (val encryptedSecret =
                    openPgp.encryptSignMessageArmored(publicKey.result, privateKey, passphrase, secret)) {
                    is OpenPgpResult.Error ->
                        EncryptedSecretOrError.Error(encryptedSecret.error.message)
                    is OpenPgpResult.Result ->
                        EncryptedSecretOrError.EncryptedSecret(
                            userId,
                            encryptedSecret.result
                        )
                }
            }
        }
    }

    sealed class Output : AuthenticatedUseCaseOutput {

        override val authenticationState: AuthenticationState
            get() = when {
                this is Failure<*> && this.response.isUnauthorized -> {
                    AuthenticationState.Unauthenticated(AuthenticationState.Unauthenticated.Reason.Session)
                }
                this is Failure<*> && this.response.isMfaRequired -> {
                    val providers = MfaTypeProvider.get(this.response)
                    AuthenticationState.Unauthenticated(
                        AuthenticationState.Unauthenticated.Reason.Mfa(providers)
                    )
                }
                this is PasswordExpired -> AuthenticationState.Unauthenticated(
                    AuthenticationState.Unauthenticated.Reason.Passphrase
                )
                else -> {
                    AuthenticationState.Authenticated
                }
            }

        data class Success(
            val resource: ResourceModelWithAttributes
        ) : Output()

        data class Failure<T : Any>(val response: NetworkResult.Failure<T>) : Output()

        object PasswordExpired : Output()

        data class OpenPgpError(val message: String) : Output()
    }

    data class Input(
        val resourceTypeId: String,
        val issuer: String?, // mapped to resource url
        val label: String, // mapped to resource name
        val period: Long,
        val digits: Int,
        val algorithm: String,
        val secretKey: String
    )
}
