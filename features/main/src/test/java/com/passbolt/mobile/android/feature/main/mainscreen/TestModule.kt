package com.passbolt.mobile.android.feature.main.mainscreen

import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.core.inappreview.InAppReviewInteractor
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.main.mainscreen.encouragements.EncouragementsInteractor
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.dsl.module
import org.mockito.kotlin.mock

internal val mockInAppReviewInteractor = mock<InAppReviewInteractor>()
internal val mockFullDataRefreshExecutor = mock<FullDataRefreshExecutor>()
internal val mockGetFeatureFlagsUseCase = mock<GetFeatureFlagsUseCase>()
internal val mockEncouragementsInteractor = mock<EncouragementsInteractor>()

@OptIn(ExperimentalCoroutinesApi::class)
val testMainModule = module {
    factory<CoroutineLaunchContext> { TestCoroutineLaunchContext() }
    factory<MainContract.Presenter> {
        MainPresenter(
            inAppReviewInteractor = mockInAppReviewInteractor,
            fullDataRefreshExecutor = mockFullDataRefreshExecutor,
            getFeatureFlagsUseCase = mockGetFeatureFlagsUseCase,
            encouragementsInteractor = mockEncouragementsInteractor,
            coroutineLaunchContext = get()
        )
    }
}
