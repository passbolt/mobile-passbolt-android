package com.passbolt.mobile.android.feature.home.filtersmenu

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.entity.home.HomeDisplayView
import com.passbolt.mobile.android.feature.home.screen.model.HomeDisplayViewModel
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.storage.usecase.preferences.UpdateAccountPreferencesUseCase
import com.passbolt.mobile.android.ui.FiltersMenuModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class FiltersMenuPresenter(
    private val getFeatureFlagsUseCase: GetFeatureFlagsUseCase,
    private val updateAccountPreferencesUseCase: UpdateAccountPreferencesUseCase,
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
                is HomeDisplayViewModel.AllItems -> selectAllItemsItem()
                is HomeDisplayViewModel.Favourites -> selectFavouritesItem()
                is HomeDisplayViewModel.RecentlyModified -> selectRecentlyModifiedItem()
                is HomeDisplayViewModel.SharedWithMe -> selectSharedWithMeItem()
                is HomeDisplayViewModel.OwnedByMe -> selectOwnedByMeItem()
                is HomeDisplayViewModel.Folders -> selectFoldersMenuItem()
                is HomeDisplayViewModel.Tags -> selectTagsMenuItem()
                is HomeDisplayViewModel.Groups -> selectGroupsMenuItem()
            }
        }
    }

    private fun processAdditionalItemsVisibility() {
        scope.launch {
            val featureFlags = getFeatureFlagsUseCase.execute(Unit).featureFlags
            if (featureFlags.areFoldersAvailable) {
                view?.showFoldersMenuItem()
            }
            if (featureFlags.areTagsAvailable) {
                view?.showTagsMenuItem()
            }
        }
    }

    private fun saveLastUsedHomeView(lastUsedHomeView: HomeDisplayView) {
        updateAccountPreferencesUseCase.execute(
            UpdateAccountPreferencesUseCase.Input(lastUsedHomeView = lastUsedHomeView)
        )
    }

    // TODO move home view filters to recycler view - replace click methods with one taking in model - MOB-492
    override fun allItemsClick() {
        saveLastUsedHomeView(HomeDisplayView.ALL_ITEMS)
    }

    override fun favouritesClick() {
        saveLastUsedHomeView(HomeDisplayView.FAVOURITES)
    }

    override fun recentlyModifiedClick() {
        saveLastUsedHomeView(HomeDisplayView.RECENTLY_MODIFIED)
    }

    override fun sharedWithMeClick() {
        saveLastUsedHomeView(HomeDisplayView.SHARED_WITH_ME)
    }

    override fun ownedByMeClick() {
        saveLastUsedHomeView(HomeDisplayView.OWNED_BY_ME)
    }

    override fun foldersClick() {
        saveLastUsedHomeView(HomeDisplayView.FOLDERS)
    }

    override fun tagsClick() {
        saveLastUsedHomeView(HomeDisplayView.TAGS)
    }

    override fun groupsClick() {
        saveLastUsedHomeView(HomeDisplayView.GROUPS)
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super.detach()
    }
}
