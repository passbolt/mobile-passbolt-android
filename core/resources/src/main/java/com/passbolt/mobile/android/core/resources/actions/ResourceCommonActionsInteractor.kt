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
    private val deleteResourceUseCase: DeleteResourceUseCase
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
                    flowOf(ResourceCommonActionResult.Success(resource.name))
                }
            }
        }

    suspend fun deleteResource(): Flow<ResourceCommonActionResult> =
        when (val response = runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
            deleteResourceUseCase.execute(DeleteResourceUseCase.Input(resource.resourceId))
        }) {
            is DeleteResourceUseCase.Output.Success -> {
                flowOf(ResourceCommonActionResult.Success(resource.name))
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
    doOnFailure: () -> Unit
) {
    when (val result = action().single()) {
        is ResourceCommonActionResult.Failure -> doOnFailure()
        is ResourceCommonActionResult.Success -> doOnSuccess(result)
    }
}
