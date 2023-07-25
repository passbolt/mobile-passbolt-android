package com.passbolt.mobile.android.core.resources.actions

import androidx.annotation.VisibleForTesting
import com.passbolt.mobile.android.common.types.ClipboardLabel
import com.passbolt.mobile.android.common.types.ResourceName
import com.passbolt.mobile.android.core.mvp.authentication.UnauthenticatedReason
import com.passbolt.mobile.android.core.resources.usecase.DeleteResourceUseCase
import com.passbolt.mobile.android.core.resources.usecase.FavouritesInteractor
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.PASSWORD_DESCRIPTION_TOTP
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.PASSWORD_WITH_DESCRIPTION
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.SIMPLE_PASSWORD
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.STANDALONE_TOTP
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.DecryptedSecret
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretParser
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.ui.DecryptedSecretOrError
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

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

typealias IsSecret = Boolean

class ResourceAuthenticatedActionsInteractor(
    private val needSessionRefreshFlow: MutableStateFlow<UnauthenticatedReason?>,
    private val sessionRefreshedFlow: StateFlow<Unit?>,
    private val resource: ResourceModel,
    private val resourceTypeFactory: ResourceTypeFactory,
    private val secretParser: SecretParser,
    private val secretInteractor: SecretInteractor,
    private val favouritesInteractor: FavouritesInteractor,
    private val deleteResourceUseCase: DeleteResourceUseCase
) {

    suspend fun provideDescription(
        decryptionFailure: () -> Unit,
        fetchFailure: () -> Unit,
        success: (ClipboardLabel, String, IsSecret) -> Unit
    ) {
        when (val resourceTypeEnum = resourceTypeFactory.getResourceTypeEnum(resource.resourceTypeId)) {
            SIMPLE_PASSWORD -> {
                resource.description?.let {
                    success(DESCRIPTION_LABEL, it, false)
                }
            }
            PASSWORD_WITH_DESCRIPTION, PASSWORD_DESCRIPTION_TOTP -> {
                fetchAndDecrypt(decryptionFailure, fetchFailure) {
                    when (val description = secretParser.extractDescription(resourceTypeEnum, it)) {
                        is DecryptedSecretOrError.DecryptedSecret -> success(
                            DESCRIPTION_LABEL,
                            description.secret,
                            true
                        )
                        is DecryptedSecretOrError.Error -> decryptionFailure()
                    }
                }
            }
            STANDALONE_TOTP -> {
                Timber.e("Standalone totp resource does not contain description field")
                decryptionFailure()
            }
        }
    }

    suspend fun providePassword(
        decryptionFailure: () -> Unit,
        fetchFailure: () -> Unit,
        success: (ClipboardLabel, String) -> Unit
    ) {
        fetchAndDecrypt(decryptionFailure, fetchFailure) {
            val resourceTypeEnum = resourceTypeFactory.getResourceTypeEnum(resource.resourceTypeId)
            when (val password = secretParser.extractPassword(resourceTypeEnum, it)) {
                is DecryptedSecretOrError.DecryptedSecret -> success(SECRET_LABEL, password.secret)
                is DecryptedSecretOrError.Error -> decryptionFailure()
            }
        }
    }

    suspend fun provideOtp(
        decryptionFailure: () -> Unit,
        fetchFailure: () -> Unit,
        success: (ClipboardLabel, DecryptedSecret.StandaloneTotp.Totp) -> Unit
    ) {
        fetchAndDecrypt(decryptionFailure, fetchFailure) {
            val resourceTypeEnum = resourceTypeFactory.getResourceTypeEnum(resource.resourceTypeId)
            when (val totp = secretParser.extractTotpData(resourceTypeEnum, it)) {
                is DecryptedSecretOrError.DecryptedSecret -> success(OTP_LABEL, totp.secret)
                is DecryptedSecretOrError.Error -> decryptionFailure()
            }
        }
    }

    private suspend fun fetchAndDecrypt(
        decryptionFailure: () -> Unit,
        fetchFailure: () -> Unit,
        success: suspend (ByteArray) -> Unit
    ) {
        when (val output =
            runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                secretInteractor.fetchAndDecrypt(resource.resourceId)
            }
        ) {
            is SecretInteractor.Output.DecryptFailure -> decryptionFailure()
            is SecretInteractor.Output.FetchFailure -> fetchFailure()
            is SecretInteractor.Output.Success -> {
                success(output.decryptedSecret)
            }
            is SecretInteractor.Output.Unauthorized -> {
                // can be ignored - runAuthenticatedOperation handles it
                Timber.d("Unauthorized during decrypting secret")
            }
        }
    }

    suspend fun toggleFavourite(
        favouriteOption: ResourceMoreMenuModel.FavouriteOption,
        failure: () -> Unit,
        success: (ResourceName) -> Unit
    ) {
        val output = when (favouriteOption) {
            ResourceMoreMenuModel.FavouriteOption.ADD_TO_FAVOURITES ->
                runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                    favouritesInteractor.addToFavouritesAndUpdateLocal(resource)
                }
            ResourceMoreMenuModel.FavouriteOption.REMOVE_FROM_FAVOURITES ->
                runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                    favouritesInteractor.removeFromFavouritesAndUpdateLocal(resource)
                }
        }

        when (output) {
            is FavouritesInteractor.Output.Failure -> failure()
            is FavouritesInteractor.Output.Success -> {
                Timber.d("Added to favourites")
                success(resource.name)
            }
        }
    }

    suspend fun deleteResource(
        failure: () -> Unit,
        success: (ResourceName) -> Unit
    ) {
        when (val response = runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
            deleteResourceUseCase.execute(DeleteResourceUseCase.Input(resource.resourceId))
        }) {
            is DeleteResourceUseCase.Output.Success -> {
                success(resource.name)
            }
            is DeleteResourceUseCase.Output.Failure<*> -> {
                Timber.e(response.response.exception)
                failure()
            }
        }
    }

    companion object {
        @VisibleForTesting
        const val SECRET_LABEL = "Secret"

        @VisibleForTesting
        const val DESCRIPTION_LABEL = "Description"

        @VisibleForTesting
        const val OTP_LABEL = "TOTP"
    }
}
