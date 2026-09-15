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

package com.passbolt.mobile.android.domain.resources.actions

import com.passbolt.mobile.android.domain.folders.usecase.GetLocalFolderPermissionsUseCase
import com.passbolt.mobile.android.domain.folders.usecase.GetLocalParentFolderPermissionsToApplyToNewItemUseCase
import com.passbolt.mobile.android.domain.folders.usecase.ItemIdResourceId
import com.passbolt.mobile.android.domain.metadata.interactor.MetadataPrivateKeysInteractor
import com.passbolt.mobile.android.domain.metadata.interactor.MetadataPrivateKeysInteractor.Output.TrustedKeyDeleted
import com.passbolt.mobile.android.domain.metadata.model.MetadataKeyPurpose.ENCRYPT
import com.passbolt.mobile.android.domain.metadata.usecase.GetMetadataKeysSettingsUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.GetMetadataTypesSettingsUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.db.GetLocalMetadataKeysUseCase
import com.passbolt.mobile.android.domain.resources.actions.ResourceCreateActionResult.CannotCreateWithCurrentConfig
import com.passbolt.mobile.android.domain.resources.actions.ResourceCreateActionResult.CryptoFailure
import com.passbolt.mobile.android.domain.resources.actions.ResourceCreateActionResult.Failure
import com.passbolt.mobile.android.domain.resources.actions.ResourceCreateActionResult.FetchFailure
import com.passbolt.mobile.android.domain.resources.actions.ResourceCreateActionResult.JsonSchemaValidationFailure
import com.passbolt.mobile.android.domain.resources.actions.ResourceCreateActionResult.MetadataKeyDeleted
import com.passbolt.mobile.android.domain.resources.actions.ResourceCreateActionResult.MetadataKeyModified
import com.passbolt.mobile.android.domain.resources.actions.ResourceCreateActionResult.PermissionsDrifted
import com.passbolt.mobile.android.domain.resources.actions.ResourceCreateActionResult.ShareFailure
import com.passbolt.mobile.android.domain.resources.actions.ResourceCreateActionResult.SimulateShareFailure
import com.passbolt.mobile.android.domain.resources.actions.ResourceCreateActionResult.Success
import com.passbolt.mobile.android.domain.resources.actions.ResourceCreateActionResult.Unauthorized
import com.passbolt.mobile.android.domain.resources.interactor.create.CreateResourceInteractor
import com.passbolt.mobile.android.domain.resources.usecase.CreatePermissionsSnapshotInteractor
import com.passbolt.mobile.android.domain.resources.usecase.CreatePermissionsSnapshotInteractor.DriftOutput
import com.passbolt.mobile.android.domain.resources.usecase.ResourceShareInteractor
import com.passbolt.mobile.android.domain.resources.usecase.db.AddLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.domain.resources.usecase.db.AddLocalResourceUseCase
import com.passbolt.mobile.android.domain.resourcetypes.usecase.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.domain.secrets.model.SecretJsonModel
import com.passbolt.mobile.android.domain.users.usecase.GetLocalCurrentUserUseCase
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.CreateResourceModel
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.MetadataKeyParamsModel
import com.passbolt.mobile.android.ui.MetadataKeyTypeModel
import com.passbolt.mobile.android.ui.MetadataTypeModel
import com.passbolt.mobile.android.ui.NewMetadataKeyToTrustModel
import com.passbolt.mobile.android.ui.PermissionModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourceUiModelWithAttributes
import com.passbolt.mobile.android.ui.TrustedKeyDeletedModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import timber.log.Timber

