package com.passbolt.mobile.android.domain.resources.usecase

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import com.passbolt.mobile.android.common.transaction.DatabaseTransactionRunner
import com.passbolt.mobile.android.commontest.transaction.PassThroughTransactionRunner
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.architecture.result.DomainResult.Incomplete.Error.Reason.OFFLINE
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.preferences.GlobalPreferencesRepository
import com.passbolt.mobile.android.domain.resources.usecase.db.AddLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.domain.resources.usecase.db.RemoveLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.domain.resources.usecase.db.RemoveLocalResourcesWithUpdateStateUseCase
import com.passbolt.mobile.android.domain.resources.usecase.db.RemoveLocalUrisUseCase
import com.passbolt.mobile.android.domain.resources.usecase.db.SetLocalResourcesUpdateStateUseCase
import com.passbolt.mobile.android.domain.resources.usecase.db.UpsertLocalResourcesUseCase
import com.passbolt.mobile.android.domain.tags.usecase.AddLocalTagsUseCase
import com.passbolt.mobile.android.domain.tags.usecase.RemoveLocalTagsUseCase
import com.passbolt.mobile.android.jsonmodel.JSON_MODEL_GSON
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathJsonPathOps
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathsOps
import com.passbolt.mobile.android.ui.GlobalPreferencesUiModel
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.MetadataKeyTypeModel
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.ResourceUiModel
import com.passbolt.mobile.android.ui.ResourceUiModelWithAttributes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
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
import java.util.EnumSet
import kotlin.test.assertIs

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

