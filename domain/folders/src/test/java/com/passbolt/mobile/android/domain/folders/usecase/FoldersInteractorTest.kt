/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

package com.passbolt.mobile.android.domain.folders.usecase

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.common.transaction.DatabaseTransactionRunner
import com.passbolt.mobile.android.commontest.transaction.PassThroughTransactionRunner
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.architecture.result.DomainResult.Incomplete.Error.Reason.OFFLINE
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.domain.folders.model.FolderModel
import com.passbolt.mobile.android.domain.folders.model.FolderModelWithAttributes
import com.passbolt.mobile.android.domain.preferences.GlobalPreferencesRepository
import com.passbolt.mobile.android.entity.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.ui.GlobalPreferencesUiModel
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
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
import java.time.ZonedDateTime
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
                    single { mock<UpdateLocalFoldersIsSharedUseCase>() }
                    single { mock<GlobalPreferencesRepository>() }
                    singleOf(::PassThroughTransactionRunner) bind DatabaseTransactionRunner::class
                    singleOf(::FoldersInteractor)
                },
            )
        }

    @Before
    fun setUp() {
        whenever(get<GlobalPreferencesRepository>().getGlobalPreferences())
            .doReturn(
                GlobalPreferencesUiModel(
                    areDebugLogsEnabled = false,
                    debugLogFileCreationDateTime = null,
                    debugLogLastAppVersion = null,
                    isHideRootDialogEnabled = false,
                    isAuthRequiredOnEveryEntry = false,
                    apiFetchPageSize = FOLDERS_PAGE_SIZE,
                    isApiFetchPageSizeManuallySet = false,
                    accessibilityPoliciesConsentGiven = true,
                    isCopyTotpOnAutofillEnabled = false,
                ),
            )
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
            val totalCount = FOLDERS_PAGE_SIZE * 2 + 500 // requires 3 pages
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
    fun `should notify page progress for each processed page`() =
        runTest {
            stubFoldersAvailable(true)
            val totalCount = FOLDERS_PAGE_SIZE * 2 + 500 // requires 3 pages
            stubFoldersPaginatedSuccess(page = 1, folders = listOf(FOLDER_WITH_ATTRIBUTES), totalCount = totalCount)
            stubFoldersPaginatedSuccess(page = 2, folders = listOf(FOLDER_WITH_ATTRIBUTES), totalCount = totalCount)
            stubFoldersPaginatedSuccess(page = 3, folders = listOf(FOLDER_WITH_ATTRIBUTES), totalCount = totalCount)
            val pageProgress = mutableListOf<Pair<Int, Int>>()

            foldersInteractor.fetchAndSaveFolders { processedPages, totalPages ->
                pageProgress.add(processedPages to totalPages)
            }

            assertThat(pageProgress).containsExactly(1 to 3, 2 to 3, 3 to 3).inOrder()
        }

    @Test
    fun `should not notify page progress when folders feature is disabled`() =
        runTest {
            stubFoldersAvailable(false)
            val pageProgress = mutableListOf<Pair<Int, Int>>()

            foldersInteractor.fetchAndSaveFolders { processedPages, totalPages ->
                pageProgress.add(processedPages to totalPages)
            }

            assertThat(pageProgress).isEmpty()
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
            val totalCount = FOLDERS_PAGE_SIZE * 2 // 2 pages
            stubFoldersPaginatedSuccess(page = 1, folders = listOf(FOLDER_WITH_ATTRIBUTES), totalCount = totalCount)
            stubFoldersPaginatedFailure(page = 2)

            val result = foldersInteractor.fetchAndSaveFolders()

            assertIs<FoldersInteractor.Output.Failure>(result)
            verify(get<UpsertLocalFoldersUseCase>(), times(1)).execute(any())
            verify(get<RemoveLocalFoldersWithUpdateStateUseCase>(), never()).execute(any())
        }

    @Test
    fun `should return authenticated failure on database exception`() =
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
                    totalCount = totalCount,
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
                    DomainResult.Incomplete.Error(OFFLINE, null),
                ),
            )
        }
    }

    private companion object {
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
                        modified = ZonedDateTime.now(),
                    ),
                folderPermissions = emptyList(),
            )
    }
}
