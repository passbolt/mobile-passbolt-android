package com.passbolt.mobile.android.resourcemoremenu

import com.passbolt.mobile.android.core.fulldatarefresh.base.DataRefreshViewReactivePresenter
import com.passbolt.mobile.android.core.idlingresource.CreateMenuModelIdlingResource
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.resourcemoremenu.usecase.CreateResourceMoreMenuModelUseCase
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.DescriptionOption.HAS_METADATA_DESCRIPTION
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.DescriptionOption.HAS_SECURE_NOTE
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.FavouriteOption.ADD_TO_FAVOURITES
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.FavouriteOption.REMOVE_FROM_FAVOURITES
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import timber.log.Timber

class ResourceMoreMenuPresenter(
    private val createResourceMoreMenuModelUseCase: CreateResourceMoreMenuModelUseCase,
    private val menuModelIdlingResource: CreateMenuModelIdlingResource,
    coroutineLaunchContext: CoroutineLaunchContext
) : DataRefreshViewReactivePresenter<ResourceMoreMenuContract.View>(coroutineLaunchContext),
    ResourceMoreMenuContract.Presenter {

    override var view: ResourceMoreMenuContract.View? = null
    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(job + coroutineLaunchContext.ui)

    private lateinit var resourceId: String
    private lateinit var menuModel: ResourceMoreMenuModel

    override fun detach() {
        coroutineScope.coroutineContext.cancelChildren()
        super<DataRefreshViewReactivePresenter>.detach()
    }

    override fun argsRetrieved(resourceId: String) {
        this.resourceId = resourceId
    }

    override fun refreshSuccessAction() {
        menuModelIdlingResource.setIdle(false)
        coroutineScope.launch {
            try {
                menuModel = createResourceMoreMenuModelUseCase.execute(
                    CreateResourceMoreMenuModelUseCase.Input(resourceId)
                ).resourceMenuModel
                view?.showTitle(menuModel.title)
                processDynamicButtons()
                when (menuModel.favouriteOption) {
                    ADD_TO_FAVOURITES -> view?.showAddToFavouritesButton()
                    REMOVE_FROM_FAVOURITES -> view?.showRemoveFromFavouritesButton()
                }
            } catch (exception: Exception) {
                Timber.d("Resource item for the shown menu was deleted deleted $exception")
                view?.hideMenu()
            }
            menuModelIdlingResource.setIdle(true)
        }
    }

    override fun refreshFailureAction() {
        view?.showRefreshFailure()
    }

    private fun processDynamicButtons() {
        if (menuModel.canCopy) {
            view?.showCopyButton()
        }

        when (menuModel.descriptionOption) {
            HAS_SECURE_NOTE -> view?.showCopySecureNoteButton()
            HAS_METADATA_DESCRIPTION -> view?.showCopyMetadataDescriptionButton()
            null -> {
                // ignore - no description button is visible
            }
        }

        if (menuModel.canDelete || menuModel.canEdit || menuModel.canShare) {
            view?.showSeparator()
        }

        if (menuModel.canShare) {
            view?.showShareButton()
        }

        if (menuModel.canDelete) {
            view?.showDeleteButton()
        }

        if (menuModel.canEdit) {
            view?.showEditButton()
        }
    }

    override fun menuFavouriteClick() {
        view?.notifyFavouriteClick(menuModel.favouriteOption)
    }
}