@ExperimentalCoroutinesApi
class ResourceInteractorTest : KoinTest {
    private val resourceInteractor: ResourceInteractor by inject()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<RemoveLocalResourcePermissionsUseCase>() }
                    single { mock<RemoveLocalTagsUseCase>() }
                    single { mock<RemoveLocalUrisUseCase>() }
                    single { mock<GetResourcesPaginatedUseCase>() }
                    single { mock<UpsertLocalResourcesUseCase>() }
                    single { mock<AddLocalTagsUseCase>() }
                    single { mock<AddLocalResourcePermissionsUseCase>() }
                    single { mock<SetLocalResourcesUpdateStateUseCase>() }
                    single { mock<RemoveLocalResourcesWithUpdateStateUseCase>() }
                    single { mock<GlobalPreferencesRepository>() }
                    single { mock<GetSelectedAccountUseCase>() }
                    singleOf(::PassThroughTransactionRunner) bind DatabaseTransactionRunner::class
                    single(named(JSON_MODEL_GSON)) { Gson() }
                    single {
                        Configuration
                            .builder()
                            .jsonProvider(GsonJsonProvider())
                            .mappingProvider(GsonMappingProvider())
                            .options(EnumSet.noneOf(Option::class.java))
                            .build()
                    }
                    singleOf(::JsonPathJsonPathOps) bind JsonPathsOps::class
                    singleOf(::ResourceInteractor)
                },
            )
        }

    @Before
    fun setUp() {
        whenever(get<GetSelectedAccountUseCase>().execute(Unit))
            .doReturn(GetSelectedAccountUseCase.Output(SELECTED_ACCOUNT_ID))
        whenever(get<GlobalPreferencesRepository>().getGlobalPreferences())
            .doReturn(
                GlobalPreferencesUiModel(
                    areDebugLogsEnabled = false,
                    debugLogFileCreationDateTime = null,
                    debugLogLastAppVersion = null,
                    isHideRootDialogEnabled = false,
                    isAuthRequiredOnEveryEntry = false,
                    apiFetchPageSize = RESOURCES_PAGE_SIZE,
                    isApiFetchPageSizeManuallySet = false,
                    accessibilityPoliciesConsentGiven = true,
                    isCopyTotpOnAutofillEnabled = false,
                ),
            )
    }

    @Test
    fun `should fetch and save single page of resources`() =
        runTest {
            stubResourcesPaginatedSuccess(
                page = 1,
                resources = listOf(createResourceWithAttributes()),
                totalCount = 1,
            )

            val result = resourceInteractor.fetchAndSaveResources()

            assertIs<ResourceInteractor.Output.Success>(result)
            verify(get<SetLocalResourcesUpdateStateUseCase>()).execute(any())
            verify(get<RemoveLocalUrisUseCase>()).execute(any())
            verify(get<RemoveLocalTagsUseCase>()).execute(any())
            verify(get<RemoveLocalResourcePermissionsUseCase>()).execute(any())
            verify(get<GetResourcesPaginatedUseCase>(), times(1)).execute(any())
            verify(get<UpsertLocalResourcesUseCase>()).execute(any())
            verify(get<AddLocalTagsUseCase>()).execute(any())
            verify(get<AddLocalResourcePermissionsUseCase>()).execute(any())
            verify(get<RemoveLocalResourcesWithUpdateStateUseCase>()).execute(any())
        }

    @Test
    fun `should fetch all pages when multiple pages are available`() =
        runTest {
            val totalCount = RESOURCES_PAGE_SIZE * 2 + 500 // requires 3 pages
            stubResourcesPaginatedSuccess(page = 1, resources = listOf(createResourceWithAttributes()), totalCount = totalCount)
            stubResourcesPaginatedSuccess(page = 2, resources = listOf(createResourceWithAttributes()), totalCount = totalCount)
            stubResourcesPaginatedSuccess(page = 3, resources = listOf(createResourceWithAttributes()), totalCount = totalCount)

            val result = resourceInteractor.fetchAndSaveResources()

            assertIs<ResourceInteractor.Output.Success>(result)
            verify(get<GetResourcesPaginatedUseCase>(), times(3)).execute(any())
            verify(get<UpsertLocalResourcesUseCase>(), times(3)).execute(any())
            verify(get<AddLocalTagsUseCase>(), times(3)).execute(any())
            verify(get<AddLocalResourcePermissionsUseCase>(), times(3)).execute(any())
            verify(get<RemoveLocalResourcesWithUpdateStateUseCase>()).execute(any())
        }

    @Test
    fun `should notify page progress for each processed page`() =
        runTest {
            val totalCount = RESOURCES_PAGE_SIZE * 2 + 500 // requires 3 pages
            stubResourcesPaginatedSuccess(page = 1, resources = listOf(createResourceWithAttributes()), totalCount = totalCount)
            stubResourcesPaginatedSuccess(page = 2, resources = listOf(createResourceWithAttributes()), totalCount = totalCount)
            stubResourcesPaginatedSuccess(page = 3, resources = listOf(createResourceWithAttributes()), totalCount = totalCount)
            val pageProgress = mutableListOf<Pair<Int, Int>>()

            resourceInteractor.fetchAndSaveResources { processedPages, totalPages ->
                pageProgress.add(processedPages to totalPages)
            }

            assertThat(pageProgress).containsExactly(1 to 3, 2 to 3, 3 to 3).inOrder()
        }

    @Test
    fun `should return failure when first page fetch fails`() =
        runTest {
            stubResourcesPaginatedFailure(page = 1)

            val result = resourceInteractor.fetchAndSaveResources()

            assertIs<ResourceInteractor.Output.Failure>(result)
            verify(get<UpsertLocalResourcesUseCase>(), never()).execute(any())
            verify(get<RemoveLocalResourcesWithUpdateStateUseCase>(), never()).execute(any())
        }

    @Test
    fun `should return failure when subsequent page fetch fails`() =
        runTest {
            val totalCount = RESOURCES_PAGE_SIZE * 2 // 2 pages
            stubResourcesPaginatedSuccess(page = 1, resources = listOf(createResourceWithAttributes()), totalCount = totalCount)
            stubResourcesPaginatedFailure(page = 2)

            val result = resourceInteractor.fetchAndSaveResources()

            assertIs<ResourceInteractor.Output.Failure>(result)
            verify(get<UpsertLocalResourcesUseCase>(), times(1)).execute(any())
            verify(get<RemoveLocalResourcesWithUpdateStateUseCase>(), never()).execute(any())
        }

    @Test
    fun `should return failure with authenticated state on database exception`() =
        runTest {
            get<SetLocalResourcesUpdateStateUseCase>().stub {
                onBlocking { execute(any()) }.thenThrow(android.database.SQLException())
            }

            val result = resourceInteractor.fetchAndSaveResources()

            val failure = assertIs<ResourceInteractor.Output.Failure>(result)
            assertThat(failure.authenticationState).isEqualTo(AuthenticationState.Authenticated)
        }

    @Test
    fun `should handle empty result set`() =
        runTest {
            stubResourcesPaginatedSuccess(page = 1, resources = emptyList(), totalCount = 0)

            val result = resourceInteractor.fetchAndSaveResources()

            assertIs<ResourceInteractor.Output.Success>(result)
            verify(get<GetResourcesPaginatedUseCase>(), times(1)).execute(any())
            verify(get<RemoveLocalResourcesWithUpdateStateUseCase>()).execute(any())
        }

    private fun stubResourcesPaginatedSuccess(
        page: Int,
        resources: List<ResourceUiModelWithAttributes>,
        totalCount: Int,
    ) {
        get<GetResourcesPaginatedUseCase>().stub {
            onBlocking {
                execute(
                    GetResourcesPaginatedUseCase.Input(page = page, limit = RESOURCES_PAGE_SIZE),
                )
            }.doReturn(
                GetResourcesPaginatedUseCase.Output.Success(
                    totalCount = totalCount,
                    resources = resources,
                ),
            )
        }
    }

    private fun stubResourcesPaginatedFailure(page: Int) {
        get<GetResourcesPaginatedUseCase>().stub {
            onBlocking {
                execute(
                    GetResourcesPaginatedUseCase.Input(page = page, limit = RESOURCES_PAGE_SIZE),
                )
            }.doReturn(
                GetResourcesPaginatedUseCase.Output.Failure(
                    DomainResult.Incomplete.Error(OFFLINE, null),
                ),
            )
        }
    }

    private fun createResourceWithAttributes() =
        ResourceUiModelWithAttributes(
            resourceModel =
                ResourceUiModel(
                    resourceId = "resourceId",
                    resourceTypeId = "resourceTypeId",
                    slug = "password-and-description",
                    folderId = null,
                    permission = ResourcePermission.READ,
                    favouriteId = null,
                    modified = ZonedDateTime.now(),
                    expiry = null,
                    metadataKeyId = null,
                    metadataKeyType = MetadataKeyTypeModel.PERSONAL,
                    metadataJsonModel =
                        MetadataJsonModel(
                            "{\"name\": \"Test\", \"object_type\": \"Resource\", \"resource_type_id\": \"test\"}",
                        ),
                ),
            resourceTags = emptyList(),
            resourcePermissions = emptyList(),
            favouriteId = null,
        )

    private companion object {
        private const val SELECTED_ACCOUNT_ID = "selectedAccountId"
        private const val RESOURCES_PAGE_SIZE = 2_000
    }
}
