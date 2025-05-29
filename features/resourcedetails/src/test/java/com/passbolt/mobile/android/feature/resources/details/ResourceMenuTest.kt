package com.passbolt.mobile.android.feature.resources.details

import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderLocationUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.DataRefreshStatus
import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.core.fulldatarefresh.HomeDataInteractor
import com.passbolt.mobile.android.core.rbac.usecase.GetRbacRulesUseCase
import com.passbolt.mobile.android.core.resources.actions.ResourceCommonActionResult
import com.passbolt.mobile.android.core.resources.actions.ResourcePropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourcePropertyActionResult
import com.passbolt.mobile.android.core.resources.usecase.FavouritesInteractor
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceTagsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.entity.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsContract
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.RbacModel
import com.passbolt.mobile.android.ui.RbacRuleModel
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
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testResourceDetailsModule)
        }

    @Before
    fun setup() {
        resourceModel =
            ResourceModel(
                resourceId = ID,
                resourceTypeId = RESOURCE_TYPE_ID.toString(),
                folderId = FOLDER_ID_ID,
                permission = ResourcePermission.READ,
                favouriteId = "fav-id",
                modified = ZonedDateTime.now(),
                expiry = null,
                metadataJsonModel =
                    MetadataJsonModel(
                        """
                        {
                            "name": "$NAME",
                            "uri": "$URL",
                            "username": "$USERNAME",
                            "description": "$DESCRIPTION"
                        }
                        """.trimIndent(),
                    ),
                metadataKeyType = null,
                metadataKeyId = null,
            )
        mockGetLocalResourceUseCase.stub {
            onBlocking { execute(GetLocalResourceUseCase.Input(resourceModel.resourceId)) }
                .doReturn(GetLocalResourceUseCase.Output(resourceModel))
        }
        mockGetLocalResourcePermissionsUseCase.stub {
            onBlocking { execute(GetLocalResourcePermissionsUseCase.Input(resourceModel.resourceId)) }
                .doReturn(GetLocalResourcePermissionsUseCase.Output(listOf(groupPermission, userPermission)))
        }
        mockGetFeatureFlagsUseCase.stub {
            onBlocking { execute(Unit) } doReturn
                GetFeatureFlagsUseCase.Output(
                    FeatureFlagsModel(
                        privacyPolicyUrl = null,
                        termsAndConditionsUrl = null,
                        isPreviewPasswordAvailable = true,
                        areFoldersAvailable = true,
                        areTagsAvailable = true,
                        isTotpAvailable = true,
                        isRbacAvailable = true,
                        isPasswordExpiryAvailable = true,
                        arePasswordPoliciesAvailable = true,
                        canUpdatePasswordPolicies = true,
                        isV5MetadataAvailable = false,
                    ),
                )
        }
        mockGetRbacRulesUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetRbacRulesUseCase.Output(
                    RbacModel(
                        passwordPreviewRule = RbacRuleModel.ALLOW,
                        passwordCopyRule = RbacRuleModel.ALLOW,
                        tagsUseRule = RbacRuleModel.ALLOW,
                        shareViewRule = RbacRuleModel.ALLOW,
                        foldersUseRule = RbacRuleModel.ALLOW,
                    ),
                ),
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
            flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success)),
        )
        mockGetFolderLocationUseCase.stub {
            onBlocking { execute(any()) }.doReturn(GetLocalFolderLocationUseCase.Output(emptyList()))
        }
        mockResourceTypeIdToSlugMappingProvider.stub {
            onBlocking { provideMappingForSelectedAccount() }.doReturn(
                mapOf(RESOURCE_TYPE_ID to ContentType.PasswordString.slug),
            )
        }
        presenter.attach(view)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `delete resource should show confirmation dialog, delete and close details`() =
        runTest {
            mockResourceCommonActionsInteractor.stub {
                onBlocking { deleteResource() } doReturn
                    flowOf(
                        ResourceCommonActionResult.Success(resourceModel.metadataJsonModel.name),
                    )
            }

            presenter.argsReceived(
                resourceModel,
                100,
                20f,
            )
            presenter.resume(view)
            presenter.moreClick()
            presenter.deleteClick()
            presenter.deleteResourceConfirmed()

            verify(view).showDeleteConfirmationDialog()
            verify(view).closeWithDeleteSuccessResult(resourceModel.metadataJsonModel.name)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `delete resource should show error when there is deletion error`() =
        runTest {
            mockResourceCommonActionsInteractor.stub {
                onBlocking { deleteResource() } doReturn flowOf(ResourceCommonActionResult.Failure)
            }

            presenter.argsReceived(
                resourceModel,
                100,
                20f,
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
    fun `resource url website should be opened if not empty`() =
        runTest {
            mockResourcePropertiesActionsInteractor.stub {
                onBlocking { provideWebsiteUrl() } doReturn
                    flowOf(
                        ResourcePropertyActionResult(
                            ResourcePropertiesActionsInteractor.URL_LABEL,
                            isSecret = false,
                            resourceModel.metadataJsonModel.uri.orEmpty(),
                        ),
                    )
            }

            presenter.argsReceived(
                resourceModel,
                100,
                20f,
            )
            presenter.resume(view)
            presenter.moreClick()
            presenter.launchWebsiteClick()

            verify(view).openWebsite(resourceModel.metadataJsonModel.uri.orEmpty())
        }

    @Test
    fun `resource username should be copied correct if not empty`() {
        mockResourcePropertiesActionsInteractor.stub {
            onBlocking { provideUsername() } doReturn
                flowOf(
                    ResourcePropertyActionResult(
                        ResourcePropertiesActionsInteractor.USERNAME_LABEL,
                        isSecret = false,
                        resourceModel.metadataJsonModel.username.orEmpty(),
                    ),
                )
        }
        presenter.argsReceived(
            resourceModel,
            100,
            20f,
        )
        presenter.resume(view)
        presenter.moreClick()
        presenter.usernameCopyClick()

        verify(view).addToClipboard(
            ResourcePropertiesActionsInteractor.USERNAME_LABEL,
            resourceModel.metadataJsonModel.username.orEmpty(),
            isSecret = false,
        )
    }

    @Test
    fun `resource url should be copied correct if not empty`() {
        mockResourcePropertiesActionsInteractor.stub {
            onBlocking { provideWebsiteUrl() } doReturn
                flowOf(
                    ResourcePropertyActionResult(
                        ResourcePropertiesActionsInteractor.URL_LABEL,
                        isSecret = false,
                        resourceModel.metadataJsonModel.uri.orEmpty(),
                    ),
                )
        }
        presenter.argsReceived(
            resourceModel,
            100,
            20f,
        )
        presenter.resume(view)
        presenter.moreClick()
        presenter.urlCopyClick()

        verify(view).addToClipboard(
            ResourcePropertiesActionsInteractor.URL_LABEL,
            resourceModel.metadataJsonModel.uri.orEmpty(),
            isSecret = false,
        )
    }

    private companion object {
        private const val NAME = "name"
        private const val USERNAME = "username"
        private const val URL = "https://www.passbolt.com"
        private val ID = UUID.randomUUID().toString()
        private const val DESCRIPTION = "desc"
        private val RESOURCE_TYPE_ID = UUID.randomUUID()
        private const val FOLDER_ID_ID = "folderId"
        private lateinit var resourceModel: ResourceModel
        private val groupPermission =
            PermissionModelUi.GroupPermissionModel(
                permission = ResourcePermission.READ,
                permissionId = "permId1",
                group = GroupModel("grId", "grName"),
            )
        private val userPermission =
            PermissionModelUi.UserPermissionModel(
                permission = ResourcePermission.OWNER,
                permissionId = "permId2",
                user =
                    UserWithAvatar(
                        userId = "usId",
                        firstName = "first",
                        lastName = "last",
                        userName = "uName",
                        isDisabled = false,
                        avatarUrl = null,
                    ),
            )
        private val RESOURCE_TAGS =
            listOf(
                TagModel(id = "id1", slug = "tag1", isShared = false),
                TagModel(id = "id2", slug = "tag2", isShared = false),
            )
    }
}
