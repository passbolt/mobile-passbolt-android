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

package com.passbolt.mobile.android.core.resources.interactor.create

import com.google.gson.Gson
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.networking.MfaTypeProvider
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetResourceTypeIdToSlugMappingUseCase
import com.passbolt.mobile.android.dto.request.CreateResourceDto
import com.passbolt.mobile.android.dto.request.EncryptedSecret
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.mappers.PermissionsModelMapper
import com.passbolt.mobile.android.mappers.ResourceModelMapper
import com.passbolt.mobile.android.passboltapi.resource.ResourceRepository
import com.passbolt.mobile.android.serializers.gson.validation.JsonSchemaValidationRunner
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity.RESOURCE
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity.SECRET
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.cache.passphrase.PotentialPassphrase
import com.passbolt.mobile.android.storage.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.privatekey.GetPrivateKeyUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.ui.EncryptedSecretOrError
import com.passbolt.mobile.android.ui.ResourceModelWithAttributes
import java.time.ZonedDateTime

abstract class CreateResourceInteractor<CustomInput>(
    private val resourceRepository: ResourceRepository,
    private val resourceModelMapper: ResourceModelMapper,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val permissionsModelMapper: PermissionsModelMapper,
    private val getResourceTypeIdToSlugMappingUseCase: GetResourceTypeIdToSlugMappingUseCase,
    private val jsonSchemaValidationRunner: JsonSchemaValidationRunner,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val getPrivateKeyUseCase: GetPrivateKeyUseCase,
    private val openPgp: OpenPgp,
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val gson: Gson
) {

    abstract val slug: String

    suspend fun execute(input: CommonInput, customInput: CustomInput): Output {
        val passphrase = when (val result = passphraseMemoryCache.get()) {
            is PotentialPassphrase.Passphrase -> result.passphrase
            is PotentialPassphrase.PassphraseNotPresent -> return Output.PasswordExpired
        }

        val secret = createPlainSecret(input, customInput, passphrase)
        return if (isSecretValid(secret)) {
            when (val encryptedSecret = encrypt(secret, passphrase)) {
                is EncryptedSecretOrError.Error -> Output.OpenPgpError(encryptedSecret.message)
                is EncryptedSecretOrError.EncryptedSecret -> {
                    createResource(input, encryptedSecret, customInput)
                }
            }
        } else {
            Output.JsonSchemaValidationFailure(SECRET)
        }
    }

    private suspend fun createResource(
        input: CommonInput,
        encryptedSecret: EncryptedSecretOrError.EncryptedSecret,
        customInput: CustomInput
    ): Output {
        val createResourceDto = CreateResourceDto(
            name = input.resourceName,
            resourceTypeId = getResourceTypeIdForSlug(slug),
            // from API documentation: An array of secrets in object format - exactly one secret must be provided.
            secrets = listOf(EncryptedSecret(encryptedSecret.userId, encryptedSecret.data)),
            username = input.resourceUsername,
            uri = input.resourceUri,
            description = createCommonDescription(customInput),
            folderParentId = input.resourceParentFolderId,
            expiry = getResourceExpiry()
        )
        return if (isResourceValid(createResourceDto)) {
            when (val response = resourceRepository.createResource(createResourceDto)) {
                is NetworkResult.Failure -> Output.Failure(response)
                is NetworkResult.Success -> Output.Success(
                    ResourceModelWithAttributes(
                        resourceModelMapper.map(response.value.body),
                        emptyList(), // cannot add tags during creation
                        listOf(permissionsModelMapper.mapToUserPermission(response.value.body.permission)),
                        response.value.body.favorite?.id?.toString()
                    )
                )
            }
        } else {
            Output.JsonSchemaValidationFailure(RESOURCE)
        }
    }

    abstract suspend fun getResourceExpiry(): ZonedDateTime?

    private suspend fun encrypt(secret: String, passphrase: ByteArray): EncryptedSecretOrError {
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        val userServerId = requireNotNull(getSelectedAccountDataUseCase.execute(Unit).serverId)
        val privateKey = getPrivateKeyUseCase.execute(UserIdInput(userId)).privateKey

        return when (val publicKey = openPgp.generatePublicKey(privateKey)) {
            is OpenPgpResult.Error -> EncryptedSecretOrError.Error(publicKey.error.message)
            is OpenPgpResult.Result -> {
                when (val encryptedSecret =
                    openPgp.encryptSignMessageArmored(publicKey.result, privateKey, passphrase, secret)) {
                    is OpenPgpResult.Error -> EncryptedSecretOrError.Error(encryptedSecret.error.message)
                    is OpenPgpResult.Result -> EncryptedSecretOrError.EncryptedSecret(
                        userServerId,
                        encryptedSecret.result
                    )
                }
            }
        }
    }

    private suspend fun isSecretValid(plainSecretJson: String) =
        jsonSchemaValidationRunner.isSecretValid(plainSecretJson, slug)

    private suspend fun isResourceValid(createResourceDto: CreateResourceDto) =
        jsonSchemaValidationRunner.isResourceValid(gson.toJson(createResourceDto), slug)

    private suspend fun getResourceTypeIdForSlug(slug: String) =
        getResourceTypeIdToSlugMappingUseCase.execute(Unit)
            .idToSlugMapping
            .filterValues { it == slug }
            .keys
            .first()
            .toString()

    abstract fun createPlainSecret(
        input: CommonInput,
        customInput: CustomInput,
        passphrase: ByteArray
    ): String

    abstract suspend fun createCommonDescription(
        customInput: CustomInput
    ): String?

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

        data class Success(val resource: ResourceModelWithAttributes) : Output()

        data class Failure<T : Any>(val response: NetworkResult.Failure<T>) : Output()

        data object PasswordExpired : Output()

        data class OpenPgpError(val message: String) : Output()

        data class JsonSchemaValidationFailure(val entity: SchemaEntity) : Output()
    }

    data class CommonInput(
        val resourceName: String,
        val resourceUsername: String?,
        val resourceUri: String?,
        val resourceParentFolderId: String?
    )
}
