package com.passbolt.mobile.android.feature.home.filtersmenu

import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.preferences.usecase.UpdateHomeDisplayViewPrefsUseCase
import com.passbolt.mobile.android.core.rbac.usecase.GetRbacRulesUseCase
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.dsl.module
import org.mockito.kotlin.mock

internal val mockGetFeatureFlagsUseCase = mock<GetFeatureFlagsUseCase>()
internal val mockUpdateHomeDisplayViewPrefsUseCase = mock<UpdateHomeDisplayViewPrefsUseCase>()
internal val mockGetRbacRulesUseCase = mock<GetRbacRulesUseCase>()

@ExperimentalCoroutinesApi
val testFiltersMenuModule =
    module {
        factory<CoroutineLaunchContext> { TestCoroutineLaunchContext() }
        factory<FiltersMenuContract.Presenter> {
            FiltersMenuPresenter(
                coroutineLaunchContext = get(),
                getFeatureFlagsUseCase = mockGetFeatureFlagsUseCase,
                updateHomeDisplayViewPrefsUseCase = mockUpdateHomeDisplayViewPrefsUseCase,
                getRbacRulesUseCase = mockGetRbacRulesUseCase,
            )
        }
    }
