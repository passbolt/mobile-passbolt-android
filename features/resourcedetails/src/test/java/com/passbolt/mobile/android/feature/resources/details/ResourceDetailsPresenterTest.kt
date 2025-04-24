package com.passbolt.mobile.android.feature.resources.details

import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderLocationUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.DataRefreshStatus
import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.core.fulldatarefresh.HomeDataInteractor
import com.passbolt.mobile.android.core.rbac.usecase.GetRbacRulesUseCase
import com.passbolt.mobile.android.core.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.SecretPropertyActionResult
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
import com.passbolt.mobile.android.ui.RbacRuleModel.ALLOW
import com.passbolt.mobile.android.ui.RbacRuleModel.DENY
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
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

class ResourceDetailsPresenterTest : KoinTest {

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
        resourceModel = ResourceModel(
            resourceId = ID,
            resourceTypeId = RESOURCE_TYPE_ID.toString(),
            folderId = FOLDER_ID_ID,
            permission = ResourcePermission.READ,
            favouriteId = "fav-id",
            modified = ZonedDateTime.now(),
            expiry = null,
            metadataJsonModel = MetadataJsonModel(
                """
                    {
                        "name": "$NAME",
                        "uri": "$URL",
                        "username": "$USERNAME",
                        "description": "$DESCRIPTION"
                    }
                """.trimIndent()
            ),
            metadataKeyId = null,
            metadataKeyType = null
        )
        resourceModelExpired = ResourceModel(
            resourceId = ID_EXPIRED,
            resourceTypeId = RESOURCE_TYPE_ID.toString(),
            folderId = FOLDER_ID_ID,
            permission = ResourcePermission.READ,
            favouriteId = "fav-id",
            modified = ZonedDateTime.now(),
            expiry = ZonedDateTime.now().minusDays(1),
            metadataJsonModel = MetadataJsonModel(
                """
                    {
                        "name": "$NAME",
                        "uri": "$URL",
                        "username": "$USERNAME",
                        "description": "$DESCRIPTION"
                    }
                """.trimIndent()
            ),
            metadataKeyId = null,
            metadataKeyType = null
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
            onBlocking { execute(Unit) } doReturn GetFeatureFlagsUseCase.Output(
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
                    isV5MetadataAvailable = false
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
        mockResourceTypeIdToSlugMappingProvider.stub {
            onBlocking { provideMappingForSelectedAccount() }.doReturn(
                mapOf(RESOURCE_TYPE_ID to ContentType.PasswordAndDescription.slug)
            )
        }
        mockGetRbacRulesUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetRbacRulesUseCase.Output(
                    RbacModel(
                        passwordPreviewRule = ALLOW,
                        passwordCopyRule = ALLOW,
                        tagsUseRule = ALLOW,
                        shareViewRule = ALLOW,
                        foldersUseRule = ALLOW
                    )
                )
            )
        }
        presenter.attach(view)
    }

    @Test
    fun `password details should be shown correct for password and description`() {
        mockResourceTypeIdToSlugMappingProvider.stub {
            onBlocking { provideMappingForSelectedAccount() }.doReturn(
                mapOf(RESOURCE_TYPE_ID to ContentType.PasswordAndDescription.slug)
            )
        }

        presenter.argsReceived(
            resourceModel,
            100,
            20f
        )
        presenter.resume(view)

        verify(view, times(2)).showFavouriteStar()
        verify(view, times(2)).displayTitle(NAME)
        verify(view, never()).displayExpiryTitle(any())
        verify(view, never()).displayExpirySection(any())
        verify(view, times(2)).displayUsername(USERNAME)
        verify(view, times(2)).displayInitialsIcon(NAME, "n")
        verify(view, times(2)).displayUrl(URL)
        verify(view, times(2)).hidePassword()
        verify(view, times(2)).showPermissions(eq(listOf(groupPermission)), eq(listOf(userPermission)), any(), any())
        verify(view, times(2)).showTags(RESOURCE_TAGS.map { it.slug })
        verify(view, times(2)).disableMetadataDescription()
        verify(view, times(2)).showFolderLocation(emptyList())
        verify(view, times(2)).hideTotpSection()
        verify(view, times(2)).showPasswordSection()
        verify(view, times(2)).showPasswordEyeIcon()
        verify(view, times(2)).hideExpirySection()
        verify(view).hideRefreshProgress()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `password details should be shown correct for password string`() {
        mockResourceTypeIdToSlugMappingProvider.stub {
            onBlocking { provideMappingForSelectedAccount() }.doReturn(
                mapOf(RESOURCE_TYPE_ID to ContentType.PasswordString.slug)
            )
        }

        presenter.argsReceived(
            resourceModel,
            100,
            20f
        )
        presenter.resume(view)

        verify(view, times(2)).showFavouriteStar()
        verify(view, times(2)).displayTitle(NAME)
        verify(view, never()).displayExpiryTitle(any())
        verify(view, never()).displayExpirySection(any())
        verify(view, times(2)).displayUsername(USERNAME)
        verify(view, times(2)).displayInitialsIcon(NAME, "n")
        verify(view, times(2)).displayUrl(URL)
        verify(view, times(2)).hidePassword()
        verify(view, times(2)).showPermissions(eq(listOf(groupPermission)), eq(listOf(userPermission)), any(), any())
        verify(view, times(2)).showTags(RESOURCE_TAGS.map { it.slug })
        verify(view, times(2)).showMetadataDescription(DESCRIPTION)
        verify(view, times(2)).showFolderLocation(emptyList())
        verify(view, times(2)).hideTotpSection()
        verify(view, times(2)).disableNote()
        verify(view, times(2)).showPasswordSection()
        verify(view, times(2)).showPasswordEyeIcon()
        verify(view, times(2)).hideExpirySection()
        verify(view).hideRefreshProgress()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `password details should be shown correct for password description totp`() {
        mockResourceTypeIdToSlugMappingProvider.stub {
            onBlocking { provideMappingForSelectedAccount() }.doReturn(
                mapOf(RESOURCE_TYPE_ID to ContentType.PasswordDescriptionTotp.slug)
            )
        }

        presenter.argsReceived(
            resourceModel,
            100,
            20f
        )
        presenter.resume(view)

        verify(view, times(2)).showFavouriteStar()
        verify(view, times(2)).displayTitle(NAME)
        verify(view, never()).displayExpiryTitle(any())
        verify(view, never()).displayExpirySection(any())
        verify(view, times(2)).displayUsername(USERNAME)
        verify(view, times(2)).displayInitialsIcon(NAME, "n")
        verify(view, times(2)).displayUrl(URL)
        verify(view, times(2)).hidePassword()
        verify(view, times(2)).showPermissions(eq(listOf(groupPermission)), eq(listOf(userPermission)), any(), any())
        verify(view, times(2)).showTags(RESOURCE_TAGS.map { it.slug })
        verify(view, times(2)).showFolderLocation(emptyList())
        verify(view, times(2)).showTotpSection()
        verify(view, times(2)).showPasswordSection()
        verify(view, times(2)).showPasswordEyeIcon()
        verify(view, times(2)).disableMetadataDescription()
        verify(view, times(2)).hideExpirySection()
        verify(view).hideRefreshProgress()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `password details should be shown correct for standalone totp`() {
        mockResourceTypeIdToSlugMappingProvider.stub {
            onBlocking { provideMappingForSelectedAccount() }.doReturn(
                mapOf(RESOURCE_TYPE_ID to ContentType.Totp.slug)
            )
        }

        presenter.argsReceived(
            resourceModel,
            100,
            20f
        )
        presenter.resume(view)

        verify(view, times(2)).showFavouriteStar()
        verify(view, times(2)).displayTitle(NAME)
        verify(view, never()).displayExpiryTitle(any())
        verify(view, never()).displayExpirySection(any())
        verify(view, times(2)).displayUsername(USERNAME)
        verify(view, times(2)).displayInitialsIcon(NAME, "n")
        verify(view, times(2)).displayUrl(URL)
        verify(view, times(2)).hidePassword()
        verify(view, times(2)).showPermissions(eq(listOf(groupPermission)), eq(listOf(userPermission)), any(), any())
        verify(view, times(2)).showTags(RESOURCE_TAGS.map { it.slug })
        verify(view, times(2)).showFolderLocation(emptyList())
        verify(view, times(2)).showTotpSection()
        verify(view, times(2)).hidePasswordSection()
        verify(view, times(2)).hidePassword()
        verify(view, times(2)).disableMetadataDescription()
        verify(view, times(2)).disableNote()
        verify(view, times(2)).showPasswordEyeIcon()
        verify(view, times(2)).hideExpirySection()
        verify(view).hideRefreshProgress()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `password details should be shown correct for expired resource`() {
        mockGetLocalResourceUseCase.stub {
            onBlocking { execute(GetLocalResourceUseCase.Input(resourceModelExpired.resourceId)) }
                .doReturn(GetLocalResourceUseCase.Output(resourceModelExpired))
        }
        mockGetLocalResourcePermissionsUseCase.stub {
            onBlocking { execute(GetLocalResourcePermissionsUseCase.Input(resourceModelExpired.resourceId)) }
                .doReturn(GetLocalResourcePermissionsUseCase.Output(listOf(groupPermission, userPermission)))
        }

        presenter.argsReceived(
            resourceModelExpired,
            100,
            20f
        )
        presenter.resume(view)

        verify(view, times(2)).showFavouriteStar()
        verify(view, never()).displayTitle(NAME)
        verify(view, times(2)).displayExpiryTitle(any())
        verify(view, times(2)).showExpiryIndicator()
        verify(view, times(2)).displayExpirySection(resourceModelExpired.expiry!!)
        verify(view, times(2)).displayUsername(USERNAME)
        verify(view, times(2)).displayInitialsIcon(NAME, "n")
        verify(view, times(2)).displayUrl(URL)
        verify(view, times(2)).hidePassword()
        verify(view, times(2)).showPermissions(eq(listOf(groupPermission)), eq(listOf(userPermission)), any(), any())
        verify(view, times(2)).showTags(RESOURCE_TAGS.map { it.slug })
        verify(view, times(2)).disableMetadataDescription()
        verify(view, times(2)).showPasswordSection()
        verify(view, times(2)).showFolderLocation(emptyList())
        verify(view, times(2)).hideTotpSection()
        verify(view, times(2)).showPasswordEyeIcon()
        verify(view).hideRefreshProgress()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `eye icon should react to password visibility change correct`() {
        mockResourceTypeIdToSlugMappingProvider.stub {
            onBlocking { provideMappingForSelectedAccount() }.doReturn(
                mapOf(RESOURCE_TYPE_ID to ContentType.PasswordAndDescription.slug)
            )
        }
        val password = "pass"
        mockSecretPropertiesActionsInteractor.stub {
            onBlocking { providePassword() } doReturn flowOf(
                SecretPropertyActionResult.Success(
                    SecretPropertiesActionsInteractor.SECRET_LABEL,
                    isSecret = true,
                    password
                )
            )
        }

        presenter.argsReceived(
            resourceModel,
            100,
            20f
        )
        presenter.resume(view)
        presenter.passwordActionClick()
        presenter.passwordActionClick()

        verify(view).showPassword(password)
        verify(view, times(3)).hidePassword()
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `view should show decrypt error correct`() = runTest {
        mockSecretPropertiesActionsInteractor.stub {
            onBlocking { providePassword() } doReturn flowOf(SecretPropertyActionResult.DecryptionFailure())
        }

        presenter.argsReceived(
            resourceModel,
            100,
            20f
        )
        presenter.resume(view)
        presenter.passwordActionClick()

        verify(view).showDecryptionFailure()
    }

    @Test
    fun `view should show fetch error correct`() {
        mockSecretPropertiesActionsInteractor.stub {
            onBlocking { providePassword() } doReturn flowOf(SecretPropertyActionResult.FetchFailure())
        }

        presenter.argsReceived(
            resourceModel,
            100,
            20f
        )
        presenter.resume(view)
        presenter.passwordActionClick()

        verify(view).showFetchFailure()
    }

    @Test
    fun `view should hide preview password when appropriate feature flag is set`() {
        mockGetFeatureFlagsUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetFeatureFlagsUseCase.Output(
                    FeatureFlagsModel(
                        null,
                        null,
                        isPreviewPasswordAvailable = false,
                        areFoldersAvailable = false,
                        areTagsAvailable = false,
                        isTotpAvailable = true,
                        isRbacAvailable = true,
                        isPasswordExpiryAvailable = true,
                        arePasswordPoliciesAvailable = true,
                        canUpdatePasswordPolicies = true,
                        isV5MetadataAvailable = false
                    )
                )
            )
        }

        presenter.argsReceived(
            resourceModel,
            100,
            20f
        )
        presenter.resume(view)

        verify(view, never()).showPasswordEyeIcon()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `resource permissions should be displayed`() = runTest {
        presenter.argsReceived(
            resourceModel,
            100,
            20f
        )
        presenter.resume(view)

        verify(view, times(2)).showPermissions(eq(listOf(groupPermission)), eq(listOf(userPermission)), any(), any())
    }

    @Test
    fun `view should not show features disabled by rbac`() {
        mockGetRbacRulesUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetRbacRulesUseCase.Output(
                    RbacModel(
                        passwordPreviewRule = DENY,
                        passwordCopyRule = DENY,
                        tagsUseRule = DENY,
                        shareViewRule = DENY,
                        foldersUseRule = DENY
                    )
                )
            )
        }

        presenter.argsReceived(
            resourceModel,
            100,
            20f
        )
        presenter.resume(view)

        verify(view, never()).showPasswordEyeIcon()
        verify(view, never()).showTags(any())
        verify(view, never()).showPermissions(any(), any(), any(), any())
        verify(view, never()).showFolderLocation(any())
    }

    private companion object {
        private const val NAME = "name"
        private const val USERNAME = "username"
        private const val URL = "https://www.passbolt.com"
        private val ID = UUID.randomUUID().toString()
        private val ID_EXPIRED = UUID.randomUUID().toString()
        private const val DESCRIPTION = "desc"
        private val RESOURCE_TYPE_ID = UUID.randomUUID()
        private const val FOLDER_ID_ID = "folderId"
        private lateinit var resourceModel: ResourceModel
        private lateinit var resourceModelExpired: ResourceModel
        private val groupPermission = PermissionModelUi.GroupPermissionModel(
            permission = ResourcePermission.READ,
            permissionId = "permId1",
            group = GroupModel(
                groupId = "grId",
                groupName = "grName"
            )
        )
        private val userPermission = PermissionModelUi.UserPermissionModel(
            permission = ResourcePermission.OWNER,
            permissionId = "permId2",
            user = UserWithAvatar(
                userId = "usId",
                firstName = "first",
                lastName = "last",
                userName = "uName",
                isDisabled = false,
                avatarUrl = null
            )
        )
        private val RESOURCE_TAGS = listOf(
            TagModel(id = "id1", slug = "tag1", isShared = false),
            TagModel(id = "id2", slug = "tag2", isShared = false)
        )
    }
}
