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
import com.passbolt.mobile.android.core.resources.interactor.update.UpdateLinkedTotpResourceInteractor
import com.passbolt.mobile.android.core.resources.interactor.update.UpdatePasswordAndDescriptionResourceInteractor
import com.passbolt.mobile.android.core.resources.interactor.update.UpdateResourceInteractor
import com.passbolt.mobile.android.core.resources.interactor.update.UpdateSimplePasswordResourceInteractor
import com.passbolt.mobile.android.core.resources.interactor.update.UpdateStandaloneTotpResourceInteractor
import com.passbolt.mobile.android.core.resources.usecase.db.UpdateLocalResourceUseCase
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import org.koin.core.component.KoinComponent

class ResourceUpdateActionsInteractor(
    private val resource: ResourceModel,
    private val needSessionRefreshFlow: MutableStateFlow<UnauthenticatedReason?>,
    private val sessionRefreshedFlow: StateFlow<Unit?>,
    private val secretPropertiesActionsInteractor: SecretPropertiesActionsInteractor,
    private val updatePasswordAndDescriptionResourceInteractor: UpdatePasswordAndDescriptionResourceInteractor,
    private val updateLinkedTotpResourceInteractor: UpdateLinkedTotpResourceInteractor,
    private val updateSimplePasswordResourceInteractor: UpdateSimplePasswordResourceInteractor,
    private val updateStandaloneTotpResourceInteractor: UpdateStandaloneTotpResourceInteractor,
    private val updateLocalResourceUseCase: UpdateLocalResourceUseCase
) : KoinComponent {

    suspend fun downgradeToPasswordAndDescriptionResource(): Flow<ResourceUpdateActionResult> {
        val password = secretPropertiesActionsInteractor.providePassword().single()
        val description = secretPropertiesActionsInteractor.provideDescription().single()
        return flowOf(
            when {
                password is SecretPropertyActionResult.Success && description is SecretPropertyActionResult.Success ->
                    runUpdateOperation {
                        updatePasswordAndDescriptionResourceInteractor.execute(
                            UpdateResourceInteractor.CommonInput(
                                resourceId = resource.resourceId,
                                resourceName = resource.name,
                                resourceUsername = resource.username,
                                resourceUri = resource.url,
                                resourceParentFolderId = resource.folderId
                            ),
                            UpdatePasswordAndDescriptionResourceInteractor.UpdatePasswordAndDescriptionInput(
                                password = password.result,
                                description = description.result
                            )
                        )
                    }
                listOf(password, description).any { it is SecretPropertyActionResult.FetchFailure } ->
                    ResourceUpdateActionResult.FetchFailure
                listOf(password, description).any { it is SecretPropertyActionResult.DecryptionFailure } ->
                    ResourceUpdateActionResult.CryptoFailure()
                else -> ResourceUpdateActionResult.Failure()
            }
        )
    }

    suspend fun updatePasswordAndDescriptionResource(
        resourceName: String,
        resourceUsername: String?,
        resourceUri: String?,
        resourceParentFolderId: String?,
        password: String,
        description: String?
    ): Flow<ResourceUpdateActionResult> {
        return flowOf(
            runUpdateOperation {
                updatePasswordAndDescriptionResourceInteractor.execute(
                    UpdateResourceInteractor.CommonInput(
                        resourceId = resource.resourceId,
                        resourceName = resourceName,
                        resourceUsername = resourceUsername,
                        resourceUri = resourceUri,
                        resourceParentFolderId = resourceParentFolderId
                    ),
                    UpdatePasswordAndDescriptionResourceInteractor.UpdatePasswordAndDescriptionInput(
                        password = password,
                        description = description
                    )
                )
            }
        )
    }

    suspend fun updateStandaloneTotpResource(
        label: String,
        issuer: String?,
        period: Long,
        digits: Int,
        algorithm: String,
        secretKey: String
    ): Flow<ResourceUpdateActionResult> {
        return flowOf(
            runUpdateOperation {
                updateStandaloneTotpResourceInteractor.execute(
                    UpdateResourceInteractor.CommonInput(
                        resourceId = resource.resourceId,
                        resourceName = label,
                        resourceUsername = resource.username,
                        resourceUri = issuer,
                        resourceParentFolderId = resource.folderId
                    ),
                    UpdateStandaloneTotpResourceInteractor.UpdateStandaloneTotpInput(
                        period = period,
                        digits = digits,
                        algorithm = algorithm,
                        secretKey = secretKey
                    )
                )
            }
        )
    }

    suspend fun updateSimplePasswordResource(
        resourceName: String,
        resourceUsername: String?,
        resourceUri: String?,
        resourceParentFolderId: String?,
        password: String,
        description: String?
    ): Flow<ResourceUpdateActionResult> {
        return flowOf(
            runUpdateOperation {
                updateSimplePasswordResourceInteractor.execute(
                    UpdateResourceInteractor.CommonInput(
                        resourceId = resource.resourceId,
                        resourceName = resourceName, // validated to be not null
                        resourceUsername = resourceUsername,
                        resourceUri = resourceUri,
                        resourceParentFolderId = resourceParentFolderId
                    ),
                    UpdateSimplePasswordResourceInteractor.UpdateSimplePasswordInput(
                        password = password,
                        description = description
                    )
                )
            }
        )
    }

    suspend fun updateLinkedTotpResourceTotpFields(
        label: String,
        issuer: String?,
        period: Long,
        digits: Int,
        algorithm: String,
        secretKey: String
    ): Flow<ResourceUpdateActionResult> {
        val passwordAndDescriptionSecret =
            secretPropertiesActionsInteractor.providePasswordAndSecretDescription().single()
        return flowOf(
            when (passwordAndDescriptionSecret) {
                is SecretPropertyActionResult.DecryptionFailure -> ResourceUpdateActionResult.CryptoFailure()
                is SecretPropertyActionResult.FetchFailure -> ResourceUpdateActionResult.FetchFailure
                is SecretPropertyActionResult.Unauthorized -> ResourceUpdateActionResult.Unauthorized
                is SecretPropertyActionResult.Success -> {
                    runUpdateOperation {
                        updateLinkedTotpResourceInteractor.execute(
                            UpdateResourceInteractor.CommonInput(
                                resourceId = resource.resourceId,
                                resourceName = label,
                                resourceUsername = resource.username,
                                resourceUri = issuer,
                                resourceParentFolderId = resource.folderId
                            ),
                            UpdateLinkedTotpResourceInteractor.UpdateToLinkedTotpInput(
                                period = period,
                                digits = digits,
                                algorithm = algorithm,
                                secretKey = secretKey,
                                description = passwordAndDescriptionSecret.result.description.orEmpty(),
                                password = passwordAndDescriptionSecret.result.password
                            )
                        )
                    }
                }
            }
        )
    }

    suspend fun updateLinkedTotpResourcePasswordFields(
        resourceName: String,
        resourceUsername: String?,
        resourceUri: String?,
        resourceParentFolderId: String?,
        password: String,
        description: String?
    ): Flow<ResourceUpdateActionResult> {
        val otpSecret = secretPropertiesActionsInteractor.provideOtp().single()
        return flowOf(
            when (otpSecret) {
                is SecretPropertyActionResult.DecryptionFailure -> ResourceUpdateActionResult.CryptoFailure()
                is SecretPropertyActionResult.FetchFailure -> ResourceUpdateActionResult.FetchFailure
                is SecretPropertyActionResult.Unauthorized -> ResourceUpdateActionResult.Unauthorized
                is SecretPropertyActionResult.Success ->
                    runUpdateOperation {
                        updateLinkedTotpResourceInteractor.execute(
                            UpdateResourceInteractor.CommonInput(
                                resourceId = resource.resourceId,
                                resourceName = resourceName,
                                resourceUsername = resourceUsername,
                                resourceUri = resourceUri,
                                resourceParentFolderId = resourceParentFolderId
                            ),
                            UpdateLinkedTotpResourceInteractor.UpdateToLinkedTotpInput(
                                period = otpSecret.result.period,
                                digits = otpSecret.result.digits,
                                algorithm = otpSecret.result.algorithm,
                                secretKey = otpSecret.result.key,
                                description = description.orEmpty(),
                                password = password
                            )
                        )
                    }
            }
        )
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
        }
    }
}

suspend fun performResourceUpdateAction(
    action: suspend () -> Flow<ResourceUpdateActionResult>,
    doOnCryptoFailure: (String) -> Unit,
    doOnFailure: (String) -> Unit,
    doOnSuccess: (ResourceUpdateActionResult.Success) -> Unit,
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
        }
    }
}
