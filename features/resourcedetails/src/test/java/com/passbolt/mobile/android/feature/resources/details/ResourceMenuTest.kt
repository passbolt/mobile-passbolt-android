package com.passbolt.mobile.android.feature.resources.details

import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderLocationUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.DataRefreshStatus
import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.core.fulldatarefresh.HomeDataInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourceCommonActionResult
import com.passbolt.mobile.android.core.resources.actions.ResourcePropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourcePropertyActionResult
import com.passbolt.mobile.android.core.resources.usecase.FavouritesInteractor
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceTagsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetResourceTypeIdToSlugMappingUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetResourceTypeWithFieldsByIdUseCase
import com.passbolt.mobile.android.entity.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.entity.resource.ResourceField
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsContract
import com.passbolt.mobile.android.storage.usecase.featureflags.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.PASSWORD_AND_DESCRIPTION_SLUG
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.TagModel
import com.passbolt.mobile.android.ui.UserWithAvatar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import java.util.UUID

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

class ResourceMenuTest : KoinTest {

    private val presenter: ResourceDetailsContract.Presenter by inject()
    private val view: ResourceDetailsContract.View = mock()
    private val mockFullDataRefreshExecutor: FullDataRefreshExecutor by inject()

    @ExperimentalCoroutinesApi
    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testResourceDetailsModule)
    }

    @Before
    fun setup() {
        mockGetLocalResourceUseCase.stub {
            onBlocking { execute(GetLocalResourceUseCase.Input(RESOURCE_MODEL.resourceId)) }
                .doReturn(GetLocalResourceUseCase.Output(RESOURCE_MODEL))
        }
        mockGetLocalResourcePermissionsUseCase.stub {
            onBlocking { execute(GetLocalResourcePermissionsUseCase.Input(RESOURCE_MODEL.resourceId)) }
                .doReturn(GetLocalResourcePermissionsUseCase.Output(listOf(groupPermission, userPermission)))
        }
        mockGetFeatureFlagsUseCase.stub {
            onBlocking { execute(Unit) } doReturn GetFeatureFlagsUseCase.Output(
                FeatureFlagsModel(
                    privacyPolicyUrl = null,
                    termsAndConditionsUrl = null,
                    isPreviewPasswordAvailable = true,
                    areFoldersAvailable = true,
                    areTagsAvailable = true,
                    isTotpAvailable = true
                )
            )
        }
        mockFavouritesInteractor.stub {
            onBlocking { addToFavouritesAndUpdateLocal(any()) }.doReturn(FavouritesInteractor.Output.Success)
            onBlocking { removeFromFavouritesAndUpdateLocal(any()) }.doReturn(FavouritesInteractor.Output.Success)
        }
        mockResourceTagsUseCase.stub {
            onBlocking { execute(any()) }.doReturn(GetLocalResourceTagsUseCase.Output(RESOURCE_TAGS))
        }
        whenever(mockFullDataRefreshExecutor.dataRefreshStatusFlow).doReturn(
            flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        )
        mockGetFolderLocationUseCase.stub {
            onBlocking { execute(any()) }.doReturn(GetLocalFolderLocationUseCase.Output(emptyList()))
        }
        mockGetResourceTypeWithFields.stub {
            onBlocking { execute(any()) }.doReturn(
                GetResourceTypeWithFieldsByIdUseCase.Output(
                    resourceTypeId = RESOURCE_MODEL.resourceTypeId,
                    listOf(ResourceField(0, "description", false, null, true, "string"))
                )
            )
        }
        mockGetResourceTypeIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(UUID.fromString(RESOURCE_TYPE_ID) to PASSWORD_AND_DESCRIPTION_SLUG)
                )
            )
        }
        presenter.attach(view)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `delete resource should show confirmation dialog, delete and close details`() = runTest {
        mockResourceCommonActionsInteractor.stub {
            onBlocking { deleteResource() } doReturn flowOf(
                ResourceCommonActionResult.Success(RESOURCE_MODEL.name)
            )
        }

        presenter.argsReceived(
            RESOURCE_MODEL.resourceId,
            100,
            20f
        )
        presenter.resume(view)
        presenter.moreClick()
        presenter.deleteClick()
        presenter.deleteResourceConfirmed()

        verify(view).showDeleteConfirmationDialog()
        verify(view).closeWithDeleteSuccessResult(RESOURCE_MODEL.name)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `delete resource should show error when there is deletion error`() = runTest {
        mockResourceCommonActionsInteractor.stub {
            onBlocking { deleteResource() } doReturn flowOf(ResourceCommonActionResult.Failure)
        }

        presenter.argsReceived(
            RESOURCE_MODEL.resourceId,
            100,
            20f
        )
        presenter.resume(view)
        presenter.moreClick()
        presenter.deleteClick()
        presenter.deleteResourceConfirmed()

        verify(view).showDeleteConfirmationDialog()
        verify(view).showGeneralError()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `resource url website should be opened if not empty`() = runTest {
        mockResourcePropertiesActionsInteractor.stub {
            onBlocking { provideWebsiteUrl() } doReturn flowOf(
                ResourcePropertyActionResult(
                    ResourcePropertiesActionsInteractor.URL_LABEL,
                    isSecret = false,
                    RESOURCE_MODEL.url.orEmpty()
                )
            )
        }

        presenter.argsReceived(
            RESOURCE_MODEL.resourceId,
            100,
            20f
        )
        presenter.resume(view)
        presenter.moreClick()
        presenter.launchWebsiteClick()

        verify(view).openWebsite(RESOURCE_MODEL.url.orEmpty())
    }

    @Test
    fun `resource username should be copied correct if not empty`() {
        mockResourcePropertiesActionsInteractor.stub {
            onBlocking { provideUsername() } doReturn flowOf(
                ResourcePropertyActionResult(
                    ResourcePropertiesActionsInteractor.USERNAME_LABEL,
                    isSecret = false,
                    RESOURCE_MODEL.username.orEmpty()
                )
            )
        }
        presenter.argsReceived(
            RESOURCE_MODEL.resourceId,
            100,
            20f
        )
        presenter.resume(view)
        presenter.moreClick()
        presenter.usernameCopyClick()

        verify(view).addToClipboard(
            ResourcePropertiesActionsInteractor.USERNAME_LABEL,
            RESOURCE_MODEL.username.orEmpty(),
            isSecret = false
        )
    }

    @Test
    fun `resource url should be copied correct if not empty`() {
        mockResourcePropertiesActionsInteractor.stub {
            onBlocking { provideWebsiteUrl() } doReturn flowOf(
                ResourcePropertyActionResult(
                    ResourcePropertiesActionsInteractor.URL_LABEL,
                    isSecret = false,
                    RESOURCE_MODEL.url.orEmpty()
                )
            )
        }
        presenter.argsReceived(
            RESOURCE_MODEL.resourceId,
            100,
            20f
        )
        presenter.resume(view)
        presenter.moreClick()
        presenter.urlCopyClick()

        verify(view).addToClipboard(
            ResourcePropertiesActionsInteractor.URL_LABEL,
            RESOURCE_MODEL.url.orEmpty(),
            isSecret = false
        )
    }

    private companion object {
        private const val NAME = "name"
        private const val USERNAME = "username"
        private const val INITIALS = "NN"
        private const val URL = "https://www.passbolt.com"
        private val ID = UUID.randomUUID().toString()
        private const val DESCRIPTION = "desc"
        private val RESOURCE_TYPE_ID = UUID.randomUUID().toString()
        private const val FOLDER_ID_ID = "folderId"
        private val RESOURCE_MODEL = ResourceModel(
            ID,
            RESOURCE_TYPE_ID,
            FOLDER_ID_ID,
            NAME,
            USERNAME,
            null,
            INITIALS,
            URL,
            DESCRIPTION,
            ResourcePermission.READ,
            "fav-id",
            ZonedDateTime.now()
        )
        private val groupPermission = PermissionModelUi.GroupPermissionModel(
            ResourcePermission.READ, "permId1", GroupModel("grId", "grName")
        )
        private val userPermission = PermissionModelUi.UserPermissionModel(
            ResourcePermission.OWNER,
            "permId2",
            UserWithAvatar("usId", "first", "last", "uName", null)
        )
        private val RESOURCE_TAGS = listOf(
            TagModel("id1", "tag1", false),
            TagModel("id2", "tag2", false)
        )
    }
}
