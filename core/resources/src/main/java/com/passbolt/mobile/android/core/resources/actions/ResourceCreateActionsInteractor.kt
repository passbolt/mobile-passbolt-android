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
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalParentFolderPermissionsToApplyToNewItemUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.ItemIdResourceId
import com.passbolt.mobile.android.core.mvp.authentication.UnauthenticatedReason
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionResult.CannotCreateWithCurrentConfig
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionResult.CryptoFailure
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionResult.Failure
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionResult.FetchFailure
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionResult.JsonSchemaValidationFailure
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionResult.MetadataKeyDeleted
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionResult.MetadataKeyModified
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionResult.ShareFailure
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionResult.SimulateShareFailure
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionResult.Success
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionResult.Unauthorized
import com.passbolt.mobile.android.core.resources.interactor.create.CreateResourceInteractor
import com.passbolt.mobile.android.core.resources.usecase.ResourceShareInteractor
import com.passbolt.mobile.android.core.resources.usecase.db.AddLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.AddLocalResourceUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetLocalResourceTypesUseCase
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretJsonModel
import com.passbolt.mobile.android.core.users.usecase.db.GetLocalCurrentUserUseCase
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.metadata.interactor.MetadataPrivateKeysInteractor
import com.passbolt.mobile.android.metadata.interactor.MetadataPrivateKeysInteractor.Output.TrustedKeyDeleted
import com.passbolt.mobile.android.metadata.usecase.GetMetadataKeysSettingsUseCase
import com.passbolt.mobile.android.metadata.usecase.GetMetadataTypesSettingsUseCase
import com.passbolt.mobile.android.metadata.usecase.db.GetLocalMetadataKeysUseCase
import com.passbolt.mobile.android.metadata.usecase.db.GetLocalMetadataKeysUseCase.MetadataKeyPurpose.ENCRYPT
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.CreateResourceModel
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.MetadataKeyParamsModel
import com.passbolt.mobile.android.ui.MetadataKeyTypeModel
import com.passbolt.mobile.android.ui.MetadataTypeModel
import com.passbolt.mobile.android.ui.NewMetadataKeyToTrustModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.TrustedKeyDeletedModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import org.koin.core.component.KoinComponent
import timber.log.Timber

