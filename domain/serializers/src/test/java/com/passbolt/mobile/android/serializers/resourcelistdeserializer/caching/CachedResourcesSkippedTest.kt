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

package com.passbolt.mobile.android.serializers.resourcelistdeserializer.caching

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetResourceTypeIdToSlugMappingUseCase
import com.passbolt.mobile.android.dto.response.MetadataKeyTypeDto
import com.passbolt.mobile.android.dto.response.PermissionDto
import com.passbolt.mobile.android.dto.response.ResourceResponseDto
import com.passbolt.mobile.android.dto.response.ResourceResponseV4Dto
import com.passbolt.mobile.android.dto.response.ResourceResponseV5Dto
import com.passbolt.mobile.android.entity.metadata.MetadataKeyType
import com.passbolt.mobile.android.entity.resource.Permission
import com.passbolt.mobile.android.entity.resource.ResourceWithMetadata
import com.passbolt.mobile.android.serializers.gson.MetadataDecryptor.Output.Success
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordAndDescription
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5DefaultWithTotp
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import java.util.UUID

class CachedResourcesSkippedTest : KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(resourceListItemDeserializationTestModule)
    }

    private val gson: Gson by inject()

    @Before
    fun setup() {
        mockGetSelectedAccountUseCase.stub {
            onBlocking { execute(Unit) } doReturn GetSelectedAccountUseCase.Output("selectedAccountId")
        }
        reset(mockJsonSchemaValidationRunner)
    }

    @Test
    fun `validation and decryption of backend resource with same modified date is skipped for v5`() = runTest {
        val modifiedDate = ZonedDateTime.now()
        val resourceId = UUID.randomUUID()
        val localEncryptedMetadata = "local encrypted metadata"

        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to V5DefaultWithTotp.slug)
                )
            )
        }
        val backendResource = listOf(
            ResourceResponseV5Dto(
                id = resourceId,
                resourceTypeId = testedResourceTypeUuid,
                resourceFolderId = null,
                permission = PermissionDto(UUID.randomUUID(), 1, "", UUID.randomUUID(), "", UUID.randomUUID(), "", ""),
                favorite = null,
                modified = modifiedDate.toString(),
                tags = emptyList(),
                permissions = emptyList(),
                expired = null,
                metadataKeyType = MetadataKeyTypeDto.SHARED,
                metadata = "encrypted metadata",
                metadataKeyId = UUID.randomUUID()
            )
        )
        whenever(mockResourcesSnapShot.getCachedResource(resourceId.toString())).thenReturn(
            ResourceWithMetadata(
                resourceId = resourceId.toString(),
                folderId = null,
                resourcePermission = Permission.READ,
                resourceTypeId = testedResourceTypeUuid.toString(),
                favouriteId = null,
                modified = modifiedDate,
                expiry = null,
                metadataJson = localEncryptedMetadata,
                metadataKeyId = UUID.randomUUID().toString(),
                metadataKeyType = MetadataKeyType.SHARED
            )
        )

        val listJson = gson.toJson(backendResource)
        val resulList = gson.fromJson<List<ResourceResponseDto>>(
            listJson,
            object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type
        )

        verify(mockMetadataDecryptor, never()).decryptMetadata(any())
        verify(mockJsonSchemaValidationRunner, never()).isResourceValid(any(), any())

        assertThat(resulList).hasSize(1)
        assertThat((resulList[0] as ResourceResponseV5Dto).metadata).isEqualTo(localEncryptedMetadata)
    }

    @Test
    fun `validation and decryption of backend resource with different modified date is not skipped for v5`() = runTest {
        val resourceId = UUID.randomUUID()
        val backendEncryptedMetadata = "backend encrypted metadata"

        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to V5DefaultWithTotp.slug)
                )
            )
        }
        val backendResource = listOf(
            ResourceResponseV5Dto(
                id = resourceId,
                resourceTypeId = testedResourceTypeUuid,
                resourceFolderId = null,
                permission = PermissionDto(UUID.randomUUID(), 1, "", UUID.randomUUID(), "", UUID.randomUUID(), "", ""),
                favorite = null,
                modified = ZonedDateTime.now().plusDays(1).toString(),
                tags = emptyList(),
                permissions = emptyList(),
                expired = null,
                metadataKeyType = MetadataKeyTypeDto.SHARED,
                metadata = backendEncryptedMetadata,
                metadataKeyId = UUID.randomUUID()
            )
        )
        whenever(mockResourcesSnapShot.getCachedResource(resourceId.toString())).thenReturn(
            ResourceWithMetadata(
                resourceId = resourceId.toString(),
                folderId = null,
                resourcePermission = Permission.READ,
                resourceTypeId = testedResourceTypeUuid.toString(),
                favouriteId = null,
                modified = ZonedDateTime.now(),
                expiry = null,
                metadataJson = "local encrypted metadata",
                metadataKeyId = UUID.randomUUID().toString(),
                metadataKeyType = MetadataKeyType.SHARED
            )
        )

        mockMetadataDecryptor.stub {
            onBlocking { decryptMetadata(any()) }.doAnswer { invocation ->
                val param = invocation.arguments[0] as ResourceResponseV5Dto
                Success(param.metadata)
            }
        }
        mockJsonSchemaValidationRunner.stub {
            onBlocking { isResourceValid(any(), any()) }.doReturn(true)
        }

        val listJson = gson.toJson(backendResource)
        val resulList = gson.fromJson<List<ResourceResponseDto>>(
            listJson,
            object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type
        )

        verify(mockMetadataDecryptor).decryptMetadata(any())
        verify(mockJsonSchemaValidationRunner).isResourceValid(any(), any())

        assertThat(resulList).hasSize(1)
        assertThat((resulList[0] as ResourceResponseV5Dto).metadata).isEqualTo(backendEncryptedMetadata)
    }

    @Test
    fun `validation of backend resource with same modified date is skipped for v4`() = runTest {
        val modifiedDate = ZonedDateTime.now()
        val resourceId = UUID.randomUUID()
        val localEncryptedMetadata = "local encrypted metadata"

        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to PasswordAndDescription.slug)
                )
            )
        }
        val backendResource = listOf(
            ResourceResponseV4Dto(
                id = resourceId,
                resourceTypeId = testedResourceTypeUuid,
                resourceFolderId = null,
                permission = PermissionDto(UUID.randomUUID(), 1, "", UUID.randomUUID(), "", UUID.randomUUID(), "", ""),
                favorite = null,
                modified = modifiedDate.toString(),
                tags = emptyList(),
                permissions = emptyList(),
                expired = null,
                name = "backend name",
                uri = "backend uri",
                description = "backend description",
                username = "backend username"
            )
        )
        whenever(mockResourcesSnapShot.getCachedResource(resourceId.toString())).thenReturn(
            ResourceWithMetadata(
                resourceId = resourceId.toString(),
                folderId = null,
                resourcePermission = Permission.READ,
                resourceTypeId = testedResourceTypeUuid.toString(),
                favouriteId = null,
                modified = modifiedDate,
                expiry = null,
                metadataJson = localEncryptedMetadata,
                metadataKeyId = null,
                metadataKeyType = null
            )
        )

        val listJson = gson.toJson(backendResource)
        val resulList = gson.fromJson<List<ResourceResponseDto>>(
            listJson,
            object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type
        )

        verify(mockJsonSchemaValidationRunner, never()).isResourceValid(any(), any())

        assertThat(resulList).hasSize(1)
        assertThat((resulList[0] as ResourceResponseV4Dto).name).isEqualTo("backend name")
        assertThat((resulList[0] as ResourceResponseV4Dto).uri).isEqualTo("backend uri")
        assertThat((resulList[0] as ResourceResponseV4Dto).description).isEqualTo("backend description")
        assertThat((resulList[0] as ResourceResponseV4Dto).username).isEqualTo("backend username")
    }

    @Test
    fun `validation and decryption of backend resource with different modified date is not skipped for v4`() = runTest {
        val resourceId = UUID.randomUUID()

        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to PasswordAndDescription.slug)
                )
            )
        }
        val backendResource = listOf(
            ResourceResponseV4Dto(
                id = resourceId,
                resourceTypeId = testedResourceTypeUuid,
                resourceFolderId = null,
                permission = PermissionDto(UUID.randomUUID(), 1, "", UUID.randomUUID(), "", UUID.randomUUID(), "", ""),
                favorite = null,
                modified = ZonedDateTime.now().plusDays(1).toString(),
                tags = emptyList(),
                permissions = emptyList(),
                expired = null,
                name = "backend name",
                uri = "backend uri",
                description = "backend description",
                username = "backend username"
            )
        )
        whenever(mockResourcesSnapShot.getCachedResource(resourceId.toString())).thenReturn(
            ResourceWithMetadata(
                resourceId = resourceId.toString(),
                folderId = null,
                resourcePermission = Permission.READ,
                resourceTypeId = testedResourceTypeUuid.toString(),
                favouriteId = null,
                modified = ZonedDateTime.now(),
                expiry = null,
                metadataJson = "local encrypted metadata",
                metadataKeyId = null,
                metadataKeyType = null
            )
        )

        mockJsonSchemaValidationRunner.stub {
            onBlocking { isResourceValid(any(), any()) }.doReturn(true)
        }

        val listJson = gson.toJson(backendResource)
        val resulList = gson.fromJson<List<ResourceResponseDto>>(
            listJson,
            object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type
        )

        verify(mockMetadataDecryptor, never()).decryptMetadata(any())
        verify(mockJsonSchemaValidationRunner).isResourceValid(any(), any())

        assertThat(resulList).hasSize(1)
        assertThat((resulList[0] as ResourceResponseV4Dto).name).isEqualTo("backend name")
    }

    private companion object {
        private val testedResourceTypeUuid = UUID.randomUUID()
    }
}
