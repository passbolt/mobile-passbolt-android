package com.password.mobile.android.feature.home

import com.nhaarman.mockitokotlin2.mock
import com.passbolt.mobile.android.common.InitialsProvider
import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.core.commonresource.ResourceInteractor
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.autofill.resources.FetchAndUpdateDatabaseUseCase
import com.passbolt.mobile.android.feature.home.screen.HomeContract
import com.passbolt.mobile.android.feature.home.screen.HomePresenter
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.mappers.ResourceModelMapper
import com.passbolt.mobile.android.storage.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.dsl.module

internal val resourcesInteractor = mock<ResourceInteractor>()
internal val getSelectedAccountDataUseCase = mock<GetSelectedAccountDataUseCase>()
internal val getSelectedAccountUseCase = mock<GetSelectedAccountUseCase>()
internal val fetchAndUpdateDatabaseUseCase = mock<FetchAndUpdateDatabaseUseCase>()
internal val mockSecretInteractor = mock<SecretInteractor>()

@ExperimentalCoroutinesApi
val testHomeModule = module {
    factory { resourcesInteractor }
    factory {
        ResourceModelMapper(
            initialsProvider = get()
        )
    }
    factory { getSelectedAccountDataUseCase }
    factory { fetchAndUpdateDatabaseUseCase }
    factory { InitialsProvider() }
    factory<CoroutineLaunchContext> { TestCoroutineLaunchContext() }
    factory { SearchableMatcher() }
    factory<HomeContract.Presenter> {
        HomePresenter(
            coroutineLaunchContext = get(),
            resourcesInteractor = get(),
            resourceModelMapper = get(),
            getSelectedAccountDataUseCase = get(),
            fetchAndUpdateDatabaseUseCase = get(),
            secretInteractor = mockSecretInteractor,
            resourceMatcher = get()
        )
    }
}
