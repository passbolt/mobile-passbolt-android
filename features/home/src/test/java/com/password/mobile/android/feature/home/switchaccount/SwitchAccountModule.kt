package com.password.mobile.android.feature.home.switchaccount

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.passbolt.mobile.android.comparator.SwitchAccountUiModelComparator
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.entity.account.Account
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountContract
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountPresenter
import com.passbolt.mobile.android.mappers.SwitchAccountModelMapper
import com.passbolt.mobile.android.storage.usecase.accounts.GetAllAccountsDataUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.password.mobile.android.feature.home.TestCoroutineLaunchContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.dsl.module

internal val accountsList = listOf(
    Account(
        "selected", "first", "last", "acc@passbolt.com", null, "pasbolt.com", "id1", "label1"
    ),
    Account(
        "second", "second", "last", "acc@passbolt.com", null, "pasbolt.com", "id2", "label2"
    )
)
internal val mockGetAllAccountsDataUseCase = mock<GetAllAccountsDataUseCase> {
    on { execute(Unit) }.doReturn(GetAllAccountsDataUseCase.Output(accountsList))
}
internal val mockSignOutUseCase = mock<SignOutUseCase>()
internal val mockGetSelectedAccountUseCase = mock<GetSelectedAccountUseCase>() {
    on { execute(any()) }.doReturn(GetSelectedAccountUseCase.Output("selected"))
}
internal val switchAccountModelMapper = SwitchAccountModelMapper(
    mockGetSelectedAccountUseCase, SwitchAccountUiModelComparator()
)

@ExperimentalCoroutinesApi
val testSwitchAccountModule = module {
    factory<CoroutineLaunchContext> { TestCoroutineLaunchContext() }
    factory<SwitchAccountContract.Presenter> {
        SwitchAccountPresenter(
            coroutineLaunchContext = get(),
            getAllAccountsDataUseCase = mockGetAllAccountsDataUseCase,
            switchAccountModelMapper = switchAccountModelMapper,
            signOutUseCase = mockSignOutUseCase
        )
    }
}
