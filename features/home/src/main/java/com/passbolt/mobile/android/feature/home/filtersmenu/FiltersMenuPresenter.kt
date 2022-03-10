package com.passbolt.mobile.android.feature.home.filtersmenu

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.ui.FiltersMenuModel
import com.passbolt.mobile.android.ui.ResourcesDisplayView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class FiltersMenuPresenter(
    private val getFeatureFlagsUseCase: GetFeatureFlagsUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : FiltersMenuContract.Presenter {

    override var view: FiltersMenuContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun creatingView() {
        processAdditionalItemsVisibility()
    }

    override fun argsRetrieved(menuModel: FiltersMenuModel) {
        scope.launch {
            selectCurrentlyActiveDisplayView(menuModel)
        }
    }

    private fun selectCurrentlyActiveDisplayView(menuModel: FiltersMenuModel) {
        view?.apply {
            unselectAll()
            when (menuModel.activeDisplayView) {
                ResourcesDisplayView.ALL -> selectAllItemsItem()
                ResourcesDisplayView.FAVOURITES -> selectFavouritesItem()
                ResourcesDisplayView.RECENTLY_MODIFIED -> selectRecentlyModifiedItem()
                ResourcesDisplayView.SHARED_WITH_ME -> selectSharedWithMeItem()
                ResourcesDisplayView.OWNED_BY_ME -> selectOwnedByMeItem()
                ResourcesDisplayView.FOLDERS -> selectFoldersMenuItem()
            }
        }
    }

    private fun processAdditionalItemsVisibility() {
        scope.launch {
            if (getFeatureFlagsUseCase.execute(Unit).featureFlags.areFoldersAvailable) {
                view?.apply {
                    addBottomSeparator()
                    addFoldersMenuItem()
                }
            }
        }
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super.detach()
    }
}
