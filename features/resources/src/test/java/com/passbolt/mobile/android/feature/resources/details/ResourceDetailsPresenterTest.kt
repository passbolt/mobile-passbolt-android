package com.passbolt.mobile.android.feature.resources.details

import com.passbolt.mobile.android.core.commonresource.ResourceTypeFactory
import com.passbolt.mobile.android.core.commonresource.usecase.DeleteResourceUseCase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.database.usecase.GetLocalResourceUseCase
import com.passbolt.mobile.android.entity.resource.ResourceField
import com.passbolt.mobile.android.entity.resource.ResourceType
import com.passbolt.mobile.android.entity.resource.ResourceTypeIdWithFields
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.test.runBlockingTest
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
        presenter.attach(view)
    }

    @Test
    fun `constant password details should be shown correct`() {
        presenter.argsReceived(RESOURCE_MODEL.resourceId)

        verify(view).displayTitle(NAME)
        verify(view).displayUsername(USERNAME)
        verify(view).displayInitialsIcon(NAME, INITIALS)
        verify(view).displayUrl(URL)
        verify(view).showPasswordHidden()
        verify(view).showPasswordHiddenIcon()
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

        presenter.argsReceived(RESOURCE_MODEL.resourceId)

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

        presenter.argsReceived(RESOURCE_MODEL.resourceId)

        verify(view).showDescriptionIsEncrypted()
    }

    @Test
    fun `eye icon should react to password visibility change correct`() {
        mockSecretInterActor.stub {
            onBlocking { fetchAndDecrypt(ID) }.doReturn(SecretInteractor.Output.Success(DECRYPTED_SECRET))
        }
        mockResourceTypeFactory.stub {
            onBlocking { getResourceTypeEnum(any()) }.doReturn(
                ResourceTypeFactory.ResourceTypeEnum.SIMPLE_PASSWORD
            )
        }
        whenever(mockSecretParser.extractPassword(any(), any()))
            .doReturn(String(DECRYPTED_SECRET))

        presenter.argsReceived(RESOURCE_MODEL.resourceId)
        presenter.secretIconClick()
        presenter.secretIconClick()

        verify(view).showPasswordVisibleIcon()
        verify(view).showPassword(String(DECRYPTED_SECRET))
        verify(view, times(2)).showPasswordHiddenIcon()
        verify(view, times(2)).showPasswordHidden()
    }

    @Test
    fun `view should show decrypt error correct`() {
        mockSecretInterActor.stub {
            onBlocking { fetchAndDecrypt(ID) }.doReturn(SecretInteractor.Output.DecryptFailure(RuntimeException()))
        }

        presenter.argsReceived(RESOURCE_MODEL.resourceId)
        presenter.secretIconClick()

        verify(view).showDecryptionFailure()
    }

    @Test
    fun `view should show fetch error correct`() {
        mockSecretInterActor.stub {
            onBlocking { fetchAndDecrypt(ID) }.doReturn(SecretInteractor.Output.FetchFailure(RuntimeException()))
        }

        presenter.argsReceived(RESOURCE_MODEL.resourceId)
        presenter.secretIconClick()

        verify(view).showFetchFailure()
    }

    @Test
    fun `view should show auth when passphrase not in cache`() {
        mockSecretInterActor.stub {
            onBlocking { fetchAndDecrypt(ID) }.doReturn(SecretInteractor.Output.Unauthorized)
        }

        presenter.argsReceived(RESOURCE_MODEL.resourceId)
        presenter.secretIconClick()

        verify(view).showAuth(AuthenticationState.Unauthenticated.Reason.Passphrase)
    }

    @Test
    fun `view should hide preview password when appropriate feature flag is set`() {
        mockGetFeatureFlagsUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetFeatureFlagsUseCase.Output(
                    FeatureFlagsModel(null, null, isPreviewPasswordAvailable = false, areFoldersAvailable = false)
                )
            )
        }

        presenter.argsReceived(RESOURCE_MODEL.resourceId)

        verify(view).hidePasswordEyeIcon()
    }

    @Test
    fun `delete resource should show confirmation dialog, delete and close details`() = runBlockingTest {
        whenever(mockDeleteResourceUseCase.execute(any()))
            .thenReturn(DeleteResourceUseCase.Output.Success)

        presenter.argsReceived(RESOURCE_MODEL.resourceId)
        presenter.moreClick()
        presenter.menuDeleteClick()
        presenter.deleteResourceConfirmed()

        verify(view).showDeleteConfirmationDialog()
        verify(view).closeWithDeleteSuccessResult(RESOURCE_MODEL.name)
    }

    @Test
    fun `delete resource should show error when there is deletion error`() = runBlockingTest {
        whenever(mockDeleteResourceUseCase.execute(any()))
            .thenReturn(
                DeleteResourceUseCase.Output.Failure<String>(
                    NetworkResult.Failure.NetworkError(
                        RuntimeException(),
                        ""
                    )
                )
            )

        presenter.argsReceived(RESOURCE_MODEL.resourceId)
        presenter.moreClick()
        presenter.menuDeleteClick()
        presenter.deleteResourceConfirmed()

        verify(view).showDeleteConfirmationDialog()
        verify(view).showGeneralError()
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
            false,
            ZonedDateTime.now()
        )
        private val DECRYPTED_SECRET = "decrypted".toByteArray()
    }
}
