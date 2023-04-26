package com.passbolt.mobile.android.core.resources.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.networking.MfaTypeProvider
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.core.resources.SecretInputCreator
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory
import com.passbolt.mobile.android.dto.request.CreateResourceDto
import com.passbolt.mobile.android.dto.request.EncryptedSecret
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.mappers.ResourceModelMapper
import com.passbolt.mobile.android.passboltapi.resource.ResourceRepository
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.cache.passphrase.PotentialPassphrase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.privatekey.GetPrivateKeyUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.ui.EncryptedSecretOrError
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.UserModel

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
class UpdateResourceUseCase(
    private val resourceRepository: ResourceRepository,
    private val openPgp: OpenPgp,
    private val getPrivateKeyUseCase: GetPrivateKeyUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val resourceModelMapper: ResourceModelMapper,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val secretInputCreator: SecretInputCreator,
    private val resourceTypeFactory: ResourceTypeFactory
) : AsyncUseCase<UpdateResourceUseCase.Input, UpdateResourceUseCase.Output> {

    override suspend fun execute(input: Input): Output {
        val passphrase = when (val result = passphraseMemoryCache.get()) {
            is PotentialPassphrase.Passphrase -> {
                result.passphrase
            }
            is PotentialPassphrase.PassphraseNotPresent -> return Output.PasswordExpired
        }
        val secretsResult = createSecrets(input, passphrase)
        return if (secretsResult.any { it is EncryptedSecretOrError.Error }) {
            Output.OpenPgpError(secretsResult.filterIsInstance<EncryptedSecretOrError.Error>().first().message)
        } else {
            val secrets = secretsResult.filterIsInstance<EncryptedSecretOrError.EncryptedSecret>()
            when (val response = resourceRepository.updateResource(
                input.resourceId,
                CreateResourceDto(
                    name = input.name,
                    resourceTypeId = input.resourceTypeId,
                    secrets = secrets.map { EncryptedSecret(it.userId, it.data) },
                    username = input.username,
                    uri = input.uri,
                    description = createDescription(input),
                    folderParentId = input.resourceParentFolderId
                )
            )) {
                is NetworkResult.Failure -> Output.Failure(response)
                is NetworkResult.Success -> Output.Success(resourceModelMapper.map(response.value.body))
            }
        }
    }

    private suspend fun createDescription(input: Input): String? =
        when (resourceTypeFactory.getResourceTypeEnum(input.resourceTypeId)) {
            ResourceTypeFactory.ResourceTypeEnum.SIMPLE_PASSWORD -> input.description
            ResourceTypeFactory.ResourceTypeEnum.PASSWORD_WITH_DESCRIPTION -> null
        }

    private suspend fun createSecrets(
        input: Input,
        passphrase: ByteArray
    ): List<EncryptedSecretOrError> {
        return input.users.mapTo(mutableListOf()) {
            val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
            val privateKey = getPrivateKeyUseCase.execute(UserIdInput(userId)).privateKey
            val publicKey = it.gpgKey.armoredKey
            val secret = secretInputCreator.createSecretInput(input.resourceTypeId, input.password, input.description)
            when (val encryptedSecret = openPgp.encryptSignMessageArmored(publicKey, privateKey, passphrase, secret)) {
                is OpenPgpResult.Error -> EncryptedSecretOrError.Error(encryptedSecret.error.message)
                is OpenPgpResult.Result -> EncryptedSecretOrError.EncryptedSecret(it.id, encryptedSecret.result)
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
            val resource: ResourceModel
        ) : Output()

        data class Failure<T : Any>(val response: NetworkResult.Failure<T>) : Output()

        object PasswordExpired : Output()

        data class OpenPgpError(val message: String) : Output()
    }

    data class Input(
        val resourceId: String,
        val resourceTypeId: String,
        val name: String,
        val password: String,
        val description: String?,
        val username: String?,
        val uri: String?,
        val users: List<UserModel>,
        val resourceParentFolderId: String?
    )
}
