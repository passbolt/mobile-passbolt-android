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

import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalParentFolderPermissionsToApplyToNewItemUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.ItemIdResourceId
import com.passbolt.mobile.android.core.mvp.authentication.UnauthenticatedReason
import com.passbolt.mobile.android.core.resources.SecretInputCreator
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
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.DecryptedSecret
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
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
    private val secretInputCreator: SecretInputCreator,
    private val addLocalResourceUseCase: AddLocalResourceUseCase,
    private val addLocalResourcePermissionsUseCase: AddLocalResourcePermissionsUseCase,
    private val resourceShareInteractor: ResourceShareInteractor,
    private val getLocalParentFolderPermissionsToApplyUseCase: GetLocalParentFolderPermissionsToApplyToNewItemUseCase
) : KoinComponent {

    suspend fun createPasswordAndDescriptionResource(
        resourceName: String,
        resourceUsername: String?,
        resourceUri: String?,
        resourceParentFolderId: String?,
        password: String,
        description: String?
    ): Flow<ResourceCreateActionResult> =
        createResource(
            createResource = {
                CreateResourceInteractor.ResourceInput(
                    resourceName = resourceName,
                    resourceUsername = resourceUsername,
                    resourceUri = resourceUri,
                    resourceParentFolderId = resourceParentFolderId,
                    description = null,
                    slug = ContentType.PasswordAndDescription.slug
                )
            },
            createSecret = {
                val secretJson = secretInputCreator.createPasswordWithDescriptionSecretInput(
                    password = password,
                    description = description
                )
                DecryptedSecret(secretJson)
            }
        )

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
    ): Flow<ResourceCreateActionResult> =
        createResource(
            createResource = {
                CreateResourceInteractor.ResourceInput(
                    resourceName = label,
                    resourceUsername = resourceUsername,
                    resourceUri = issuer,
                    resourceParentFolderId = resourceParentFolderId,
                    description = null,
                    slug = ContentType.Totp.slug
                )
            },
            createSecret = {
                val secretJson = secretInputCreator.createTotpSecretInput(
                    algorithm = algorithm,
                    key = secretKey,
                    digits = digits,
                    period = period
                )
                DecryptedSecret(secretJson)
            }
        )

    private suspend fun createResource(
        createResource: () -> CreateResourceInteractor.ResourceInput,
        createSecret: () -> DecryptedSecret
    ): Flow<ResourceCreateActionResult> {
        return try {
            flowOf(
                runCreateOperation {
                    createResourceInteractor.execute(
                        resourceInput = createResource(),
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
                    operationResult.resource.resourceModel.name
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
            is ResourceShareInteractor.Output.Success -> Success(resource.resourceId, resource.name)
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
