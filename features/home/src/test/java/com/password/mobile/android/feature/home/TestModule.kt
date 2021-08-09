package com.password.mobile.android.feature.home

import com.nhaarman.mockitokotlin2.mock
import com.passbolt.mobile.android.common.InitialsProvider
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.database.usecase.AddLocalResourcesUseCase
import com.passbolt.mobile.android.database.usecase.RemoveLocalResourcesUseCase
import com.passbolt.mobile.android.feature.home.screen.HomeContract
import com.passbolt.mobile.android.feature.home.screen.HomePresenter
import com.passbolt.mobile.android.feature.home.screen.usecase.GetResourcesUseCase
import com.passbolt.mobile.android.mappers.ResourceModelMapper
import com.passbolt.mobile.android.storage.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.dsl.module

internal val getResourcesUseCase = mock<GetResourcesUseCase>()
internal val getSelectedAccountDataUseCase = mock<GetSelectedAccountDataUseCase>()
internal val addLocalResourcesUseCase = mock<AddLocalResourcesUseCase>()
internal val getSelectedAccountUseCase = mock<GetSelectedAccountUseCase>()
internal val removeLocalResourcesUseCase = mock<RemoveLocalResourcesUseCase>()

@ExperimentalCoroutinesApi
val testModule = module {
    factory { getResourcesUseCase }
    factory {
        ResourceModelMapper(
            initialsProvider = get()
        )
    }
    factory { getSelectedAccountDataUseCase }
    factory { InitialsProvider() }
    factory { addLocalResourcesUseCase }
    factory { removeLocalResourcesUseCase }
    factory { getSelectedAccountUseCase }
    factory<CoroutineLaunchContext> { TestCoroutineLaunchContext() }
    factory<HomeContract.Presenter> {
        HomePresenter(
            coroutineLaunchContext = get(),
            getResourcesUseCase = get(),
            resourceModelMapper = get(),
            getSelectedAccountDataUseCase = get(),
            addLocalResourcesUseCase = get(),
            removeLocalResourcesUseCase = get(),
            getSelectedAccountUseCase = get()
        )
    }
}
