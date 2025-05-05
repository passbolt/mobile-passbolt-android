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

import com.passbolt.mobile.android.core.mvp.authentication.UnauthenticatedReason
import com.passbolt.mobile.android.core.resources.interactor.update.UpdateResourceInteractor
import com.passbolt.mobile.android.core.resources.usecase.db.UpdateLocalResourceUseCase
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.ResourceTypesUpdatesAdjacencyGraph
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetLocalResourceTypesUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.SecretInput
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretJsonModel
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.MetadataKeyTypeModel
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.UpdateResourceModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import org.koin.core.component.KoinComponent
import timber.log.Timber
import java.util.UUID

class ResourceUpdateActionsInteractor(
    private val existingResource: ResourceModel,
    private val needSessionRefreshFlow: MutableStateFlow<UnauthenticatedReason?>,
    private val sessionRefreshedFlow: StateFlow<Unit?>,
    private val secretPropertiesActionsInteractor: SecretPropertiesActionsInteractor,
    private val updateResourceInteractor: UpdateResourceInteractor,
    private val resourceTypesUpdateGraph: ResourceTypesUpdatesAdjacencyGraph,
    private val updateLocalResourceUseCase: UpdateLocalResourceUseCase,
    private val idToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider,
    private val getLocalResourceTypesUseCase: GetLocalResourceTypesUseCase
) : KoinComponent {

    suspend fun updateGenericResource(
        newContentType: ContentType,
        metadataModification: (MetadataJsonModel) -> MetadataJsonModel = { it },
        secretModification: (SecretJsonModel) -> SecretJsonModel = { it }
    ): Flow<ResourceUpdateActionResult> {
        return if (isDeleted(newContentType)) {
            flowOf(ResourceUpdateActionResult.CannotUpdateWithCurrentConfig)
        } else {
            updateResource(
                updateResource = {
                    UpdateResourceModel(
                        contentType = newContentType,
                        resourceId = existingResource.resourceId,
                        folderId = existingResource.folderId,
                        expiry = existingResource.expiry,
                        metadataKeyId = existingResource.metadataKeyId,
                        metadataKeyType = existingResource.metadataKeyType,
                        metadataJsonModel = metadataModification(existingResource.metadataJsonModel)
                    )
                },
                updateSecret = { decryptedSecret ->
                    val existingResourceContentType = ContentType.fromSlug(
                        idToSlugMappingProvider.provideMappingForSelectedAccount()[
                            UUID.fromString(existingResource.resourceTypeId)
                        ]!!
                    )
                    val modifiedSecret = secretModification(decryptedSecret)
                    val passwordChanged =
                        decryptedSecret.getPassword(existingResourceContentType) != modifiedSecret.getPassword(
                            newContentType
                        )
                    SecretInput(
                        secretJsonModel = modifiedSecret,
                        passwordChanged = passwordChanged
                    )
                }
            )
        }
    }

    suspend fun updateGenericResource(
        updateAction: UpdateAction,
        metadataModification: (MetadataJsonModel) -> MetadataJsonModel = { it },
        secretModification: (SecretJsonModel) -> SecretJsonModel = { it }
    ): Flow<ResourceUpdateActionResult> {
        val newContentType = resourceTypesUpdateGraph.getResourceTypeSlugAfterUpdate(
            idToSlugMappingProvider.provideMappingForSelectedAccount()[
                UUID.fromString(existingResource.resourceTypeId)
            ]!!,
            updateAction
        )
        return if (isDeleted(newContentType)) {
            flowOf(ResourceUpdateActionResult.CannotUpdateWithCurrentConfig)
        } else {
            updateGenericResource(newContentType, metadataModification, secretModification)
        }
    }

    suspend fun reEncryptResourceMetadata(
        metadataKeyId: String,
        metadataKeyType: MetadataKeyTypeModel
    ): Flow<ResourceUpdateActionResult> {
        val contentType = ContentType.fromSlug(
            idToSlugMappingProvider.provideMappingForSelectedAccount()[
                UUID.fromString(existingResource.resourceTypeId)
            ]!!
        )
        return updateResource(
            updateResource = {
                UpdateResourceModel(
                    contentType = contentType,
                    resourceId = existingResource.resourceId,
                    folderId = existingResource.folderId,
                    expiry = existingResource.expiry,
                    metadataKeyId = metadataKeyId,
                    metadataKeyType = metadataKeyType,
                    metadataJsonModel = existingResource.metadataJsonModel
                )
            },
            updateSecret = { decryptedSecret ->
                SecretInput(
                    secretJsonModel = decryptedSecret,
                    passwordChanged = false
                )
            }
        )
    }

    private suspend fun isDeleted(contentType: ContentType): Boolean {
        val deletedContentTypes = getLocalResourceTypesUseCase.execute(Unit)
            .resourceTypes
            .filter { it.isDeleted }
            .map { ContentType.fromSlug(it.slug) }
        return deletedContentTypes.contains(contentType)
    }

    private suspend fun updateResource(
        updateResource: () -> UpdateResourceModel,
        updateSecret: suspend (SecretJsonModel) -> SecretInput
    ): Flow<ResourceUpdateActionResult> {
        return try {
            val decryptedSecret = secretPropertiesActionsInteractor.provideDecryptedSecret().single()
            flowOf(
                when (decryptedSecret) {
                    is SecretPropertyActionResult.Success ->
                        runUpdateOperation {
                            updateResourceInteractor.execute(
                                resourceInput = updateResource(),
                                secretInput = updateSecret(decryptedSecret.result)
                            )
                        }
                    is SecretPropertyActionResult.FetchFailure ->
                        ResourceUpdateActionResult.FetchFailure
                    is SecretPropertyActionResult.DecryptionFailure ->
                        ResourceUpdateActionResult.CryptoFailure()
                    else -> ResourceUpdateActionResult.Failure()
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Error updating resource")
            flowOf(ResourceUpdateActionResult.Failure())
        }
    }

    private suspend fun runUpdateOperation(
        operation: suspend () -> UpdateResourceInteractor.Output
    ): ResourceUpdateActionResult {
        return when (val operationResult = runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
            operation()
        }) {
            is UpdateResourceInteractor.Output.Failure<*> -> {
                ResourceUpdateActionResult.Failure(operationResult.response.exception.message)
            }
            is UpdateResourceInteractor.Output.OpenPgpError -> {
                ResourceUpdateActionResult.CryptoFailure(operationResult.message)
            }
            is UpdateResourceInteractor.Output.PasswordExpired -> {
                ResourceUpdateActionResult.Unauthorized
            }
            is UpdateResourceInteractor.Output.Success -> {
                updateLocalResourceUseCase.execute(
                    UpdateLocalResourceUseCase.Input(operationResult.resource)
                )
                ResourceUpdateActionResult.Success(
                    operationResult.resource.resourceId,
                    operationResult.resource.metadataJsonModel.name
                )
            }
            is UpdateResourceInteractor.Output.JsonSchemaValidationFailure ->
                ResourceUpdateActionResult.JsonSchemaValidationFailure(operationResult.entity)
        }
    }
}

@Suppress("LongParameterList")
suspend fun performResourceUpdateAction(
    action: suspend () -> Flow<ResourceUpdateActionResult>,
    doOnCryptoFailure: (String) -> Unit,
    doOnFailure: (String) -> Unit,
    doOnSuccess: (ResourceUpdateActionResult.Success) -> Unit,
    doOnSchemaValidationFailure: (SchemaEntity) -> Unit,
    doOnCannotEditWithCurrentConfig: () -> Unit,
    doOnFetchFailure: () -> Unit = {},
    doOnUnauthorized: () -> Unit = {}
) {
    action().single().let {
        when (it) {
            is ResourceUpdateActionResult.CryptoFailure -> doOnCryptoFailure(it.message.orEmpty())
            is ResourceUpdateActionResult.Failure -> doOnFailure(it.message.orEmpty())
            is ResourceUpdateActionResult.FetchFailure -> doOnFetchFailure()
            is ResourceUpdateActionResult.Success -> doOnSuccess(it)
            is ResourceUpdateActionResult.Unauthorized -> doOnUnauthorized()
            is ResourceUpdateActionResult.JsonSchemaValidationFailure -> doOnSchemaValidationFailure(it.entity)
            is ResourceUpdateActionResult.CannotUpdateWithCurrentConfig -> doOnCannotEditWithCurrentConfig()
        }
    }
}
