package com.passbolt.mobile.android.core.commonfolders.usecase.db

import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.database.DatabaseProvider
import com.passbolt.mobile.android.database.ResourceDatabase
import com.passbolt.mobile.android.database.impl.folders.FoldersDao
import com.passbolt.mobile.android.entity.folder.FolderUpdateState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class SetLocalFoldersUpdateStateUseCaseTest : KoinTest {
    private val useCase: SetLocalFoldersUpdateStateUseCase by inject()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<FoldersDao>() }
                    single { mock<ResourceDatabase>() }
                    single { mock<DatabaseProvider>() }
                    single { mock<GetSelectedAccountUseCase>() }
                    singleOf(::SetLocalFoldersUpdateStateUseCase)
                },
            )
        }

    @Before
    fun setUp() {
        whenever(get<ResourceDatabase>().foldersDao()).doReturn(get<FoldersDao>())
        get<DatabaseProvider>().stub {
            onBlocking { get(any()) }.doReturn(get<ResourceDatabase>())
        }
        whenever(get<GetSelectedAccountUseCase>().execute(Unit))
            .doReturn(GetSelectedAccountUseCase.Output(SELECTED_ACCOUNT_ID))
    }

    @Test
    fun `should call dao with correct update state`() =
        runTest {
            useCase.execute(SetLocalFoldersUpdateStateUseCase.Input(FolderUpdateState.PENDING))

            verify(get<FoldersDao>()).setAllUpdateState(FolderUpdateState.PENDING)
        }

    private companion object {
        private const val SELECTED_ACCOUNT_ID = "selectedAccountId"
    }
}
