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
import com.passbolt.mobile.android.common.usecase.UserIdInput
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
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.SecretInput
import com.passbolt.mobile.android.core.users.usecase.FetchUsersUseCase
import com.passbolt.mobile.android.dto.request.CreateResourceDto
import com.passbolt.mobile.android.dto.request.CreateV4ResourceDto
import com.passbolt.mobile.android.dto.request.EncryptedSecret
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.mappers.ResourceModelMapper
import com.passbolt.mobile.android.passboltapi.resource.ResourceRepository
import com.passbolt.mobile.android.serializers.gson.validation.JsonSchemaValidationRunner
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity.RESOURCE
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity.SECRET
import com.passbolt.mobile.android.serializers.validationwrapper.PlainSecretValidationWrapper
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordAndDescription
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordDescriptionTotp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordString
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5Default
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5DefaultWithTotp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5PasswordString
import com.passbolt.mobile.android.ui.EncryptedSecretOrError
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.UserModel
import java.time.ZonedDateTime

class UpdateResourceInteractor(
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val resourceModelMapper: ResourceModelMapper,
    private val resourceRepository: ResourceRepository,
    private val fetchUsersUseCase: FetchUsersUseCase,
    private val getResourceTypeIdToSlugMappingUseCase: GetResourceTypeIdToSlugMappingUseCase,
    private val jsonSchemaValidationRunner: JsonSchemaValidationRunner,
    private val gson: Gson,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val getPrivateKeyUseCase: GetPrivateKeyUseCase,
    private val openPgp: OpenPgp,
    private val passwordExpirySettingsUseCase: GetPasswordExpirySettingsUseCase
) {

    suspend fun execute(input: ResourceInput, secretInput: SecretInput): Output {
        val passphrase = when (val result = passphraseMemoryCache.get()) {
            is PotentialPassphrase.Passphrase -> result.passphrase
            is PotentialPassphrase.PassphraseNotPresent -> return Output.PasswordExpired
        }

        return when (val usersWhoHaveAccess =
            fetchUsersUseCase.execute(FetchUsersUseCase.Input(listOf(input.resourceId)))) {
            is FetchUsersUseCase.Output.Failure<*> -> Output.Failure(usersWhoHaveAccess.response)
            is FetchUsersUseCase.Output.Success -> {
                if (isSecretValid(
                        PlainSecretValidationWrapper(
                            secretInput.secretModel.json,
                            ContentType.fromSlug(input.newResourceTypeSlug)
                        )
                            .validationPlainSecret,
                        input.newResourceTypeSlug
                    )
                ) {
                    createResource(secretInput, passphrase, usersWhoHaveAccess, input)
                } else {
                    Output.JsonSchemaValidationFailure(SECRET)
                }
            }
        }
    }

    private suspend fun createResource(
        secretInput: SecretInput,
        passphrase: ByteArray,
        usersWhoHaveAccess: FetchUsersUseCase.Output.Success,
        resourceInput: ResourceInput
    ): Output {
        val encryptedSecrets = encrypt(secretInput.secretModel.json, passphrase, usersWhoHaveAccess.users)
        return if (encryptedSecrets.any { it is EncryptedSecretOrError.Error }) {
            Output.OpenPgpError(
                encryptedSecrets.filterIsInstance<EncryptedSecretOrError.Error>().first().message
            )
        } else {
            val secrets = encryptedSecrets.filterIsInstance<EncryptedSecretOrError.EncryptedSecret>()
            // TODO
            val createResourceDto = CreateV4ResourceDto(
                name = resourceInput.resourceName,
                resourceTypeId = getResourceTypeIdForSlug(resourceInput.newResourceTypeSlug),
                secrets = secrets.map { EncryptedSecret(it.userId, it.data) },
                username = resourceInput.resourceUsername,
                uri = resourceInput.resourceUri,
                description = resourceInput.description,
                folderParentId = resourceInput.resourceParentFolderId,
                expiry = getResourceExpiry(resourceInput, secretInput)
            )
            if (isResourceValid(createResourceDto, resourceInput.newResourceTypeSlug)) {
                when (val response = resourceRepository.updateResource(
                    resourceInput.resourceId,
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

    @Suppress("NestedBlockDepth")
    // https://drive.google.com/file/d/1lqiF0ajpuvx1xaZ74aSSjxiDLMGPBXVa/view?usp=drive_link
    private suspend fun getResourceExpiry(resourceInput: ResourceInput, secretInput: SecretInput): ZonedDateTime? {
        return if (resourcesSlugsSupportingExpiry.contains(ContentType.fromSlug(resourceInput.newResourceTypeSlug))) {
            val expirySettings = passwordExpirySettingsUseCase.execute(Unit).expirySettings
            if (expirySettings.automaticUpdate) {
                if (secretInput.passwordChanged) {
                    if (expirySettings.defaultExpiryPeriodDays != null) {
                        ZonedDateTime.now()
                            .plusDays(expirySettings.defaultExpiryPeriodDays!!.toLong())
                            .withFixedOffsetZone()
                    } else {
                        null
                    }
                } else {
                    resourceInput.expiry
                }
            } else {
                resourceInput.expiry
            }
        } else {
            null
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

    private suspend fun isSecretValid(plainSecret: String, slug: String) =
        jsonSchemaValidationRunner.isSecretValid(plainSecret, slug)

    private suspend fun isResourceValid(createResourceDto: CreateResourceDto, slug: String) =
        jsonSchemaValidationRunner.isResourceValid(gson.toJson(createResourceDto), slug)

    private suspend fun getResourceTypeIdForSlug(slug: String) =
        getResourceTypeIdToSlugMappingUseCase.execute(Unit)
            .idToSlugMapping
            .filterValues { it == slug }
            .keys
            .first()
            .toString()

    data class ResourceInput(
        val resourceId: String,
        val resourceName: String,
        val resourceUsername: String?,
        val resourceUri: String?,
        val resourceParentFolderId: String?,
        val description: String?,
        val newResourceTypeSlug: String,
        val expiry: ZonedDateTime?
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

    private companion object {
        private val resourcesSlugsSupportingExpiry = setOf(
            PasswordString,
            PasswordAndDescription,
            PasswordDescriptionTotp,
            V5PasswordString,
            V5Default,
            V5DefaultWithTotp
        )
    }
}
