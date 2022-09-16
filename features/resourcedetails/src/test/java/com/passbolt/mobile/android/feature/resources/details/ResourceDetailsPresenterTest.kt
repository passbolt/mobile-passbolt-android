package com.passbolt.mobile.android.feature.resources.details

import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.core.resources.usecase.DeleteResourceUseCase
import com.passbolt.mobile.android.core.resources.usecase.FavouritesInteractor
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.database.impl.resources.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.database.impl.resources.GetLocalResourceTagsUseCase
import com.passbolt.mobile.android.database.impl.resources.GetLocalResourceUseCase
import com.passbolt.mobile.android.entity.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.entity.resource.ResourceField
import com.passbolt.mobile.android.entity.resource.ResourceType
import com.passbolt.mobile.android.entity.resource.ResourceTypeIdWithFields
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsContract
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpError
import com.passbolt.mobile.android.storage.usecase.featureflags.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.TagModel
import com.passbolt.mobile.android.ui.UserWithAvatar
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime

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
                    areTagsAvailable = true
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
        presenter.attach(view)
    }

    @Test
    fun `password details should be shown correct`() {
        presenter.argsReceived(
            RESOURCE_MODEL.resourceId,
            100,
            20f
        )

        verify(view).showFavouriteStar()
        verify(view).displayTitle(NAME)
        verify(view).displayUsername(USERNAME)
        verify(view).displayInitialsIcon(NAME, INITIALS)
        verify(view).displayUrl(URL)
        verify(view).showPasswordHidden()
        verify(view).showPasswordHiddenIcon()
        verify(view).showPermissions(eq(listOf(groupPermission)), eq(listOf(userPermission)), any(), any())
        verify(view).showTags(RESOURCE_TAGS.map { it.slug })
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `password details description encryption state should be shown correct for simple password`() {
        mockResourceTypesDao.stub {
            onBlocking { getResourceTypeWithFieldsById(any()) }.doReturn(
                ResourceTypeIdWithFields(
                    ResourceType("id", "simple password", "slug"),
                    listOf(
                        ResourceField(
                            name = "description",
                            isSecret = false,
                            maxLength = null,
                            isRequired = false,
                            type = "string"
                        )
                    )
                )
            )
        }

        presenter.argsReceived(
            RESOURCE_MODEL.resourceId,
            100,
            20f
        )

        verify(view).showDescription(DESCRIPTION, useSecretFont = false)
    }

    @Test
    fun `password details description encryption state should be shown correct for default password`() {
        mockResourceTypesDao.stub {
            onBlocking { getResourceTypeWithFieldsById(any()) }.doReturn(
                ResourceTypeIdWithFields(
                    ResourceType("id", "password", "slug"),
                    listOf(
                        ResourceField(
                            name = "description",
                            isSecret = true,
                            maxLength = null,
                            isRequired = false,
                            type = "string"
                        )
                    )
                )
            )
        }

        presenter.argsReceived(
            RESOURCE_MODEL.resourceId,
            100,
            20f
        )

        verify(view).showDescriptionIsEncrypted()
    }

    @Test
    fun `eye icon should react to password visibility change correct`() {
        mockSecretInteractor.stub {
            onBlocking { fetchAndDecrypt(ID) }.doReturn(SecretInteractor.Output.Success(DECRYPTED_SECRET))
        }
        mockResourceTypeFactory.stub {
            onBlocking { getResourceTypeEnum(any()) }.doReturn(
                ResourceTypeFactory.ResourceTypeEnum.SIMPLE_PASSWORD
            )
        }
        whenever(mockSecretParser.extractPassword(any(), any()))
            .doReturn(String(DECRYPTED_SECRET))

        presenter.argsReceived(
            RESOURCE_MODEL.resourceId,
            100,
            20f
        )
        presenter.secretIconClick()
        presenter.secretIconClick()

        verify(view).showPasswordVisibleIcon()
        verify(view).showPassword(String(DECRYPTED_SECRET))
        verify(view, times(2)).showPasswordHiddenIcon()
        verify(view, times(2)).showPasswordHidden()
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `view should show decrypt error correct`() = runTest {
        mockSecretInteractor.stub {
            onBlocking { fetchAndDecrypt(ID) }.doReturn(
                SecretInteractor.Output.DecryptFailure(
                    OpenPgpError("errorMessage")
                )
            )
        }

        presenter.argsReceived(
            RESOURCE_MODEL.resourceId,
            100,
            20f
        )
        presenter.secretIconClick()

        verify(view).showDecryptionFailure()
    }

    @Test
    fun `view should show fetch error correct`() {
        mockSecretInteractor.stub {
            onBlocking { fetchAndDecrypt(ID) }.doReturn(SecretInteractor.Output.FetchFailure(RuntimeException()))
        }

        presenter.argsReceived(
            RESOURCE_MODEL.resourceId,
            100,
            20f
        )
        presenter.secretIconClick()

        verify(view).showFetchFailure()
    }

    @Test
    fun `view should show auth when passphrase not in cache`() {
        mockSecretInteractor.stub {
            onBlocking { fetchAndDecrypt(ID) }.doReturn(
                SecretInteractor.Output.Unauthorized(
                    AuthenticationState.Unauthenticated.Reason.Session
                )
            )
        }

        presenter.argsReceived(
            RESOURCE_MODEL.resourceId,
            100,
            20f
        )
        presenter.secretIconClick()

        verify(view).showAuth(AuthenticationState.Unauthenticated.Reason.Passphrase)
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
                        areTagsAvailable = false
                    )
                )
            )
        }

        presenter.argsReceived(
            RESOURCE_MODEL.resourceId,
            100,
            20f
        )

        verify(view).hidePasswordEyeIcon()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `delete resource should show confirmation dialog, delete and close details`() = runTest {
        whenever(mockDeleteResourceUseCase.execute(any()))
            .thenReturn(DeleteResourceUseCase.Output.Success)

        presenter.argsReceived(
            RESOURCE_MODEL.resourceId,
            100,
            20f
        )
        presenter.moreClick()
        presenter.deleteClick()
        presenter.deleteResourceConfirmed()

        verify(view).showDeleteConfirmationDialog()
        verify(view).closeWithDeleteSuccessResult(RESOURCE_MODEL.name)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `delete resource should show error when there is deletion error`() = runTest {
        whenever(mockDeleteResourceUseCase.execute(any()))
            .thenReturn(
                DeleteResourceUseCase.Output.Failure<String>(
                    NetworkResult.Failure.NetworkError(
                        RuntimeException(),
                        ""
                    )
                )
            )

        presenter.argsReceived(
            RESOURCE_MODEL.resourceId,
            100,
            20f
        )
        presenter.moreClick()
        presenter.deleteClick()
        presenter.deleteResourceConfirmed()

        verify(view).showDeleteConfirmationDialog()
        verify(view).showGeneralError()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `resource permissions should be displayed`() = runTest {
        presenter.argsReceived(
            RESOURCE_MODEL.resourceId,
            100,
            20f
        )

        verify(view).showPermissions(eq(listOf(groupPermission)), eq(listOf(userPermission)), any(), any())
    }

    private companion object {
        private const val NAME = "name"
        private const val USERNAME = "username"
        private const val INITIALS = "NN"
        private const val URL = "https://www.passbolt.com"
        private const val ID = "id"
        private const val DESCRIPTION = "desc"
        private const val RESOURCE_TYPE_ID = "resTypeId"
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
        private val DECRYPTED_SECRET = "decrypted".toByteArray()
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
