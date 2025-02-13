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

import com.passbolt.mobile.android.common.extension.stripPGPHeaders
import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.privatekey.GetPrivateKeyUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.networking.MfaTypeProvider
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.passphrasememorycache.PotentialPassphrase
import com.passbolt.mobile.android.core.policies.usecase.GetPasswordExpirySettingsUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetResourceTypeIdToSlugMappingUseCase
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretModel
import com.passbolt.mobile.android.dto.request.CreateV4ResourceDto
import com.passbolt.mobile.android.dto.request.CreateV5ResourceDto
import com.passbolt.mobile.android.dto.request.EncryptedSecret
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.mappers.MetadataMapper
import com.passbolt.mobile.android.mappers.PermissionsModelMapper
import com.passbolt.mobile.android.mappers.ResourceModelMapper
import com.passbolt.mobile.android.passboltapi.resource.ResourceRepository
import com.passbolt.mobile.android.serializers.gson.MetadataEncryptor
import com.passbolt.mobile.android.serializers.gson.validation.JsonSchemaValidationRunner
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity.RESOURCE
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity.SECRET
import com.passbolt.mobile.android.serializers.validationwrapper.PlainSecretValidationWrapper
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes
import com.passbolt.mobile.android.ui.CreateResourceModel
import com.passbolt.mobile.android.ui.EncryptedSecretOrError
import com.passbolt.mobile.android.ui.ResourceModelWithAttributes
import org.koin.core.component.KoinComponent
import java.time.ZonedDateTime

