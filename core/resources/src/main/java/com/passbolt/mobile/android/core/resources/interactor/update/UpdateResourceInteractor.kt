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

package com.passbolt.mobile.android.core.resources.interactor.update

import com.google.gson.Gson
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.networking.MfaTypeProvider
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetResourceTypeIdToSlugMappingUseCase
import com.passbolt.mobile.android.core.users.usecase.FetchUsersUseCase
import com.passbolt.mobile.android.dto.request.CreateResourceDto
import com.passbolt.mobile.android.dto.request.EncryptedSecret
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.mappers.ResourceModelMapper
import com.passbolt.mobile.android.passboltapi.resource.ResourceRepository
import com.passbolt.mobile.android.serializers.gson.validation.JsonSchemaValidationRunner
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity.RESOURCE
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity.SECRET
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.cache.passphrase.PotentialPassphrase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.privatekey.GetPrivateKeyUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.ui.EncryptedSecretOrError
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.UserModel

abstract class UpdateResourceInteractor<CustomInput>(
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val resourceModelMapper: ResourceModelMapper,
    private val resourceRepository: ResourceRepository,
    private val fetchUsersUseCase: FetchUsersUseCase,
    private val getResourceTypeIdToSlugMappingUseCase: GetResourceTypeIdToSlugMappingUseCase,
    private val jsonSchemaValidationRunner: JsonSchemaValidationRunner,
    private val gson: Gson,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val getPrivateKeyUseCase: GetPrivateKeyUseCase,
    private val openPgp: OpenPgp
) {

    abstract val slug: String

    suspend fun execute(input: CommonInput, customInput: CustomInput): Output {
        val passphrase = when (val result = passphraseMemoryCache.get()) {
            is PotentialPassphrase.Passphrase -> result.passphrase
            is PotentialPassphrase.PassphraseNotPresent -> return Output.PasswordExpired
        }

        return when (val usersWhoHaveAccess =
            fetchUsersUseCase.execute(FetchUsersUseCase.Input(listOf(input.resourceId)))) {
            is FetchUsersUseCase.Output.Failure<*> -> Output.Failure(usersWhoHaveAccess.response)
            is FetchUsersUseCase.Output.Success -> {
                val plainSecret = createSecret(input, customInput, passphrase)
                if (isSecretValid(PlainSecretValidationWrapper(plainSecret, slug).validationPlainSecret)) {
                    createResource(plainSecret, passphrase, usersWhoHaveAccess, input, customInput)
                } else {
                    Output.JsonSchemaValidationFailure(SECRET)
                }
            }
        }
    }

    private suspend fun createResource(
        plainSecret: String,
        passphrase: ByteArray,
        usersWhoHaveAccess: FetchUsersUseCase.Output.Success,
        input: CommonInput,
        customInput: CustomInput
    ): Output {
        val encryptedSecrets = encrypt(plainSecret, passphrase, usersWhoHaveAccess.users)
        return if (encryptedSecrets.any { it is EncryptedSecretOrError.Error }) {
            Output.OpenPgpError(
                encryptedSecrets.filterIsInstance<EncryptedSecretOrError.Error>().first().message
            )
        } else {
            val secrets = encryptedSecrets.filterIsInstance<EncryptedSecretOrError.EncryptedSecret>()
            val createResourceDto = CreateResourceDto(
                name = input.resourceName,
                resourceTypeId = getResourceTypeIdForSlug(slug),
                secrets = secrets.map { EncryptedSecret(it.userId, it.data) },
                username = input.resourceUsername,
                uri = input.resourceUri,
                description = createCommonDescription(customInput),
                folderParentId = input.resourceParentFolderId
            )
            if (isResourceValid(createResourceDto)) {
                when (val response = resourceRepository.updateResource(
                    input.resourceId,
                    createResourceDto
                )) {
                    is NetworkResult.Failure -> Output.Failure(response)
                    is NetworkResult.Success -> Output.Success(resourceModelMapper.map(response.value.body))
                }
            } else {
                Output.JsonSchemaValidationFailure(RESOURCE)
            }
        }
    }

    private suspend fun encrypt(
        plainSecret: String,
        passphrase: ByteArray,
        usersWhoHaveAccess: List<UserModel>
    ): List<EncryptedSecretOrError> =
        usersWhoHaveAccess.mapTo(mutableListOf()) {
            val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
            val privateKey = getPrivateKeyUseCase.execute(UserIdInput(userId)).privateKey
            val publicKey = it.gpgKey.armoredKey

            when (val encryptedSecret =
                openPgp.encryptSignMessageArmored(publicKey, privateKey, passphrase, plainSecret)) {
                is OpenPgpResult.Error -> EncryptedSecretOrError.Error(encryptedSecret.error.message)
                is OpenPgpResult.Result -> EncryptedSecretOrError.EncryptedSecret(it.id, encryptedSecret.result)
            }
        }

    private suspend fun isSecretValid(plainSecret: String) =
        jsonSchemaValidationRunner.isSecretValid(plainSecret, slug)

    private suspend fun isResourceValid(createResourceDto: CreateResourceDto) =
        jsonSchemaValidationRunner.isResourceValid(gson.toJson(createResourceDto), slug)

    private suspend fun getResourceTypeIdForSlug(slug: String) =
        getResourceTypeIdToSlugMappingUseCase.execute(Unit)
            .idToSlugMapping
            .filterValues { it == slug }
            .keys
            .first()
            .toString()

    abstract fun createSecret(
        input: CommonInput,
        customInput: CustomInput,
        passphrase: ByteArray
    ): String

    abstract suspend fun createCommonDescription(
        customInput: CustomInput
    ): String?

    data class CommonInput(
        val resourceId: String,
        val resourceName: String,
        val resourceUsername: String?,
        val resourceUri: String?,
        val resourceParentFolderId: String?
    )

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

        data object PasswordExpired : Output()

        data class OpenPgpError(val message: String) : Output()

        data class JsonSchemaValidationFailure(val entity: SchemaEntity) : Output()
    }
}
