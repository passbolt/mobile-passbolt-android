package com.passbolt.mobile.android.feature.home.filtersmenu

import com.passbolt.mobile.android.ui.FiltersMenuModel
import com.passbolt.mobile.android.ui.HomeFilter

class FiltersMenuPresenter : FiltersMenuContract.Presenter {

    override var view: FiltersMenuContract.View? = null

    override fun argsRetrieved(menuModel: FiltersMenuModel) {
        view?.apply {
            unselectAll()
            when (menuModel.activeFilter) {
                HomeFilter.ALL -> selectAllItemsItem()
                HomeFilter.FAVOURITES -> selectFavouritesItem()
                HomeFilter.RECENTLY_MODIFIED -> selectRecentlyModifiedItem()
                HomeFilter.SHARED_WITH_ME -> selectSharedWithMeItem()
                HomeFilter.OWNED_BY_ME -> selectOwnedByMeItem()
            }
        }
    }
}
