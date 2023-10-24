package com.password.mobile.android.feature.home.filtersmenu

import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuContract
import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuPresenter
import com.passbolt.mobile.android.storage.usecase.featureflags.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.storage.usecase.preferences.UpdateHomeDisplayViewPrefsUseCase
import com.passbolt.mobile.android.storage.usecase.rbac.GetRbacRulesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.dsl.module
import org.mockito.kotlin.mock

internal val mockGetFeatureFlagsUseCase = mock<GetFeatureFlagsUseCase>()
internal val mockUpdateHomeDisplayViewPrefsUseCase = mock<UpdateHomeDisplayViewPrefsUseCase>()
internal val mockGetRbacRulesUseCase = mock<GetRbacRulesUseCase>()

@ExperimentalCoroutinesApi
val testFiltersMenuModule = module {
    factory<CoroutineLaunchContext> { TestCoroutineLaunchContext() }
    factory<FiltersMenuContract.Presenter> {
        FiltersMenuPresenter(
            coroutineLaunchContext = get(),
            getFeatureFlagsUseCase = mockGetFeatureFlagsUseCase,
            updateHomeDisplayViewPrefsUseCase = mockUpdateHomeDisplayViewPrefsUseCase,
            getRbacRulesUseCase = mockGetRbacRulesUseCase
        )
    }
}
