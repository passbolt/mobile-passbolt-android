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
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionResult.CryptoFailure
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionResult.Failure
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionResult.FetchFailure
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionResult.JsonSchemaValidationFailure
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionResult.ShareFailure
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionResult.SimulateShareFailure
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionResult.Success
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionResult.Unauthorized
import com.passbolt.mobile.android.core.resources.interactor.create.CreateResourceInteractor
import com.passbolt.mobile.android.core.resources.usecase.ResourceShareInteractor
import com.passbolt.mobile.android.core.resources.usecase.db.AddLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.AddLocalResourceUseCase
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretJsonModel
import com.passbolt.mobile.android.core.users.usecase.db.GetLocalCurrentUserUseCase
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.jsonmodel.delegates.TotpSecret
import com.passbolt.mobile.android.metadata.usecase.GetMetadataKeysSettingsUseCase
import com.passbolt.mobile.android.metadata.usecase.GetMetadataTypesSettingsUseCase
import com.passbolt.mobile.android.metadata.usecase.db.GetLocalMetadataKeysUseCase
import com.passbolt.mobile.android.metadata.usecase.db.GetLocalMetadataKeysUseCase.MetadataKeyPurpose.ENCRYPT
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordAndDescription
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.Totp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5Default
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5TotpStandalone
import com.passbolt.mobile.android.ui.CreateResourceModel
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.MetadataKeyParamsModel
import com.passbolt.mobile.android.ui.MetadataKeyTypeModel
import com.passbolt.mobile.android.ui.MetadataTypeModel
import com.passbolt.mobile.android.ui.MetadataTypeModel.V4
import com.passbolt.mobile.android.ui.MetadataTypeModel.V5
import com.passbolt.mobile.android.ui.ResourceModel
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
    private val getLocalCurrentUserUseCase: GetLocalCurrentUserUseCase
) : KoinComponent {

    suspend fun createPasswordAndDescriptionResource(
        resourceName: String,
        resourceUsername: String?,
        resourceUris: List<String>?,
        resourceParentFolderId: String?,
        password: String,
        description: String?
    ): Flow<ResourceCreateActionResult> {
        val (metadataKeyId, metadataKeyType) = getMetadataKeysParams(resourceParentFolderId)
        return createResource(
            createResource = { metadataType ->
                CreateResourceModel(
                    contentType = when (metadataType) {
                        V4 -> PasswordAndDescription
                        V5 -> V5Default
                    },
                    folderId = resourceParentFolderId,
                    expiry = null,
                    metadataKeyId = metadataKeyId,
                    metadataKeyType = metadataKeyType,
                    metadataJsonModel = MetadataJsonModel.empty()
                ).apply {
                    metadataJsonModel.name = resourceName
                    metadataJsonModel.username = resourceUsername
                    when (metadataType) {
                        V4 -> this.metadataJsonModel.uri = resourceUris?.firstOrNull()
                        V5 -> this.metadataJsonModel.uris = resourceUris
                    }
                }
            },
            createSecret = {
                SecretJsonModel.emptyPassword().apply {
                    this.secret = password
                    this.description = description
                }
            }
        )
    }

    @Suppress("LongParameterList")
    suspend fun createStandaloneTotpResource(
        resourceUsername: String?,
        resourceParentFolderId: String?,
        label: String,
        issuer: String?,
        period: Long,
        digits: Int,
        algorithm: String,
        secretKey: String
    ): Flow<ResourceCreateActionResult> {
        val (metadataKeyId, metadataKeyType) = getMetadataKeysParams(resourceParentFolderId)

        return createResource(
            createResource = { metadataType ->
                CreateResourceModel(
                    contentType = when (metadataType) {
                        V4 -> Totp
                        V5 -> V5TotpStandalone
                    },
                    folderId = resourceParentFolderId,
                    expiry = null,
                    metadataKeyId = metadataKeyId,
                    metadataKeyType = metadataKeyType,
                    metadataJsonModel = MetadataJsonModel.empty()
                ).apply {
                    metadataJsonModel.name = label
                    metadataJsonModel.username = resourceUsername
                    when (metadataType) {
                        V4 -> this.metadataJsonModel.uri = issuer
                        V5 -> this.metadataJsonModel.uris = issuer?.let { listOf(it) }
                    }
                }
            },

            createSecret = {
                SecretJsonModel.emptyTotp().apply {
                    this.totp = TotpSecret(
                        algorithm = algorithm,
                        key = secretKey,
                        digits = digits,
                        period = period
                    )
                }
            }
        )
    }

    private suspend fun getMetadataKeysParams(parentFolderId: String?): MetadataKeyParamsModel {
        val isPersonalKeyAllowed = getMetadataKeysSettingsUseCase.execute(Unit)
            .metadataKeysSettingsModel.allowUsageOfPersonalKeys
        val isParentFolderShared = parentFolderId?.let {
            getLocalFolderPermissionsUseCase.execute(
                GetLocalFolderPermissionsUseCase.Input(parentFolderId)
            ).permissions.size > 1
        } ?: false
        val metadataKeyType = if (isPersonalKeyAllowed && !isParentFolderShared) {
            MetadataKeyTypeModel.PERSONAL
        } else {
            MetadataKeyTypeModel.SHARED
        }

        val metadataKeyId = if (metadataKeyType == MetadataKeyTypeModel.SHARED) {
            getMetadataKeysUseCase.execute(GetLocalMetadataKeysUseCase.Input(ENCRYPT)).firstOrNull()?.id?.toString()
        } else {
            getLocalCurrentUserUseCase.execute(Unit).user.gpgKey.id
        }

        return MetadataKeyParamsModel(
            metadataKeyId = metadataKeyId,
            metadataKeyType = metadataKeyType
        )
    }

    // TODO: Confront with resource-types.json response
    // TODO: there can be default v5 setting but no v5 content types in response
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

                operationResult.resource.resourceModel.folderId?.let {
                    applyFolderPermissionsToCreatedResource(operationResult.resource.resourceModel, it)
                } ?: Success(
                    operationResult.resource.resourceModel.resourceId,
                    operationResult.resource.resourceModel.metadataJsonModel.name
                )
            }
            is CreateResourceInteractor.Output.JsonSchemaValidationFailure ->
                JsonSchemaValidationFailure(operationResult.entity)
        }
    }

    private suspend fun applyFolderPermissionsToCreatedResource(
        resource: ResourceModel,
        resourceParentFolderId: String
    ): ResourceCreateActionResult {
        val newPermissionsToApply = getLocalParentFolderPermissionsToApplyUseCase.execute(
            GetLocalParentFolderPermissionsToApplyToNewItemUseCase.Input(
                resourceParentFolderId,
                ItemIdResourceId(resource.resourceId)
            )
        ).permissions

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
    doOnFetchFailure: () -> Unit = {},
    doOnUnauthorized: () -> Unit = {},
    doOnShareFailure: (String) -> Unit = {}
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
        }
    }
}
