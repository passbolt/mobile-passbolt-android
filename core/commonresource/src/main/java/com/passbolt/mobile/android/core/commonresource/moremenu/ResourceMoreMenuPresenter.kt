package com.passbolt.mobile.android.core.commonresource.moremenu

import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.FavouriteOption.ADD_TO_FAVOURITES
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.FavouriteOption.REMOVE_FROM_FAVOURITES

class ResourceMoreMenuPresenter : ResourceMoreMenuContract.Presenter {
    override var view: ResourceMoreMenuContract.View? = null

    override fun argsRetrieved(menuModel: ResourceMoreMenuModel) {
        view?.showTitle(menuModel.title)
        processEditAndDeleteButtons(menuModel)
        when (menuModel.favouriteOption) {
            ADD_TO_FAVOURITES -> view?.showAddToFavouritesButton()
            REMOVE_FROM_FAVOURITES -> view?.showRemoveFromFavouritesButton()
        }
    }

    private fun processEditAndDeleteButtons(menuModel: ResourceMoreMenuModel) {
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
}
