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

import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.networking.MfaTypeProvider
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.database.impl.resourcetypes.GetResourceTypeIdToSlugMappingUseCase
import com.passbolt.mobile.android.dto.request.CreateResourceDto
import com.passbolt.mobile.android.dto.request.EncryptedSecret
import com.passbolt.mobile.android.mappers.PermissionsModelMapper
import com.passbolt.mobile.android.mappers.ResourceModelMapper
import com.passbolt.mobile.android.passboltapi.resource.ResourceRepository
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.cache.passphrase.PotentialPassphrase
import com.passbolt.mobile.android.ui.EncryptedSecretOrError
import com.passbolt.mobile.android.ui.ResourceModelWithAttributes

abstract class CreateResourceInteractor<CustomInput>(
    private val resourceRepository: ResourceRepository,
    private val resourceModelMapper: ResourceModelMapper,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val permissionsModelMapper: PermissionsModelMapper,
    private val getResourceTypeIdToSlugMappingUseCase: GetResourceTypeIdToSlugMappingUseCase
) {

    abstract val slug: String

    suspend fun execute(input: CommonInput, customInput: CustomInput): Output {
        val passphrase = when (val result = passphraseMemoryCache.get()) {
            is PotentialPassphrase.Passphrase -> result.passphrase
            is PotentialPassphrase.PassphraseNotPresent -> return Output.PasswordExpired
        }

        return when (val secret = createSecret(input, customInput, passphrase)) {
            is EncryptedSecretOrError.Error -> Output.OpenPgpError(secret.message)
            is EncryptedSecretOrError.EncryptedSecret -> {
                when (val response = resourceRepository.createResource(
                    CreateResourceDto(
                        name = input.resourceName,
                        resourceTypeId = getResourceTypeIdForSlug(slug),
                        // from API documentation: An array of secrets in object format - exactly one secret must be provided.
                        secrets = listOf(EncryptedSecret(secret.userId, secret.data)),
                        username = input.resourceUsername,
                        uri = input.resourceUri,
                        description = createCommonDescription(customInput),
                        folderParentId = input.resourceParentFolderId
                    )
                )) {
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
        }
    }

    private suspend fun getResourceTypeIdForSlug(slug: String) =
        getResourceTypeIdToSlugMappingUseCase.execute(Unit)
            .idToSlugMapping
            .filterValues { it == slug }
            .keys
            .first()
            .toString()

    abstract suspend fun createSecret(
        input: CommonInput,
        customInput: CustomInput,
        passphrase: ByteArray
    ): EncryptedSecretOrError

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

        data class Success(
            val resource: ResourceModelWithAttributes
        ) : Output()

        data class Failure<T : Any>(val response: NetworkResult.Failure<T>) : Output()

        object PasswordExpired : Output()

        data class OpenPgpError(val message: String) : Output()
    }

    data class CommonInput(
        val resourceName: String,
        val resourceUsername: String?,
        val resourceUri: String?,
        val resourceParentFolderId: String?
    )
}
