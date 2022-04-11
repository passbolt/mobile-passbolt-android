package com.password.mobile.android.feature.home.filtersmenu

import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuContract
import com.passbolt.mobile.android.feature.home.screen.model.HomeDisplayView
import com.passbolt.mobile.android.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.ui.FiltersMenuModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class FiltersMenuPresenterTest : KoinTest {

    private val presenter: FiltersMenuContract.Presenter by inject()
    private val view: FiltersMenuContract.View = mock()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testFiltersMenuModule)
    }

    @Test
    fun `menu items should be visible based on feature flags`() {
        mockGetFeatureFlagsUseCase.stub {
            onBlocking { execute(Unit) } doReturn GetFeatureFlagsUseCase.Output(
                FeatureFlagsModel(
                    privacyPolicyUrl = null,
                    termsAndConditionsUrl = null,
                    isPreviewPasswordAvailable = false,
                    areFoldersAvailable = true,
                    areTagsAvailable = true
                )
            )
        }

        presenter.attach(view)
        presenter.creatingView()
        presenter.argsRetrieved(FiltersMenuModel(HomeDisplayView.AllItems))

        verify(view).addBottomSeparator()
        verify(view).addFoldersMenuItem()
        verify(view).addTagsMenuItem()
    }

    @Test
    fun `correct menu item should be selected`() {
        mockGetFeatureFlagsUseCase.stub {
            onBlocking { execute(Unit) } doReturn GetFeatureFlagsUseCase.Output(
                FeatureFlagsModel(
                    privacyPolicyUrl = null,
                    termsAndConditionsUrl = null,
                    isPreviewPasswordAvailable = false,
                    areFoldersAvailable = true,
                    areTagsAvailable = true
                )
            )
        }

        presenter.attach(view)
        presenter.creatingView()
        presenter.argsRetrieved(FiltersMenuModel(HomeDisplayView.AllItems))
        presenter.argsRetrieved(FiltersMenuModel(HomeDisplayView.Favourites))
        presenter.argsRetrieved(FiltersMenuModel(HomeDisplayView.OwnedByMe))
        presenter.argsRetrieved(FiltersMenuModel(HomeDisplayView.SharedWithMe))
        presenter.argsRetrieved(FiltersMenuModel(HomeDisplayView.RecentlyModified))
        presenter.argsRetrieved(FiltersMenuModel(HomeDisplayView.folderRoot()))
        presenter.argsRetrieved(FiltersMenuModel(HomeDisplayView.tagsRoot()))
        presenter.argsRetrieved(FiltersMenuModel(HomeDisplayView.groupsRoot()))

        verify(view).selectAllItemsItem()
        verify(view).selectFavouritesItem()
        verify(view).selectOwnedByMeItem()
        verify(view).selectSharedWithMeItem()
        verify(view).selectRecentlyModifiedItem()
        verify(view).selectFoldersMenuItem()
        verify(view).selectTagsMenuItem()
        verify(view).selectGroupsMenuItem()
    }
}
