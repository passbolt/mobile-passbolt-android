package com.password.mobile.android.feature.home.filtersmenu

import com.passbolt.mobile.android.entity.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuContract
import com.passbolt.mobile.android.storage.usecase.featureflags.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.storage.usecase.rbac.GetRbacRulesUseCase
import com.passbolt.mobile.android.ui.FiltersMenuModel
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import com.passbolt.mobile.android.ui.RbacModel
import com.passbolt.mobile.android.ui.RbacRuleModel.ALLOW
import com.passbolt.mobile.android.ui.RbacRuleModel.DENY
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
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

    @Before
    fun setup() {
        mockGetRbacRulesUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetRbacRulesUseCase.Output(
                    RbacModel(
                        passwordPreviewRule = ALLOW,
                        passwordCopyRule = ALLOW,
                        tagsUseRule = ALLOW,
                        shareViewRule = ALLOW,
                        foldersUseRule = ALLOW
                    )
                )
            )
        }
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
                    areTagsAvailable = true,
                    isTotpAvailable = true,
                    isRbacAvailable = true,
                    isPasswordExpiryAvailable = true,
                    arePasswordPoliciesAvailable = true,
                    canUpdatePasswordPolicies = true,
                    isV5MetadataAvailable = false
                )
            )
        }

        presenter.attach(view)
        presenter.creatingView()
        presenter.argsRetrieved(FiltersMenuModel(HomeDisplayViewModel.AllItems))

        verify(view).showFoldersMenuItem()
        verify(view).showTagsMenuItem()
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
                    areTagsAvailable = true,
                    isTotpAvailable = true,
                    isRbacAvailable = true,
                    isPasswordExpiryAvailable = true,
                    arePasswordPoliciesAvailable = true,
                    canUpdatePasswordPolicies = true,
                    isV5MetadataAvailable = false
                )
            )
        }

        presenter.attach(view)
        presenter.creatingView()
        presenter.argsRetrieved(FiltersMenuModel(HomeDisplayViewModel.AllItems))
        presenter.argsRetrieved(FiltersMenuModel(HomeDisplayViewModel.Favourites))
        presenter.argsRetrieved(FiltersMenuModel(HomeDisplayViewModel.OwnedByMe))
        presenter.argsRetrieved(FiltersMenuModel(HomeDisplayViewModel.SharedWithMe))
        presenter.argsRetrieved(FiltersMenuModel(HomeDisplayViewModel.RecentlyModified))
        presenter.argsRetrieved(FiltersMenuModel(HomeDisplayViewModel.folderRoot()))
        presenter.argsRetrieved(FiltersMenuModel(HomeDisplayViewModel.tagsRoot()))
        presenter.argsRetrieved(FiltersMenuModel(HomeDisplayViewModel.groupsRoot()))

        verify(view).selectAllItemsItem()
        verify(view).selectFavouritesItem()
        verify(view).selectOwnedByMeItem()
        verify(view).selectSharedWithMeItem()
        verify(view).selectRecentlyModifiedItem()
        verify(view).selectFoldersMenuItem()
        verify(view).selectTagsMenuItem()
        verify(view).selectGroupsMenuItem()
    }

    @Test
    fun `menu items should not be visible if not allowed in rbac`() {
        mockGetRbacRulesUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetRbacRulesUseCase.Output(
                    RbacModel(
                        passwordPreviewRule = ALLOW,
                        passwordCopyRule = ALLOW,
                        tagsUseRule = DENY,
                        shareViewRule = ALLOW,
                        foldersUseRule = DENY
                    )
                )
            )
        }

        presenter.attach(view)
        presenter.creatingView()
        presenter.argsRetrieved(FiltersMenuModel(HomeDisplayViewModel.AllItems))

        verify(view, never()).showFoldersMenuItem()
        verify(view, never()).showTagsMenuItem()
    }
}
