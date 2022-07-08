package com.password.mobile.android.feature.home.filtersmenu

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuContract
import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuPresenter
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.storage.usecase.preferences.UpdateAccountPreferencesUseCase
import com.password.mobile.android.feature.home.TestCoroutineLaunchContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.dsl.module
import org.mockito.kotlin.mock

internal val mockGetFeatureFlagsUseCase = mock<GetFeatureFlagsUseCase>()
internal val mockUpdateAccountPreferencesUseCase = mock<UpdateAccountPreferencesUseCase>()

@ExperimentalCoroutinesApi
val testFiltersMenuModule = module {
    factory<CoroutineLaunchContext> { TestCoroutineLaunchContext() }
    factory<FiltersMenuContract.Presenter> {
        FiltersMenuPresenter(
            coroutineLaunchContext = get(),
            getFeatureFlagsUseCase = mockGetFeatureFlagsUseCase,
            updateAccountPreferencesUseCase = mockUpdateAccountPreferencesUseCase
        )
    }
}