class ResourceCreateActionsInteractor(
    private val createResourceInteractor: CreateResourceInteractor,
    private val addLocalResourceUseCase: AddLocalResourceUseCase,
    private val addLocalResourcePermissionsUseCase: AddLocalResourcePermissionsUseCase,
    private val resourceShareInteractor: ResourceShareInteractor,
    private val getLocalParentFolderPermissionsToApplyUseCase: GetLocalParentFolderPermissionsToApplyToNewItemUseCase,
    private val getLocalFolderPermissionsUseCase: GetLocalFolderPermissionsUseCase,
    private val getMetadataKeysSettingsUseCase: GetMetadataKeysSettingsUseCase,
    private val getMetadataTypesSettingsUseCase: GetMetadataTypesSettingsUseCase,
    private val getMetadataKeysUseCase: GetLocalMetadataKeysUseCase,
    private val getLocalCurrentUserUseCase: GetLocalCurrentUserUseCase,
    private val metadataPrivateKeysInteractor: MetadataPrivateKeysInteractor,
    private val resourceTypeIdToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider,
    private val createPermissionsSnapshotInteractor: CreatePermissionsSnapshotInteractor,
    private val confirmedRecipientsPublicKeysResolver: ConfirmedRecipientsPublicKeysResolver,
) {
    suspend fun createGenericResource(
        contentType: ContentType,
        resourceParentFolderId: String?,
        metadataJsonModel: MetadataJsonModel,
        secretJsonModel: SecretJsonModel,
    ): Flow<ResourceCreateActionResult> =
        createGenericResource(
            contentType = contentType,
            resourceParentFolderId = resourceParentFolderId,
            metadataJsonModel = metadataJsonModel,
            secretJsonModel = secretJsonModel,
            applyPermissions = ::applyParentFolderPermissions,
        )

    suspend fun createGenericResourceWithConfirmedPermissions(
        contentType: ContentType,
        resourceParentFolderId: String?,
        metadataJsonModel: MetadataJsonModel,
        secretJsonModel: SecretJsonModel,
        confirmedPermissions: List<PermissionModelUi>,
    ): Flow<ResourceCreateActionResult> =
        createGenericResource(
            contentType = contentType,
            resourceParentFolderId = resourceParentFolderId,
            metadataJsonModel = metadataJsonModel,
            secretJsonModel = secretJsonModel,
            applyPermissions = { createdResource -> applyConfirmedPermissions(createdResource, confirmedPermissions) },
        )

    private suspend fun createGenericResource(
        contentType: ContentType,
        resourceParentFolderId: String?,
        metadataJsonModel: MetadataJsonModel,
        secretJsonModel: SecretJsonModel,
        applyPermissions: suspend (ResourceUiModelWithAttributes) -> ResourceCreateActionResult,
    ): Flow<ResourceCreateActionResult> =
        if (!isSupported(contentType)) {
            flowOf(CannotCreateWithCurrentConfig)
        } else {
            when (val metadataKeyParams = getMetadataKeysParams(resourceParentFolderId)) {
                is MetadataKeyParamsModel.ErrorDuringVerification -> {
                    flowOf(ResourceCreateActionResult.MetadataKeyVerificationFailure)
                }
                is MetadataKeyParamsModel.NewMetadataKeyToTrust -> {
                    flowOf(MetadataKeyModified(metadataKeyParams.newMetadataKeyToTrust))
                }
                is MetadataKeyParamsModel.TrustedKeyDeleted -> {
                    flowOf(MetadataKeyDeleted(metadataKeyParams.trustedKeyDeleted))
                }
                is MetadataKeyParamsModel.ParamsModel -> {
                    createResource(
                        createResource = {
                            CreateResourceModel(
                                contentType = contentType,
                                folderId = resourceParentFolderId,
                                expiry = null,
                                metadataKeyId = metadataKeyParams.metadataKeyId,
                                metadataKeyType = metadataKeyParams.metadataKeyType,
                                metadataJsonModel = metadataJsonModel,
                            )
                        },
                        createSecret = { secretJsonModel },
                        applyPermissions = applyPermissions,
                    )
                }
            }
        }

    private suspend fun isSupported(contentType: ContentType) =
        resourceTypeIdToSlugMappingProvider
            .provideMappingForSelectedAccount()
            .values
            .contains(contentType.slug)

    private suspend fun shouldPersonalKeyBeUsed(parentFolderId: String?): Boolean {
        val isPersonalKeyAllowed =
            getMetadataKeysSettingsUseCase
                .execute(Unit)
                .metadataKeysSettingsModel.allowUsageOfPersonalKeys
        val isParentFolderShared =
            parentFolderId?.let {
                getLocalFolderPermissionsUseCase
                    .execute(
                        GetLocalFolderPermissionsUseCase.Input(parentFolderId),
                    ).permissions.size > 1
            } ?: false

        return isPersonalKeyAllowed && !isParentFolderShared
    }

    @Suppress("LongMethod")
    private suspend fun getMetadataKeysParams(parentFolderId: String?): MetadataKeyParamsModel {
        val metadataKeyType =
            if (shouldPersonalKeyBeUsed(parentFolderId)) {
                MetadataKeyTypeModel.PERSONAL
            } else {
                MetadataKeyTypeModel.SHARED
            }

        return when (metadataKeyType) {
            MetadataKeyTypeModel.SHARED -> {
                val verifyOutput =
                    runAuthenticatedOperation {
                        metadataPrivateKeysInteractor.refreshAndVerifyMetadataPrivateKey()
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
                                modificationKind = verifyOutput.modificationKind,
                            ),
                        )
                    }
                    is TrustedKeyDeleted -> {
                        MetadataKeyParamsModel.TrustedKeyDeleted(
                            TrustedKeyDeletedModel(
                                keyFingerprint = verifyOutput.keyFingerprint,
                                signedUsername = verifyOutput.signedUsername,
                                signedName = verifyOutput.signedName,
                                modificationKind = verifyOutput.modificationKind,
                            ),
                        )
                    }
                    else -> {
                        // for cases when not able to verify (i.e. cannot get user, cannot validate signature)
                        // do not block the user
                        MetadataKeyParamsModel.ParamsModel(
                            metadataKeyId =
                                getMetadataKeysUseCase
                                    .execute(GetLocalMetadataKeysUseCase.Input(ENCRYPT))
                                    .firstOrNull()
                                    ?.id
                                    ?.toString(),
                            metadataKeyType = MetadataKeyTypeModel.SHARED,
                        )
                    }
                }
            }
            MetadataKeyTypeModel.PERSONAL -> {
                MetadataKeyParamsModel.ParamsModel(
                    metadataKeyId =
                        getLocalCurrentUserUseCase
                            .execute(Unit)
                            .user.gpgKey.id,
                    metadataKeyType = MetadataKeyTypeModel.PERSONAL,
                )
            }
        }
    }

    private suspend fun getMetadataType() = getMetadataTypesSettingsUseCase.execute(Unit).metadataTypesSettingsModel.defaultMetadataType

    private suspend fun createResource(
        createResource: (MetadataTypeModel) -> CreateResourceModel,
        createSecret: () -> SecretJsonModel,
        applyPermissions: suspend (ResourceUiModelWithAttributes) -> ResourceCreateActionResult,
    ): Flow<ResourceCreateActionResult> =
        try {
            flowOf(
                runCreateOperation(
                    operation = {
                        createResourceInteractor.execute(
                            resourceInput = createResource(getMetadataType()),
                            secretInput = createSecret(),
                        )
                    },
                    applyPermissions = applyPermissions,
                ),
            )
        } catch (e: Exception) {
            Timber.e(e, "Error updating resource")
            flowOf(Failure())
        }

    private suspend fun runCreateOperation(
        operation: suspend () -> CreateResourceInteractor.Output,
        applyPermissions: suspend (ResourceUiModelWithAttributes) -> ResourceCreateActionResult,
    ): ResourceCreateActionResult =
        when (
            val operationResult =
                runAuthenticatedOperation {
                    operation()
                }
        ) {
            is CreateResourceInteractor.Output.Failure -> {
                Failure(operationResult.message)
            }
            is CreateResourceInteractor.Output.OpenPgpError -> {
                CryptoFailure(operationResult.message)
            }
            is CreateResourceInteractor.Output.PasswordExpired -> {
                Unauthorized
            }
            is CreateResourceInteractor.Output.Success -> {
                addLocalResourceUseCase.execute(AddLocalResourceUseCase.Input(operationResult.resource.resourceModel))
                addLocalResourcePermissionsUseCase.execute(
                    AddLocalResourcePermissionsUseCase.Input(listOf(operationResult.resource)),
                )
                applyPermissions(operationResult.resource)
            }
            is CreateResourceInteractor.Output.JsonSchemaValidationFailure ->
                JsonSchemaValidationFailure(operationResult.entity)
        }

    private suspend fun applyParentFolderPermissions(createdResource: ResourceUiModelWithAttributes): ResourceCreateActionResult {
        val newFolderPermissionsToApply =
            createdResource.resourceModel.folderId
                ?.let {
                    getLocalParentFolderPermissionsToApplyUseCase
                        .execute(
                            GetLocalParentFolderPermissionsToApplyToNewItemUseCase.Input(
                                it,
                                ItemIdResourceId(createdResource.resourceModel.resourceId),
                            ),
                        ).permissions
                }.orEmpty()

        return if (newFolderPermissionsToApply.size > 1) {
            applyPermissionsToCreatedResource(
                createdResource.resourceModel.resourceId,
                createdResource.resourceModel.metadataJsonModel.name,
                newFolderPermissionsToApply,
                recipientsPublicKeys = emptyMap(),
            )
        } else {
            Success(
                createdResource.resourceModel.resourceId,
                createdResource.resourceModel.metadataJsonModel.name,
            )
        }
    }

    private suspend fun applyConfirmedPermissions(
        createdResource: ResourceUiModelWithAttributes,
        confirmedPermissions: List<PermissionModelUi>,
    ): ResourceCreateActionResult {
        Timber.d("Applying confirmed permissions to the created resource")
        val resourceId = createdResource.resourceModel.resourceId
        val resourceName = createdResource.resourceModel.metadataJsonModel.name
        val permissionsToApply = withOperatorRealPermissionId(confirmedPermissions, createdResource)
        if (!hasRecipientsBesidesOperator(permissionsToApply)) {
            Timber.d("No recipients besides the operator - keeping the created resource private")
            return Success(resourceId, resourceName)
        }

        return when (val driftOutput = detectPermissionsDrift(createdResource.resourceModel.folderId)) {
            is DriftOutput.NoDrift ->
                applyPermissionsToCreatedResource(
                    resourceId,
                    resourceName,
                    permissionsToApply,
                    recipientsPublicKeys = confirmedRecipientsPublicKeysResolver.resolve(permissionsToApply),
                )
            is DriftOutput.DriftDetected -> PermissionsDrifted
            is DriftOutput.SnapshotMissing -> PermissionsDrifted
            is DriftOutput.Failure -> {
                Timber.e("Unable to verify permissions drift: ${driftOutput.message} - not sharing")
                ShareFailure(driftOutput.message)
            }
        }
    }

    private suspend fun detectPermissionsDrift(folderId: String?): DriftOutput =
        if (folderId == null) {
            DriftOutput.NoDrift
        } else {
            runAuthenticatedOperation {
                createPermissionsSnapshotInteractor.detectDriftForFolder(folderId)
            }
        }

    private suspend fun hasRecipientsBesidesOperator(permissionsToApply: List<PermissionModelUi>): Boolean {
        val currentUserServerId =
            getLocalCurrentUserUseCase
                .execute(Unit)
                .user.id
        return permissionsToApply.any {
            it !is PermissionModelUi.UserPermissionModel || it.user.userId != currentUserServerId
        }
    }

    private suspend fun withOperatorRealPermissionId(
        confirmedPermissions: List<PermissionModelUi>,
        createdResource: ResourceUiModelWithAttributes,
    ): List<PermissionModelUi> {
        val currentUserServerId =
            getLocalCurrentUserUseCase
                .execute(Unit)
                .user.id
        val createdOperatorPermissionId =
            createdResource.resourcePermissions
                .filterIsInstance<PermissionModel.UserPermissionModel>()
                .firstOrNull { it.userId == currentUserServerId }
                ?.permissionId
                ?: return confirmedPermissions
        return confirmedPermissions.map {
            if (it is PermissionModelUi.UserPermissionModel && it.user.userId == currentUserServerId) {
                it.copy(permissionId = createdOperatorPermissionId)
            } else {
                it
            }
        }
    }

    private suspend fun applyPermissionsToCreatedResource(
        resourceId: String,
        resourceName: String,
        newPermissionsToApply: List<PermissionModelUi>,
        recipientsPublicKeys: Map<String, String>,
    ): ResourceCreateActionResult =
        when (
            val shareResult =
                runAuthenticatedOperation {
                    resourceShareInteractor.simulateAndShareResource(
                        resourceId,
                        newPermissionsToApply,
                        recipientsPublicKeys,
                    )
                }
        ) {
            is ResourceShareInteractor.Output.SecretDecryptFailure -> CryptoFailure(shareResult.message)
            is ResourceShareInteractor.Output.SecretEncryptFailure -> CryptoFailure(shareResult.message)
            is ResourceShareInteractor.Output.SecretFetchFailure -> FetchFailure
            is ResourceShareInteractor.Output.ShareFailure -> ShareFailure(shareResult.message)
            is ResourceShareInteractor.Output.SimulateShareFailure -> ShareFailure(shareResult.message)
            is ResourceShareInteractor.Output.Success -> Success(resourceId, resourceName)
            is ResourceShareInteractor.Output.DriftDetected -> PermissionsDrifted
            is ResourceShareInteractor.Output.Unauthorized -> Unauthorized
        }
}

