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
import com.passbolt.mobile.android.core.resourcetypes.graph.ResourceTypesUpdatesAdjacencyGraph
import com.passbolt.mobile.android.core.resourcetypes.graph.UpdateAction
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.SecretInput
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretModel
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.jsonmodel.delegates.TotpSecret
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.MetadataKeyTypeModel
import com.passbolt.mobile.android.ui.MetadataTypeModel
import com.passbolt.mobile.android.ui.MetadataTypeModel.V4
import com.passbolt.mobile.android.ui.MetadataTypeModel.V5
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
    private val resource: ResourceModel,
    private val needSessionRefreshFlow: MutableStateFlow<UnauthenticatedReason?>,
    private val sessionRefreshedFlow: StateFlow<Unit?>,
    private val secretPropertiesActionsInteractor: SecretPropertiesActionsInteractor,
    private val updateResourceInteractor: UpdateResourceInteractor,
    private val resourceTypesUpdateGraph: ResourceTypesUpdatesAdjacencyGraph,
    private val updateLocalResourceUseCase: UpdateLocalResourceUseCase,
    private val idToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider
) : KoinComponent {

    suspend fun deleteTotpFromResource(): Flow<ResourceUpdateActionResult> =
        updateResource(
            updateAction = UpdateAction.REMOVE_TOTP,
            updateResource = { newResourceType, _ ->
                UpdateResourceModel(
                    resourceId = resource.resourceId,
                    contentType = newResourceType,
                    folderId = resource.folderId,
                    expiry = resource.expiry,
                    json = resource.json,
                    metadataKeyId = resource.metadataKeyId,
                    metadataKeyType = resource.metadataKeyType
                )
            },
            updateSecret = { decryptedSecret ->
                SecretInput(
                    secretModel = decryptedSecret.apply {
                        removeTotp()
                    },
                    passwordChanged = false
                )
            }
        )

    suspend fun addTotpToResource(
        overrideName: String?,
        overrideUri: String?,
        period: Long,
        digits: Int,
        algorithm: String,
        secretKey: String
    ): Flow<ResourceUpdateActionResult> =
        updateResource(
            updateAction = UpdateAction.ADD_TOTP,
            updateResource = { newResourceType, metadataType ->
                UpdateResourceModel(
                    resourceId = resource.resourceId,
                    contentType = newResourceType,
                    folderId = resource.folderId,
                    expiry = resource.expiry,
                    json = resource.json,
                    metadataKeyId = resource.metadataKeyId,
                    metadataKeyType = resource.metadataKeyType
                ).apply {
                    name = overrideName ?: resource.name
                    username = resource.username
                    when (metadataType) {
                        V4 -> this.uri = overrideUri ?: resource.uri
                        V5 -> this.uris = if (overrideUri != null) listOf(overrideUri) else resource.uris
                    }
                }
            },
            updateSecret = { decryptedSecret ->
                SecretInput(
                    secretModel = decryptedSecret.apply {
                        totp = TotpSecret(
                            algorithm,
                            secretKey,
                            digits,
                            period
                        )
                    },
                    passwordChanged = false
                )
            }
        )

    suspend fun updatePasswordAndDescriptionResource(
        resourceName: String,
        resourceUsername: String?,
        resourceUri: String?,
        resourceParentFolderId: String?,
        password: String,
        description: String?
    ): Flow<ResourceUpdateActionResult> =
        updateResource(
            updateAction = UpdateAction.EDIT_PASSWORD,
            updateResource = { newResourceType, metadataType ->
                UpdateResourceModel(
                    resourceId = resource.resourceId,
                    contentType = newResourceType,
                    folderId = resourceParentFolderId,
                    expiry = resource.expiry,
                    json = resource.json,
                    metadataKeyId = resource.metadataKeyId,
                    metadataKeyType = resource.metadataKeyType
                ).apply {
                    name = resourceName
                    username = resourceUsername
                    when (metadataType) {
                        V4 -> this.uri = resourceUri
                        V5 -> this.uris = if (resourceUri != null) listOf(resourceUri) else emptyList()
                    }
                }
            },
            updateSecret = { decryptedSecret ->
                val passwordChanged = decryptedSecret.secret != password
                SecretInput(
                    secretModel = decryptedSecret.apply {
                        this.secret = password
                        this.description = description
                    },
                    passwordChanged = passwordChanged
                )
            }
        )

    suspend fun updateStandaloneTotpResource(
        label: String,
        issuer: String?,
        period: Long,
        digits: Int,
        algorithm: String,
        secretKey: String
    ): Flow<ResourceUpdateActionResult> =
        updateResource(
            updateAction = UpdateAction.EDIT_TOTP,
            updateResource = { newResourceType, metadataType ->
                UpdateResourceModel(
                    resourceId = resource.resourceId,
                    contentType = newResourceType,
                    folderId = resource.folderId,
                    expiry = resource.expiry,
                    json = resource.json,
                    metadataKeyId = resource.metadataKeyId,
                    metadataKeyType = resource.metadataKeyType
                ).apply {
                    name = label
                    username = resource.username
                    when (metadataType) {
                        V4 -> this.uri = issuer
                        V5 -> this.uris = if (issuer != null) listOf(issuer) else emptyList()
                    }
                }
            },
            updateSecret = { decryptedSecret ->
                SecretInput(
                    decryptedSecret.apply {
                        totp = TotpSecret(
                            algorithm,
                            secretKey,
                            digits,
                            period
                        )
                    },
                    passwordChanged = false
                )
            }
        )

    suspend fun updateSimplePasswordResource(
        resourceName: String,
        resourceUsername: String?,
        resourceUri: String?,
        resourceParentFolderId: String?,
        password: String,
        description: String?
    ): Flow<ResourceUpdateActionResult> =
        updateResource(
            updateAction = UpdateAction.EDIT_PASSWORD,
            updateResource = { newResourceType, metadataType ->
                UpdateResourceModel(
                    resourceId = resource.resourceId,
                    contentType = newResourceType,
                    folderId = resourceParentFolderId,
                    expiry = resource.expiry,
                    json = resource.json,
                    metadataKeyId = resource.metadataKeyId,
                    metadataKeyType = resource.metadataKeyType
                ).apply {
                    this.name = resourceName
                    this.username = resourceUsername
                    this.description = description
                    when (metadataType) {
                        V4 -> this.uri = resourceUri
                        V5 -> this.uris = if (resourceUri != null) listOf(resourceUri) else emptyList()
                    }
                }
            },
            updateSecret = { decryptedSecret ->
                val passwordChanged = decryptedSecret.secret != password
                SecretInput(
                    secretModel = decryptedSecret.apply {
                        this.password = password
                    },
                    passwordChanged = passwordChanged
                )
            }
        )

    suspend fun updateLinkedTotpResourceTotpFields(
        label: String,
        issuer: String?,
        period: Long,
        digits: Int,
        algorithm: String,
        secretKey: String
    ): Flow<ResourceUpdateActionResult> =
        updateResource(
            updateAction = UpdateAction.EDIT_TOTP,
            updateResource = { newResourceType, metadataType ->
                UpdateResourceModel(
                    resourceId = resource.resourceId,
                    contentType = newResourceType,
                    folderId = resource.folderId,
                    expiry = resource.expiry,
                    json = resource.json,
                    metadataKeyId = resource.metadataKeyId,
                    metadataKeyType = resource.metadataKeyType
                ).apply {
                    name = label
                    username = resource.username
                    when (metadataType) {
                        V4 -> this.uri = issuer
                        V5 -> this.uris = if (issuer != null) listOf(issuer) else emptyList()
                    }
                }
            },
            updateSecret = { decryptedSecret ->
                SecretInput(
                    secretModel = decryptedSecret.apply {
                        totp = TotpSecret(
                            algorithm,
                            secretKey,
                            digits,
                            period
                        )
                    },
                    passwordChanged = false
                )
            }
        )

    suspend fun updateLinkedTotpResourcePasswordFields(
        resourceName: String,
        resourceUsername: String?,
        resourceUri: String?,
        resourceParentFolderId: String?,
        password: String,
        description: String?
    ): Flow<ResourceUpdateActionResult> =
        updateResource(
            updateAction = UpdateAction.EDIT_PASSWORD,
            updateResource = { newResourceType, metadataType ->
                UpdateResourceModel(
                    resourceId = resource.resourceId,
                    contentType = newResourceType,
                    folderId = resourceParentFolderId,
                    expiry = resource.expiry,
                    json = resource.json,
                    metadataKeyId = resource.metadataKeyId,
                    metadataKeyType = resource.metadataKeyType
                ).apply {
                    name = resourceName
                    username = resourceUsername
                    when (metadataType) {
                        V4 -> this.uri = resourceUri
                        V5 -> this.uris = if (resourceUri != null) listOf(resourceUri) else emptyList()
                    }
                }
            },
            updateSecret = { decryptedSecret ->
                val passwordChanged = decryptedSecret.secret != password
                SecretInput(
                    secretModel = decryptedSecret.apply {
                        this.secret = password
                        this.description = description.orEmpty()
                    },
                    passwordChanged = passwordChanged
                )
            }
        )

    suspend fun reEncryptResourceMetadata(
        metadataKeyId: String,
        metadataKeyType: MetadataKeyTypeModel
    ): Flow<ResourceUpdateActionResult> =
        updateResource(
            updateAction = UpdateAction.EDIT_METADATA,
            updateResource = { newResourceType, _ ->
                UpdateResourceModel(
                    resourceId = resource.resourceId,
                    contentType = newResourceType,
                    folderId = resource.folderId,
                    expiry = resource.expiry,
                    json = resource.json,
                    metadataKeyId = metadataKeyId,
                    metadataKeyType = metadataKeyType
                )
            },
            updateSecret = { decryptedSecret ->
                SecretInput(
                    secretModel = decryptedSecret,
                    passwordChanged = false
                )
            }
        )

    private suspend fun updateResource(
        updateAction: UpdateAction,
        updateResource: (ContentType, MetadataTypeModel) -> UpdateResourceModel,
        updateSecret: (SecretModel) -> SecretInput
    ): Flow<ResourceUpdateActionResult> {
        return try {
            val decryptedSecret = secretPropertiesActionsInteractor.provideDecryptedSecret().single()
            val newResourceType = resourceTypesUpdateGraph.getResourceTypeSlugAfterUpdate(
                idToSlugMappingProvider.provideMappingForSelectedAccount()[UUID.fromString(resource.resourceTypeId)]!!,
                updateAction
            )
            flowOf(
                when (decryptedSecret) {
                    is SecretPropertyActionResult.Success ->
                        runUpdateOperation {
                            updateResourceInteractor.execute(
                                resourceInput = updateResource(
                                    newResourceType,
                                    if (newResourceType.isV5()) V5 else V4
                                ),
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
                    operationResult.resource.name
                )
            }
            is UpdateResourceInteractor.Output.JsonSchemaValidationFailure ->
                ResourceUpdateActionResult.JsonSchemaValidationFailure(operationResult.entity)
        }
    }
}

suspend fun performResourceUpdateAction(
    action: suspend () -> Flow<ResourceUpdateActionResult>,
    doOnCryptoFailure: (String) -> Unit,
    doOnFailure: (String) -> Unit,
    doOnSuccess: (ResourceUpdateActionResult.Success) -> Unit,
    doOnSchemaValidationFailure: (SchemaEntity) -> Unit,
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
        }
    }
}
