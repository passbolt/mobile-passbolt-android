package com.passbolt.mobile.android.core.resources.usecase

import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.resources.usecase.db.UpdateLocalResourceUseCase
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.isFavourite
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
class FavouritesInteractor(
    private val addToFavouritesUseCase: AddToFavouritesUseCase,
    private val removeFromFavouritesUseCase: RemoveFromFavouritesUseCase,
    private val updateLocalResourceUseCase: UpdateLocalResourceUseCase
) {

    suspend fun addToFavouritesAndUpdateLocal(resource: ResourceModel): Output {
        val addToFavouritesResult = addToFavouritesUseCase.execute(
            AddToFavouritesUseCase.Input(resource.resourceId)
        )

        return when (addToFavouritesResult) {
            is AddToFavouritesUseCase.Output.Failure<*> -> {
                Timber.e(
                    addToFavouritesResult.response.exception,
                    "Error when adding to favourites: %s",
                    addToFavouritesResult.response.headerMessage
                )
                Output.Failure(addToFavouritesResult.authenticationState)
            }
            is AddToFavouritesUseCase.Output.Success ->
                updateLocalResourceFavouriteId(addToFavouritesResult.favouriteId, resource)
        }
    }

    suspend fun removeFromFavouritesAndUpdateLocal(resource: ResourceModel): Output {
        require(resource.isFavourite())
        val removeFromFavouritesResult = removeFromFavouritesUseCase.execute(
            RemoveFromFavouritesUseCase.Input(resource.favouriteId!!)
        )

        return when (removeFromFavouritesResult) {
            is RemoveFromFavouritesUseCase.Output.Failure<*> -> {
                Timber.e(
                    removeFromFavouritesResult.response.exception,
                    "Error when removing from favourites: %s",
                    removeFromFavouritesResult.response.headerMessage
                )
                Output.Failure(removeFromFavouritesResult.authenticationState)
            }
            is RemoveFromFavouritesUseCase.Output.Success ->
                updateLocalResourceFavouriteId(favouriteId = null, resource)
        }
    }

    private suspend fun updateLocalResourceFavouriteId(favouriteId: String?, resource: ResourceModel): Output {
        updateLocalResourceUseCase.execute(
            UpdateLocalResourceUseCase.Input(
                resource.copy(favouriteId = favouriteId)
            )
        )
        return Output.Success
    }

    sealed class Output : AuthenticatedUseCaseOutput {

        object Success : Output() {
            override val authenticationState: AuthenticationState
                get() = AuthenticationState.Authenticated
        }

        class Failure(override val authenticationState: AuthenticationState) : Output()
    }
}
