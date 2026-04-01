package com.passbolt.mobile.android.core.commonfolders.usecase

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.RemoveLocalFoldersWithUpdateStateUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.SetLocalFoldersUpdateStateUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.UpsertLocalFoldersUseCase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.dto.response.Pagination
import com.passbolt.mobile.android.entity.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.ui.FolderModel
import com.passbolt.mobile.android.ui.FolderModelWithAttributes
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertIs

@ExperimentalCoroutinesApi
class FoldersInteractorTest : KoinTest {
    private val foldersInteractor: FoldersInteractor by inject()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<GetFeatureFlagsUseCase>() }
                    single { mock<GetFoldersPaginatedUseCase>() }
                    single { mock<SetLocalFoldersUpdateStateUseCase>() }
                    single { mock<UpsertLocalFoldersUseCase>() }
                    single { mock<RemoveLocalFoldersWithUpdateStateUseCase>() }
                    single { mock<RemoveLocalFolderPermissionsUseCase>() }
                    single { mock<AddLocalFolderPermissionsUseCase>() }
                    single { mock<GetSelectedAccountUseCase>() }
                    singleOf(::FoldersInteractor)
                },
            )
        }

    @Before
    fun setUp() {
        whenever(get<GetSelectedAccountUseCase>().execute(Unit))
            .doReturn(GetSelectedAccountUseCase.Output(SELECTED_ACCOUNT_ID))
    }

    @Test
    fun `should return success without fetching when folders feature is disabled`() =
        runTest {
            stubFoldersAvailable(false)

            val result = foldersInteractor.fetchAndSaveFolders()

            assertIs<FoldersInteractor.Output.Success>(result)
            verify(get<GetFoldersPaginatedUseCase>(), never()).execute(any())
            verify(get<SetLocalFoldersUpdateStateUseCase>(), never()).execute(any())
        }

    @Test
    fun `should fetch and save single page of folders`() =
        runTest {
            stubFoldersAvailable(true)
            stubFoldersPaginatedSuccess(
                page = 1,
                folders = listOf(FOLDER_WITH_ATTRIBUTES),
                totalCount = 1,
            )

            val result = foldersInteractor.fetchAndSaveFolders()

            assertIs<FoldersInteractor.Output.Success>(result)
            verify(get<SetLocalFoldersUpdateStateUseCase>()).execute(any())
            verify(get<RemoveLocalFolderPermissionsUseCase>()).execute(any())
            verify(get<GetFoldersPaginatedUseCase>(), times(1)).execute(any())
            verify(get<UpsertLocalFoldersUseCase>()).execute(any())
            verify(get<AddLocalFolderPermissionsUseCase>()).execute(any())
            verify(get<RemoveLocalFoldersWithUpdateStateUseCase>()).execute(any())
        }

    @Test
    fun `should fetch all pages when multiple pages are available`() =
        runTest {
            stubFoldersAvailable(true)
            val totalCount = 4500 // requires 3 pages with 2000 page size
            stubFoldersPaginatedSuccess(page = 1, folders = listOf(FOLDER_WITH_ATTRIBUTES), totalCount = totalCount)
            stubFoldersPaginatedSuccess(page = 2, folders = listOf(FOLDER_WITH_ATTRIBUTES), totalCount = totalCount)
            stubFoldersPaginatedSuccess(page = 3, folders = listOf(FOLDER_WITH_ATTRIBUTES), totalCount = totalCount)

            val result = foldersInteractor.fetchAndSaveFolders()

            assertIs<FoldersInteractor.Output.Success>(result)
            verify(get<GetFoldersPaginatedUseCase>(), times(3)).execute(any())
            verify(get<UpsertLocalFoldersUseCase>(), times(3)).execute(any())
            verify(get<AddLocalFolderPermissionsUseCase>(), times(3)).execute(any())
            verify(get<RemoveLocalFoldersWithUpdateStateUseCase>()).execute(any())
        }

    @Test
    fun `should return failure when first page fetch fails`() =
        runTest {
            stubFoldersAvailable(true)
            stubFoldersPaginatedFailure(page = 1)

            val result = foldersInteractor.fetchAndSaveFolders()

            assertIs<FoldersInteractor.Output.Failure>(result)
            verify(get<UpsertLocalFoldersUseCase>(), never()).execute(any())
            verify(get<RemoveLocalFoldersWithUpdateStateUseCase>(), never()).execute(any())
        }

    @Test
    fun `should return failure when subsequent page fetch fails`() =
        runTest {
            stubFoldersAvailable(true)
            val totalCount = 4000 // 2 pages
            stubFoldersPaginatedSuccess(page = 1, folders = listOf(FOLDER_WITH_ATTRIBUTES), totalCount = totalCount)
            stubFoldersPaginatedFailure(page = 2)

            val result = foldersInteractor.fetchAndSaveFolders()

            assertIs<FoldersInteractor.Output.Failure>(result)
            verify(get<UpsertLocalFoldersUseCase>(), times(1)).execute(any())
            verify(get<RemoveLocalFoldersWithUpdateStateUseCase>(), never()).execute(any())
        }

    @Test
    fun `should return failure with authenticated state on database exception`() =
        runTest {
            stubFoldersAvailable(true)
            get<SetLocalFoldersUpdateStateUseCase>().stub {
                onBlocking { execute(any()) }.thenThrow(android.database.SQLException())
            }

            val result = foldersInteractor.fetchAndSaveFolders()

            val failure = assertIs<FoldersInteractor.Output.Failure>(result)
            assertThat(failure.authenticationState).isEqualTo(AuthenticationState.Authenticated)
        }

    @Test
    fun `should handle empty result set`() =
        runTest {
            stubFoldersAvailable(true)
            stubFoldersPaginatedSuccess(page = 1, folders = emptyList(), totalCount = 0)

            val result = foldersInteractor.fetchAndSaveFolders()

            assertIs<FoldersInteractor.Output.Success>(result)
            verify(get<GetFoldersPaginatedUseCase>(), times(1)).execute(any())
            verify(get<RemoveLocalFoldersWithUpdateStateUseCase>()).execute(any())
        }

    private fun stubFoldersAvailable(available: Boolean) {
        get<GetFeatureFlagsUseCase>().stub {
            onBlocking { execute(any()) }.doReturn(
                GetFeatureFlagsUseCase.Output(
                    featureFlags =
                        FeatureFlagsModel(
                            privacyPolicyUrl = null,
                            termsAndConditionsUrl = null,
                            isPreviewPasswordAvailable = false,
                            areFoldersAvailable = available,
                            areTagsAvailable = false,
                            isTotpAvailable = false,
                            isRbacAvailable = false,
                            isPasswordExpiryAvailable = false,
                            arePasswordPoliciesAvailable = false,
                            canUpdatePasswordPolicies = false,
                            isV5MetadataAvailable = false,
                        ),
                ),
            )
        }
    }

    private fun stubFoldersPaginatedSuccess(
        page: Int,
        folders: List<FolderModelWithAttributes>,
        totalCount: Int,
    ) {
        get<GetFoldersPaginatedUseCase>().stub {
            onBlocking {
                execute(
                    GetFoldersPaginatedUseCase.Input(page = page, limit = FOLDERS_PAGE_SIZE),
                )
            }.doReturn(
                GetFoldersPaginatedUseCase.Output.Success(
                    pagination = Pagination(count = totalCount, page = page, limit = FOLDERS_PAGE_SIZE),
                    folders = folders,
                ),
            )
        }
    }

    private fun stubFoldersPaginatedFailure(page: Int) {
        get<GetFoldersPaginatedUseCase>().stub {
            onBlocking {
                execute(
                    GetFoldersPaginatedUseCase.Input(page = page, limit = FOLDERS_PAGE_SIZE),
                )
            }.doReturn(
                GetFoldersPaginatedUseCase.Output.Failure(
                    NetworkResult.Failure.NetworkError(
                        exception = Exception("Network error"),
                        headerMessage = "Error",
                    ),
                ),
            )
        }
    }

    private companion object {
        private const val SELECTED_ACCOUNT_ID = "selectedAccountId"
        private const val FOLDERS_PAGE_SIZE = 2_000

        private val FOLDER_WITH_ATTRIBUTES =
            FolderModelWithAttributes(
                folderModel =
                    FolderModel(
                        folderId = "folderId",
                        parentFolderId = null,
                        name = "Test Folder",
                        isShared = false,
                        permission = ResourcePermission.READ,
                    ),
                folderPermissions = emptyList(),
            )
    }
}