class ResourceCreateActionsInteractor(
    private val needSessionRefreshFlow: MutableStateFlow<UnauthenticatedReason?>,
    private val sessionRefreshedFlow: StateFlow<Unit?>,
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
    private val getLocalResourceTypesUseCase: GetLocalResourceTypesUseCase,
    private val metadataPrivateKeysInteractor: MetadataPrivateKeysInteractor
) : KoinComponent {

    suspend fun createGenericResource(
        contentType: ContentType,
        resourceParentFolderId: String?,
        metadataJsonModel: MetadataJsonModel,
        secretJsonModel: SecretJsonModel
    ): Flow<ResourceCreateActionResult> {
        return if (isDeleted(contentType)) {
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
                                metadataJsonModel = metadataJsonModel
                            )
                        },
                        createSecret = { secretJsonModel }
                    )
                }
            }
        }
    }

    private suspend fun isDeleted(contentType: ContentType): Boolean {
        val deletedContentTypes = getLocalResourceTypesUseCase.execute(Unit)
            .resourceTypes
            .filter { it.isDeleted }
            .map { ContentType.fromSlug(it.slug) }
        return deletedContentTypes.contains(contentType)
    }

    private suspend fun shouldPersonalKeyBeUsed(parentFolderId: String?): Boolean {
        val isPersonalKeyAllowed = getMetadataKeysSettingsUseCase.execute(Unit)
            .metadataKeysSettingsModel.allowUsageOfPersonalKeys
        val isParentFolderShared = parentFolderId?.let {
            getLocalFolderPermissionsUseCase.execute(
                GetLocalFolderPermissionsUseCase.Input(parentFolderId)
            ).permissions.size > 1
        } ?: false

        return isPersonalKeyAllowed && !isParentFolderShared
    }

    private suspend fun getMetadataKeysParams(parentFolderId: String?): MetadataKeyParamsModel {
        val metadataKeyType = if (shouldPersonalKeyBeUsed(parentFolderId)) {
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

    private suspend fun getMetadataType() =
        getMetadataTypesSettingsUseCase.execute(Unit).metadataTypesSettingsModel.defaultMetadataType

    private suspend fun createResource(
        createResource: (MetadataTypeModel) -> CreateResourceModel,
        createSecret: () -> SecretJsonModel
    ): Flow<ResourceCreateActionResult> {
        return try {
            flowOf(
                runCreateOperation {
                    createResourceInteractor.execute(
                        resourceInput = createResource(getMetadataType()),
                        secretInput = createSecret()
                    )
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Error updating resource")
            flowOf(Failure())
        }
    }

    private suspend fun runCreateOperation(
        operation: suspend () -> CreateResourceInteractor.Output
    ): ResourceCreateActionResult {
        return when (val operationResult = runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
            operation()
        }) {
            is CreateResourceInteractor.Output.Failure<*> -> {
                Failure(operationResult.response.exception.message)
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
                    AddLocalResourcePermissionsUseCase.Input(listOf(operationResult.resource))
                )

                val newFolderPermissionsToApply = operationResult.resource.resourceModel.folderId?.let {
                    getLocalParentFolderPermissionsToApplyUseCase.execute(
                        GetLocalParentFolderPermissionsToApplyToNewItemUseCase.Input(
                            it,
                            ItemIdResourceId(operationResult.resource.resourceModel.resourceId)
                        )
                    ).permissions
                }.orEmpty()

                if (newFolderPermissionsToApply.size > 1) {
                    applyFolderPermissionsToCreatedResource(
                        operationResult.resource.resourceModel,
                        newFolderPermissionsToApply
                    )
                } else {
                    Success(
                        operationResult.resource.resourceModel.resourceId,
                        operationResult.resource.resourceModel.metadataJsonModel.name
                    )
                }
            }
            is CreateResourceInteractor.Output.JsonSchemaValidationFailure ->
                JsonSchemaValidationFailure(operationResult.entity)
        }
    }

    private suspend fun applyFolderPermissionsToCreatedResource(
        resource: ResourceModel,
        newPermissionsToApply: List<PermissionModelUi>
    ): ResourceCreateActionResult {
        return when (val shareResult = runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
            resourceShareInteractor.simulateAndShareResource(resource.resourceId, newPermissionsToApply)
        }) {
            is ResourceShareInteractor.Output.SecretDecryptFailure -> CryptoFailure(shareResult.message)
            is ResourceShareInteractor.Output.SecretEncryptFailure -> CryptoFailure(shareResult.message)
            is ResourceShareInteractor.Output.SecretFetchFailure -> FetchFailure
            is ResourceShareInteractor.Output.ShareFailure -> ShareFailure(shareResult.exception.message)
            is ResourceShareInteractor.Output.SimulateShareFailure -> ShareFailure(shareResult.exception.message)
            is ResourceShareInteractor.Output.Success -> Success(resource.resourceId, resource.metadataJsonModel.name)
            is ResourceShareInteractor.Output.Unauthorized -> Unauthorized
        }
    }
}

@Suppress("LongParameterList")
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
    doOnMetadataKeyVerificationFailure: () -> Unit = {}
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
            is CannotCreateWithCurrentConfig -> doOnCannotCreateWithCurrentConfig()
            is MetadataKeyModified -> doOnMetadataKeyModified(it.keyToTrust)
            is MetadataKeyDeleted -> doOnMetadataKeyDeleted(it.deletedKey)
            is ResourceCreateActionResult.MetadataKeyVerificationFailure -> doOnMetadataKeyVerificationFailure()
        }
    }
}