@Suppress("LongParameterList", "CyclomaticComplexMethod")
suspend fun performResourceCreateAction(
    action: suspend () -> Flow<ResourceCreateActionResult>,
    doOnCryptoFailure: (String) -> Unit,
    doOnFailure: (String) -> Unit,
    doOnSuccess: (Success) -> Unit,
    doOnSchemaValidationFailure: (SchemaEntity) -> Unit,
    doOnCannotCreateWithCurrentConfig: () -> Unit,
    doOnMetadataKeyModified: (NewMetadataKeyToTrustModel) -> Unit,
    doOnMetadataKeyDeleted: (TrustedKeyDeletedModel) -> Unit,
    doOnFetchFailure: () -> Unit = {},
    doOnUnauthorized: () -> Unit = {},
    doOnShareFailure: (String) -> Unit = {},
    doOnPermissionsDrifted: () -> Unit = {},
    doOnMetadataKeyVerificationFailure: () -> Unit = {},
) {
    action().single().let {
        when (it) {
            is CryptoFailure -> doOnCryptoFailure(it.message.orEmpty())
            is Failure -> doOnFailure(it.message.orEmpty())
            is FetchFailure -> doOnFetchFailure()
            is Success -> doOnSuccess(it)
            is Unauthorized -> doOnUnauthorized()
            is JsonSchemaValidationFailure -> doOnSchemaValidationFailure(it.entity)
            is ShareFailure -> doOnShareFailure(it.message.orEmpty())
            is SimulateShareFailure -> doOnShareFailure(it.message.orEmpty())
            is PermissionsDrifted -> doOnPermissionsDrifted()
            is CannotCreateWithCurrentConfig -> doOnCannotCreateWithCurrentConfig()
            is MetadataKeyModified -> doOnMetadataKeyModified(it.keyToTrust)
            is MetadataKeyDeleted -> doOnMetadataKeyDeleted(it.deletedKey)
            is ResourceCreateActionResult.MetadataKeyVerificationFailure -> doOnMetadataKeyVerificationFailure()
        }
    }
}
