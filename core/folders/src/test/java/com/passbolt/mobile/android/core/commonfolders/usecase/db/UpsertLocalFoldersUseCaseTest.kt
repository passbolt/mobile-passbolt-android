package com.passbolt.mobile.android.core.commonfolders.usecase.db

import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.database.DatabaseProvider
import com.passbolt.mobile.android.database.ResourceDatabase
import com.passbolt.mobile.android.database.impl.folders.FoldersDao
import com.passbolt.mobile.android.entity.folder.Folder
import com.passbolt.mobile.android.entity.folder.FolderUpdateState
import com.passbolt.mobile.android.entity.resource.Permission
import com.passbolt.mobile.android.mappers.FolderModelMapper
import com.passbolt.mobile.android.ui.FolderModel
import com.passbolt.mobile.android.ui.ResourcePermission
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class UpsertLocalFoldersUseCaseTest : KoinTest {
    private val useCase: UpsertLocalFoldersUseCase by inject()

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
                    single { mock<FolderModelMapper>() }
                    singleOf(::UpsertLocalFoldersUseCase)
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
    fun `should map folders with updated state and call dao upsert`() =
        runTest {
            val folderModel =
                FolderModel(
                    folderId = "folderId",
                    parentFolderId = null,
                    name = "Test Folder",
                    isShared = false,
                    permission = ResourcePermission.READ,
                )
            val mappedFolder =
                Folder(
                    folderId = "folderId",
                    name = "Test Folder",
                    permission = Permission.READ,
                    parentId = null,
                    isShared = false,
                    updateState = FolderUpdateState.UPDATED,
                )
            get<FolderModelMapper>().stub {
                on { map(any<FolderModel>(), eq(FolderUpdateState.UPDATED)) }.doReturn(mappedFolder)
            }

            useCase.execute(UpsertLocalFoldersUseCase.Input(listOf(folderModel)))

            verify(get<FolderModelMapper>()).map(folderModel, folderUpdateState = FolderUpdateState.UPDATED)
            verify(get<FoldersDao>()).upsertAll(listOf(mappedFolder))
        }

    private companion object {
        private const val SELECTED_ACCOUNT_ID = "selectedAccountId"
    }
}
