package com.password.mobile.android.feature.home.filtersmenu

import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuContract
import com.passbolt.mobile.android.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.ui.FiltersMenuModel
import com.passbolt.mobile.android.ui.ResourcesDisplayView
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
        presenter.argsRetrieved(FiltersMenuModel(ResourcesDisplayView.ALL))

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
        presenter.argsRetrieved(FiltersMenuModel(ResourcesDisplayView.ALL))
        presenter.argsRetrieved(FiltersMenuModel(ResourcesDisplayView.FAVOURITES))
        presenter.argsRetrieved(FiltersMenuModel(ResourcesDisplayView.FOLDERS))
        presenter.argsRetrieved(FiltersMenuModel(ResourcesDisplayView.OWNED_BY_ME))
        presenter.argsRetrieved(FiltersMenuModel(ResourcesDisplayView.SHARED_WITH_ME))
        presenter.argsRetrieved(FiltersMenuModel(ResourcesDisplayView.RECENTLY_MODIFIED))

        verify(view).selectAllItemsItem()
        verify(view).selectFavouritesItem()
        verify(view).selectFoldersMenuItem()
        verify(view).selectOwnedByMeItem()
        verify(view).selectSharedWithMeItem()
        verify(view).selectRecentlyModifiedItem()
    }
}
