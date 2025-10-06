package com.password.mobile.android.feature.home.switchaccount

import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.accounts.usecase.accounts.GetAllAccountsDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.entity.account.Account
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountContract
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountPresenter
import com.passbolt.mobile.android.mappers.SwitchAccountModelMapper
import com.passbolt.mobile.android.mappers.comparator.SwitchAccountUiModelComparator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

internal val accountsList =
    listOf(
        Account(
            "selected",
            "first",
            "last",
            "acc@passbolt.com",
            null,
            "pasbolt.com",
            "id1",
            "label1",
        ),
        Account(
            "second",
            "second",
            "last",
            "acc@passbolt.com",
            null,
            "pasbolt.com",
            "id2",
            "label2",
        ),
    )
internal val mockGetAllAccountsDataUseCase =
    mock<GetAllAccountsDataUseCase> {
        on { execute(Unit) }.doReturn(GetAllAccountsDataUseCase.Output(accountsList))
    }
internal val mockSignOutUseCase = mock<SignOutUseCase>()
internal val mockGetSelectedAccountUseCase = mock<GetSelectedAccountUseCase>()
internal val switchAccountModelMapper = SwitchAccountModelMapper(SwitchAccountUiModelComparator())

@ExperimentalCoroutinesApi
val testSwitchAccountModule =
    module {
        singleOf(::DataRefreshTrackingFlow)
        factory<CoroutineLaunchContext> { TestCoroutineLaunchContext() }
        factory<SwitchAccountContract.Presenter> {
            SwitchAccountPresenter(
                coroutineLaunchContext = get(),
                getAllAccountsDataUseCase = mockGetAllAccountsDataUseCase,
                switchAccountModelMapper = switchAccountModelMapper,
                signOutUseCase = mockSignOutUseCase,
                saveSelectedAccountUseCase = mock(),
                dataRefreshTrackingFlow = get(),
                getSelectedAccountUseCase = mockGetSelectedAccountUseCase,
            )
        }
    }
