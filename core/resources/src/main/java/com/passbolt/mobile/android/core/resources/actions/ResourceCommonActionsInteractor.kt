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
import com.passbolt.mobile.android.core.resources.usecase.DeleteResourceUseCase
import com.passbolt.mobile.android.core.resources.usecase.FavouritesInteractor
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.FavouriteOption
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.FavouriteOption.ADD_TO_FAVOURITES
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.FavouriteOption.REMOVE_FROM_FAVOURITES
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import timber.log.Timber

class ResourceCommonActionsInteractor(
    private val needSessionRefreshFlow: MutableStateFlow<UnauthenticatedReason?>,
    private val sessionRefreshedFlow: StateFlow<Unit?>,
    private val resource: ResourceModel,
    private val favouritesInteractor: FavouritesInteractor,
    private val deleteResourceUseCase: DeleteResourceUseCase,
) {
    suspend fun toggleFavourite(favouriteOption: FavouriteOption): Flow<ResourceCommonActionResult> =
        when (favouriteOption) {
            ADD_TO_FAVOURITES ->
                runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                    favouritesInteractor.addToFavouritesAndUpdateLocal(resource)
                }
            REMOVE_FROM_FAVOURITES ->
                runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                    favouritesInteractor.removeFromFavouritesAndUpdateLocal(resource)
                }
        }.let { favouriteToggleOutput ->
            when (favouriteToggleOutput) {
                is FavouritesInteractor.Output.Failure -> {
                    flowOf(ResourceCommonActionResult.Failure)
                }
                is FavouritesInteractor.Output.Success -> {
                    Timber.d("Added to favourites")
                    flowOf(ResourceCommonActionResult.Success(resource.metadataJsonModel.name))
                }
            }
        }

    suspend fun deleteResource(): Flow<ResourceCommonActionResult> =
        when (
            val response =
                runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                    deleteResourceUseCase.execute(DeleteResourceUseCase.Input(resource.resourceId))
                }
        ) {
            is DeleteResourceUseCase.Output.Success -> {
                flowOf(ResourceCommonActionResult.Success(resource.metadataJsonModel.name))
            }
            is DeleteResourceUseCase.Output.Failure<*> -> {
                Timber.e(response.response.exception)
                flowOf(ResourceCommonActionResult.Failure)
            }
        }
}

suspend fun performCommonResourceAction(
    action: suspend () -> Flow<ResourceCommonActionResult>,
    doOnSuccess: (ResourceCommonActionResult.Success) -> Unit,
    doOnFailure: () -> Unit,
) {
    when (val result = action().single()) {
        is ResourceCommonActionResult.Failure -> doOnFailure()
        is ResourceCommonActionResult.Success -> doOnSuccess(result)
    }
}
