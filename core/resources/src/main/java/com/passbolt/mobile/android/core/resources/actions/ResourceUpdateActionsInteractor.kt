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

import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderPermissionsUseCase
import com.passbolt.mobile.android.core.mvp.authentication.UnauthenticatedReason
import com.passbolt.mobile.android.core.resources.interactor.update.UpdateResourceInteractor
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.UpdateLocalResourceUseCase
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.ResourceTypesUpdatesAdjacencyGraph
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetLocalResourceTypesUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.SecretInput
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretJsonModel
import com.passbolt.mobile.android.core.users.usecase.db.GetLocalCurrentUserUseCase
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.metadata.interactor.MetadataPrivateKeysInteractor
import com.passbolt.mobile.android.metadata.interactor.MetadataPrivateKeysInteractor.Output.TrustedKeyDeleted
import com.passbolt.mobile.android.metadata.usecase.GetMetadataKeysSettingsUseCase
import com.passbolt.mobile.android.metadata.usecase.db.GetLocalMetadataKeysUseCase
import com.passbolt.mobile.android.metadata.usecase.db.GetLocalMetadataKeysUseCase.MetadataKeyPurpose.ENCRYPT
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.MetadataKeyParamsModel
import com.passbolt.mobile.android.ui.MetadataKeyTypeModel
import com.passbolt.mobile.android.ui.NewMetadataKeyToTrustModel
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.TrustedKeyDeletedModel
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
    private val getLocalResourceTypesUseCase: GetLocalResourceTypesUseCase,
    private val getLocalCurrentUserUseCase: GetLocalCurrentUserUseCase,
    private val metadataPrivateKeysInteractor: MetadataPrivateKeysInteractor,
    private val getLocalFolderPermissionsUseCase: GetLocalFolderPermissionsUseCase,
    private val getLocalResourcePermissionsUseCase: GetLocalResourcePermissionsUseCase,
    private val getMetadataKeysSettingsUseCase: GetMetadataKeysSettingsUseCase,
    private val getMetadataKeysUseCase: GetLocalMetadataKeysUseCase
) : KoinComponent {

    suspend fun updateGenericResource(
        newContentType: ContentType,
        metadataModification: (MetadataJsonModel) -> MetadataJsonModel = { it },
        secretModification: (SecretJsonModel) -> SecretJsonModel = { it },
        forceMetadataSharedKey: Boolean = false
    ): Flow<ResourceUpdateActionResult> {
        return if (isDeleted(newContentType)) {
            flowOf(ResourceUpdateActionResult.CannotUpdateWithCurrentConfig)
        } else {
            when (val metadataKeyParams = getMetadataKeysParams(existingResource.folderId, forceMetadataSharedKey)) {
                is MetadataKeyParamsModel.ErrorDuringVerification -> {
                    flowOf(ResourceUpdateActionResult.MetadataKeyVerificationFailure)
                }
                is MetadataKeyParamsModel.NewMetadataKeyToTrust -> {
                    flowOf(ResourceUpdateActionResult.MetadataKeyModified(metadataKeyParams.newMetadataKeyToTrust))
                }
                is MetadataKeyParamsModel.TrustedKeyDeleted -> {
                    flowOf(ResourceUpdateActionResult.MetadataKeyDeleted(metadataKeyParams.trustedKeyDeleted))
                }
                is MetadataKeyParamsModel.ParamsModel -> {
                    updateResource(
                        updateResource = {
                            UpdateResourceModel(
                                contentType = newContentType,
                                resourceId = existingResource.resourceId,
                                folderId = existingResource.folderId,
                                expiry = existingResource.expiry,
                                metadataKeyId = metadataKeyParams.metadataKeyId,
                                metadataKeyType = metadataKeyParams.metadataKeyType,
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
        return updateGenericResource(newContentType, metadataModification, secretModification)
    }

    suspend fun reEncryptResourceMetadata(): Flow<ResourceUpdateActionResult> {
        val contentType = ContentType.fromSlug(
            idToSlugMappingProvider.provideMappingForSelectedAccount()[
                UUID.fromString(existingResource.resourceTypeId)
            ]!!
        )

        return updateGenericResource(contentType, forceMetadataSharedKey = true)
    }

    private suspend fun isDeleted(contentType: ContentType): Boolean {
        val deletedContentTypes = getLocalResourceTypesUseCase.execute(Unit)
            .resourceTypes
            .filter { it.isDeleted }
            .map { ContentType.fromSlug(it.slug) }
        return deletedContentTypes.contains(contentType)
    }

    private suspend fun shouldPersonalKeyBeUsed(parentFolderId: String?, forceMetadataSharedKey: Boolean): Boolean {
        val isPersonalKeyAllowed = getMetadataKeysSettingsUseCase.execute(Unit)
            .metadataKeysSettingsModel.allowUsageOfPersonalKeys
        val isParentFolderShared = parentFolderId?.let {
            getLocalFolderPermissionsUseCase.execute(
                GetLocalFolderPermissionsUseCase.Input(parentFolderId)
            ).permissions.size > 1
        } ?: false
        val isResourceShared = getLocalResourcePermissionsUseCase.execute(
            GetLocalResourcePermissionsUseCase.Input(existingResource.resourceId)
        ).permissions.size > 1

        return !forceMetadataSharedKey && isPersonalKeyAllowed && !isParentFolderShared && !isResourceShared
    }

    private suspend fun getMetadataKeysParams(
        parentFolderId: String?,
        forceMetadataSharedKey: Boolean
    ): MetadataKeyParamsModel {
        val metadataKeyType = if (shouldPersonalKeyBeUsed(parentFolderId, forceMetadataSharedKey)) {
            MetadataKeyTypeModel.PERSONAL
        } else {
            MetadataKeyTypeModel.SHARED
        }

        return when (metadataKeyType) {
            MetadataKeyTypeModel.SHARED -> {
                val verifyOutput = runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                    metadataPrivateKeysInteractor.verifyMetadataPrivateKey()
                }

                when (verifyOutput) {
                    is MetadataPrivateKeysInteractor.Output.Failure -> {
                        MetadataKeyParamsModel.ErrorDuringVerification
                    }
                    is MetadataPrivateKeysInteractor.Output.NewKeyToTrust -> {
                        MetadataKeyParamsModel.NewMetadataKeyToTrust(
                            NewMetadataKeyToTrustModel(
                                id = verifyOutput.metadataPrivateKey.id,
                                signedUsername = verifyOutput.signedUsername,
                                signedName = verifyOutput.signedName,
                                signatureCreationTimestampSeconds = verifyOutput.signatureCreationTimestampSeconds,
                                signatureKeyFingerprint = verifyOutput.signatureKeyFingerprint,
                                metadataPrivateKey = verifyOutput.metadataPrivateKey,
                                modificationKind = verifyOutput.modificationKind
                            )
                        )
                    }
                    is TrustedKeyDeleted -> {
                        MetadataKeyParamsModel.TrustedKeyDeleted(
                            TrustedKeyDeletedModel(
                                keyFingerprint = verifyOutput.keyFingerprint,
                                signedUsername = verifyOutput.signedUsername,
                                signedName = verifyOutput.signedName,
                                modificationKind = verifyOutput.modificationKind
                            )
                        )
                    }
                    else -> {
                        // for cases when not able to verify (i.e. cannot get user, cannot validate signature)
                        // do not block the user
                        MetadataKeyParamsModel.ParamsModel(
                            metadataKeyId = getMetadataKeysUseCase.execute(GetLocalMetadataKeysUseCase.Input(ENCRYPT))
                                .firstOrNull()?.id?.toString(),
                            metadataKeyType = MetadataKeyTypeModel.SHARED
                        )
                    }
                }
            }
            MetadataKeyTypeModel.PERSONAL -> {
                MetadataKeyParamsModel.ParamsModel(
                    metadataKeyId = getLocalCurrentUserUseCase.execute(Unit).user.gpgKey.id,
                    metadataKeyType = MetadataKeyTypeModel.PERSONAL
                )
            }
        }
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
    doOnMetadataKeyModified: (NewMetadataKeyToTrustModel) -> Unit,
    doOnMetadataKeyDeleted: (TrustedKeyDeletedModel) -> Unit,
    doOnFetchFailure: () -> Unit = {},
    doOnUnauthorized: () -> Unit = {},
    doOnMetadataKeyVerificationFailure: () -> Unit = {}
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
            is ResourceUpdateActionResult.MetadataKeyDeleted -> doOnMetadataKeyDeleted(it.deletedKey)
            is ResourceUpdateActionResult.MetadataKeyModified -> doOnMetadataKeyModified(it.keyToTrust)
            ResourceUpdateActionResult.MetadataKeyVerificationFailure -> doOnMetadataKeyVerificationFailure()
        }
    }
}