class CreateResourceInteractor(
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
    private val passwordExpirySettingsUseCase: GetPasswordExpirySettingsUseCase,
    private val metadataMapper: MetadataMapper,
    private val metadataEncryptor: MetadataEncryptor
) : KoinComponent {

    suspend fun execute(resourceInput: CreateResourceModel, secretInput: SecretModel): Output {
        val passphrase = when (val result = passphraseMemoryCache.get()) {
            is PotentialPassphrase.Passphrase -> result.passphrase
            is PotentialPassphrase.PassphraseNotPresent -> return Output.PasswordExpired
        }

        val isSecretValid = isSecretValid(
            PlainSecretValidationWrapper(secretInput.json, resourceInput.contentType)
                .validationPlainSecret,
            resourceInput.contentType
        )
        val isResourceValid = isResourceValid(resourceInput.json, resourceInput.contentType)

        if (resourceInput.contentType.slug in SupportedContentTypes.v5Slugs) {
            secretInput.apply {
                this.objectType = "PASSBOLT_SECRET_V5"
                this.resourceTypeId = getResourceTypeIdForSlug(resourceInput.contentType.slug)
            }
        }

        return if (isSecretValid && isResourceValid) {
            when (val encryptedSecret = encryptSecret(secretInput.json, passphrase)) {
                is EncryptedSecretOrError.Error -> Output.OpenPgpError(encryptedSecret.message)
                is EncryptedSecretOrError.EncryptedSecret -> {
                    createResource(resourceInput, encryptedSecret, passphrase)
                }
            }
        } else {
            if (!isSecretValid) {
                Output.JsonSchemaValidationFailure(SECRET)
            } else {
                Output.JsonSchemaValidationFailure(RESOURCE)
            }
        }
    }

    private suspend fun createResource(
        resourceInput: CreateResourceModel,
        encryptedSecret: EncryptedSecretOrError.EncryptedSecret,
        passphrase: ByteArray
    ): Output {
        val createResourceDto = if (SupportedContentTypes.v4Slugs.contains(resourceInput.contentType.slug)) {
            CreateV4ResourceDto(
                name = resourceInput.name,
                resourceTypeId = getResourceTypeIdForSlug(resourceInput.contentType.slug),
                // from API documentation: An array of secrets in object format - exactly one secret must be provided.
                secrets = listOf(EncryptedSecret(encryptedSecret.userId, encryptedSecret.data)),
                username = resourceInput.username,
                uri = resourceInput.uri,
                description = resourceInput.description,
                folderParentId = resourceInput.folderId,
                expiry = getResourceExpiry(resourceInput.contentType)
            )
        } else {
            resourceInput.apply {
                this.objectType = "PASSBOLT_RESOURCE_METADATA"
                this.resourceTypeId = getResourceTypeIdForSlug(contentType.slug)
            }

            val encryptedMetadata = metadataEncryptor.encryptMetadata(
                resourceInput.metadataKeyType!!,
                resourceInput.metadataKeyId!!,
                resourceInput.json,
                passphrase
            )
            when (encryptedMetadata) {
                is MetadataEncryptor.Output.Success -> CreateV5ResourceDto(
                    resourceTypeId = getResourceTypeIdForSlug(resourceInput.contentType.slug),
                    // from API documentation: An array of secrets in object format - exactly one secret must be provided.
                    secrets = listOf(EncryptedSecret(encryptedSecret.userId, encryptedSecret.data)),
                    folderParentId = resourceInput.folderId,
                    expiry = getResourceExpiry(resourceInput.contentType),
                    // FIXME temporary solution to satisfy web-extension validation
                    metadata = encryptedMetadata.encryptedMetadata.stripPGPHeaders(),
                    metadataKeyId = resourceInput.metadataKeyId,
                    metadataKeyType = metadataMapper.mapToDto(resourceInput.metadataKeyType)
                )
                is MetadataEncryptor.Output.Failure -> return Output.OpenPgpError(encryptedMetadata.error?.message)
            }
        }

        return when (val response = resourceRepository.createResource(createResourceDto)) {
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
    }

    @Suppress("NestedBlockDepth")
    // https://drive.google.com/file/d/1lqiF0ajpuvx1xaZ74aSSjxiDLMGPBXVa/view?usp=drive_link
    private suspend fun getResourceExpiry(contentType: ContentType): ZonedDateTime? {
        return if (SupportedContentTypes.resourcesSlugsSupportingExpiry.contains(contentType)) {
            val expirySettings = passwordExpirySettingsUseCase.execute(Unit).expirySettings
            if (expirySettings.automaticUpdate) {
                if (expirySettings.defaultExpiryPeriodDays != null) {
                    ZonedDateTime.now()
                        .plusDays(expirySettings.defaultExpiryPeriodDays!!.toLong())
                        .withFixedOffsetZone()
                } else {
                    null
                }
            } else {
                null
            }
        } else {
            null
        }
    }

    private suspend fun encryptSecret(secret: String, passphrase: ByteArray): EncryptedSecretOrError {
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        val userServerId = requireNotNull(getSelectedAccountDataUseCase.execute(Unit).serverId)
        val privateKey = getPrivateKeyUseCase.execute(UserIdInput(userId)).privateKey

        return when (val encryptedSecret =
            openPgp.encryptSignMessageArmored(privateKey, passphrase, secret)) {
            is OpenPgpResult.Error -> EncryptedSecretOrError.Error(encryptedSecret.error.message)
            is OpenPgpResult.Result -> EncryptedSecretOrError.EncryptedSecret(
                userServerId,
                encryptedSecret.result
            )
        }
    }

    private suspend fun isSecretValid(plainSecretJson: String, type: ContentType) =
        jsonSchemaValidationRunner.isSecretValid(plainSecretJson, type.slug)

    private suspend fun isResourceValid(plainResourceMetadataJson: String, type: ContentType) =
        jsonSchemaValidationRunner.isResourceValid(plainResourceMetadataJson, type.slug)

    private suspend fun getResourceTypeIdForSlug(slug: String) =
        getResourceTypeIdToSlugMappingUseCase.execute(Unit)
            .idToSlugMapping
            .filterValues { it == slug }
            .keys
            .first()
            .toString()

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

        data class OpenPgpError(val message: String?) : Output()

        data class JsonSchemaValidationFailure(val entity: SchemaEntity) : Output()
    }
}
