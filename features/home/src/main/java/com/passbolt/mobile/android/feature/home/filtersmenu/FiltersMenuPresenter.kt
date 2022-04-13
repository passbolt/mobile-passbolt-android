package com.passbolt.mobile.android.feature.home.filtersmenu

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.home.screen.model.HomeDisplayView
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.ui.FiltersMenuModel
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
                is HomeDisplayView.AllItems -> selectAllItemsItem()
                is HomeDisplayView.Favourites -> selectFavouritesItem()
                is HomeDisplayView.RecentlyModified -> selectRecentlyModifiedItem()
                is HomeDisplayView.SharedWithMe -> selectSharedWithMeItem()
                is HomeDisplayView.OwnedByMe -> selectOwnedByMeItem()
                is HomeDisplayView.Folders -> selectFoldersMenuItem()
                is HomeDisplayView.Tags -> selectTagsMenuItem()
                is HomeDisplayView.Groups -> selectGroupsMenuItem()
            }
        }
    }

    private fun processAdditionalItemsVisibility() {
        scope.launch {
            val featureFlags = getFeatureFlagsUseCase.execute(Unit).featureFlags
            if (featureFlags.areFoldersAvailable) {
                view?.apply {
                    addBottomSeparator()
                    addFoldersMenuItem()
                }
            }
            if (featureFlags.areTagsAvailable) {
                view?.addTagsMenuItem()
            }
            view?.addGroupsMenuItem()
        }
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super.detach()
    }
}
