package com.passbolt.mobile.android.feature.home.filtersmenu

import com.passbolt.mobile.android.ui.FiltersMenuModel
import com.passbolt.mobile.android.ui.ResourcesDisplayView

class FiltersMenuPresenter : FiltersMenuContract.Presenter {

    override var view: FiltersMenuContract.View? = null

    override fun argsRetrieved(menuModel: FiltersMenuModel) {
        view?.apply {
            unselectAll()
            when (menuModel.activeDisplayView) {
                ResourcesDisplayView.ALL -> selectAllItemsItem()
                ResourcesDisplayView.FAVOURITES -> selectFavouritesItem()
                ResourcesDisplayView.RECENTLY_MODIFIED -> selectRecentlyModifiedItem()
                ResourcesDisplayView.SHARED_WITH_ME -> selectSharedWithMeItem()
                ResourcesDisplayView.OWNED_BY_ME -> selectOwnedByMeItem()
            }
        }
    }
}
