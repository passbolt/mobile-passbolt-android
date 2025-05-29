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

package com.passbolt.mobile.android.serializers.resourcelistdeserializer

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetLocalResourceTypesUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetResourceTypeIdToSlugMappingUseCase
import com.passbolt.mobile.android.dto.response.MetadataKeyTypeDto
import com.passbolt.mobile.android.dto.response.PermissionDto
import com.passbolt.mobile.android.dto.response.ResourceResponseDto
import com.passbolt.mobile.android.dto.response.ResourceResponseV4Dto
import com.passbolt.mobile.android.dto.response.ResourceResponseV5Dto
import com.passbolt.mobile.android.serializers.gson.MetadataDecryptor
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordAndDescription
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordDescriptionTotp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordString
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.Totp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5Default
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5DefaultWithTotp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5PasswordString
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5TotpStandalone
import com.passbolt.mobile.android.ui.ResourceTypeModel
import net.jimblackler.jsonschemafriend.SchemaStore
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import java.util.UUID

@Suppress("LargeClass")
class ResourceListDeserializerTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(resourceListDeserializationTestModule)
        }

    private val gson: Gson by inject()

    @Before
    fun setup() {
        mockGetSelectedAccountUseCase.stub {
            onBlocking { execute(Unit) } doReturn GetSelectedAccountUseCase.Output("selectedAccountId")
        }
        mockGetLocalResourceTypesUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetLocalResourceTypesUseCase.Output(
                    listOf(
                        ResourceTypeModel(UUID.randomUUID(), PasswordString.slug, "", deleted = null),
                        ResourceTypeModel(UUID.randomUUID(), V5PasswordString.slug, "", deleted = null),
                        ResourceTypeModel(UUID.randomUUID(), PasswordAndDescription.slug, "", deleted = null),
                        ResourceTypeModel(UUID.randomUUID(), V5Default.slug, "", deleted = null),
                        ResourceTypeModel(UUID.randomUUID(), Totp.slug, "", deleted = null),
                        ResourceTypeModel(UUID.randomUUID(), V5TotpStandalone.slug, "", deleted = null),
                        ResourceTypeModel(UUID.randomUUID(), PasswordDescriptionTotp.slug, "", deleted = null),
                        ResourceTypeModel(UUID.randomUUID(), V5DefaultWithTotp.slug, "", deleted = null),
                    ),
                ),
            )
        }
        mockJSFSchemaRepository.stub {
            on { schemaForResource(PasswordString.slug) } doReturn
                SchemaStore().loadSchema(
                    this::class.java.getResource("/password-string-resource-schema.json"),
                )
            on { schemaForResource(V5PasswordString.slug) } doReturn
                SchemaStore().loadSchema(
                    this::class.java.getResource("/v5-password-string-resource-schema.json"),
                )
            on { schemaForResource(PasswordAndDescription.slug) } doReturn
                SchemaStore().loadSchema(
                    this::class.java.getResource("/password-and-description-resource-schema.json"),
                )
            on { schemaForResource(V5Default.slug) } doReturn
                SchemaStore().loadSchema(
                    this::class.java.getResource("/v5-default-resource-schema.json"),
                )
            on { schemaForResource(PasswordDescriptionTotp.slug) } doReturn
                SchemaStore().loadSchema(
                    this::class.java.getResource("/password-description-totp-resource-schema.json"),
                )
            on { schemaForResource(V5DefaultWithTotp.slug) } doReturn
                SchemaStore().loadSchema(
                    this::class.java.getResource("/v5-default-with-totp-resource-schema.json"),
                )
            on { schemaForResource(Totp.slug) } doReturn
                SchemaStore().loadSchema(
                    this::class.java.getResource("/totp-resource-schema.json"),
                )
            on { schemaForResource(V5TotpStandalone.slug) } doReturn
                SchemaStore().loadSchema(
                    this::class.java.getResource("/v5-totp-standalone-resource-schema.json"),
                )
        }
    }

    @Test
    fun `resources with invalid fields for password string type should be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to PasswordString.slug),
                ),
            )
        }

        val invalidResources =
            listOf(
                resourceWithNameOfLength(
                    PASSWORD_STRING_NAME_MAX_LENGTH + 1,
                    testedResourceTypeUuid,
                ),
                resourceWithUriOfLength(
                    PASSWORD_STRING_URI_MAX_LENGTH + 1,
                    testedResourceTypeUuid,
                ),
                resourceWithDescriptionOfLength(
                    PASSWORD_STRING_DESCRIPTION_MAX_LENGTH + 1,
                    testedResourceTypeUuid,
                ),
                resourceWithUsernameOfLength(
                    PASSWORD_STRING_USERNAME_MAX_LENGTH + 1,
                    testedResourceTypeUuid,
                ),
            )

        val listJson = gson.toJson(invalidResources)
        val resulList =
            gson.fromJson<List<ResourceResponseDto>>(
                listJson,
                object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type,
            )

        assertThat(resulList).isEmpty()
    }

    @Test
    fun `resources with invalid fields for v5 password string type should be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to V5PasswordString.slug),
                ),
            )
        }

        val invalidResources =
            listOf(
                resourceWithNameOfLength(
                    PASSWORD_STRING_NAME_MAX_LENGTH + 1,
                    testedResourceTypeUuid,
                ),
                resourceWithUriOfLength(
                    PASSWORD_STRING_URI_MAX_LENGTH + 1,
                    testedResourceTypeUuid,
                ),
                resourceWithDescriptionOfLength(
                    PASSWORD_STRING_DESCRIPTION_MAX_LENGTH + 1,
                    testedResourceTypeUuid,
                ),
                resourceWithUsernameOfLength(
                    PASSWORD_STRING_USERNAME_MAX_LENGTH + 1,
                    testedResourceTypeUuid,
                ),
            )

        val listJson = gson.toJson(invalidResources)
        val resulList =
            gson.fromJson<List<ResourceResponseDto>>(
                listJson,
                object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type,
            )

        assertThat(resulList).isEmpty()
    }

    @Test
    fun `resources with invalid fields for password and description type should be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to PasswordAndDescription.slug),
                ),
            )
        }
        val invalidResources =
            listOf(
                resourceWithNameOfLength(
                    PASSWORD_AND_DESCRIPTION_NAME_MAX_LENGTH + 1,
                    testedResourceTypeUuid,
                ),
                resourceWithUriOfLength(
                    PASSWORD_AND_DESCRIPTION_URI_MAX_LENGTH + 1,
                    testedResourceTypeUuid,
                ),
                resourceWithUsernameOfLength(
                    PASSWORD_AND_DESCRIPTION_USERNAME_MAX_LENGTH + 1,
                    testedResourceTypeUuid,
                ),
            )

        val listJson = gson.toJson(invalidResources)
        val resulList =
            gson.fromJson<List<ResourceResponseDto>>(
                listJson,
                object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type,
            )

        assertThat(resulList).isEmpty()
    }

    @Test
    fun `resources with invalid fields for v5 default type should be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to V5Default.slug),
                ),
            )
        }
        val invalidResources =
            listOf(
                resourceWithNameOfLength(
                    PASSWORD_AND_DESCRIPTION_NAME_MAX_LENGTH + 1,
                    testedResourceTypeUuid,
                ),
                resourceWithUriOfLength(
                    PASSWORD_AND_DESCRIPTION_URI_MAX_LENGTH + 1,
                    testedResourceTypeUuid,
                ),
                resourceWithUsernameOfLength(
                    PASSWORD_AND_DESCRIPTION_USERNAME_MAX_LENGTH + 1,
                    testedResourceTypeUuid,
                ),
            )

        val listJson = gson.toJson(invalidResources)
        val resulList =
            gson.fromJson<List<ResourceResponseDto>>(
                listJson,
                object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type,
            )

        assertThat(resulList).isEmpty()
    }

    @Test
    fun `resources with invalid fields for totp type should be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to Totp.slug),
                ),
            )
        }
        val invalidResources =
            listOf(
                resourceWithNameOfLength(
                    TOTP_NAME_MAX_LENGTH + 1,
                    testedResourceTypeUuid,
                ),
                resourceWithUriOfLength(
                    TOTP_URI_MAX_LENGTH + 1,
                    testedResourceTypeUuid,
                ),
            )

        val listJson = gson.toJson(invalidResources)
        val resulList =
            gson.fromJson<List<ResourceResponseDto>>(
                listJson,
                object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type,
            )

        assertThat(resulList).isEmpty()
    }

    @Test
    fun `resources with invalid fields for v5 totp standalone type should be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to V5TotpStandalone.slug),
                ),
            )
        }
        val invalidResources =
            listOf(
                resourceWithNameOfLength(
                    TOTP_NAME_MAX_LENGTH + 1,
                    testedResourceTypeUuid,
                ),
                resourceWithUriOfLength(
                    TOTP_URI_MAX_LENGTH + 1,
                    testedResourceTypeUuid,
                ),
            )

        val listJson = gson.toJson(invalidResources)
        val resulList =
            gson.fromJson<List<ResourceResponseDto>>(
                listJson,
                object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type,
            )

        assertThat(resulList).isEmpty()
    }

    @Test
    fun `resources with invalid fields for password description totp type should be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to PasswordDescriptionTotp.slug),
                ),
            )
        }
        val invalidResources =
            listOf(
                resourceWithNameOfLength(
                    PASSWORD_DESCRIPTION_TOTP_NAME_MAX_LENGTH + 1,
                    testedResourceTypeUuid,
                ),
                resourceWithUriOfLength(
                    PASSWORD_DESCRIPTION_TOTP_URI_MAX_LENGTH + 1,
                    testedResourceTypeUuid,
                ),
                resourceWithUsernameOfLength(
                    PASSWORD_DESCRIPTION_TOTP_USERNAME_MAX_LENGTH + 1,
                    testedResourceTypeUuid,
                ),
            )

        val listJson = gson.toJson(invalidResources)
        val resulList =
            gson.fromJson<List<ResourceResponseDto>>(
                listJson,
                object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type,
            )

        assertThat(resulList).isEmpty()
    }

    @Test
    fun `resources with invalid fields for v5 default with totp type should be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to V5DefaultWithTotp.slug),
                ),
            )
        }
        val invalidResources =
            listOf(
                resourceWithNameOfLength(
                    PASSWORD_DESCRIPTION_TOTP_NAME_MAX_LENGTH + 1,
                    testedResourceTypeUuid,
                ),
                resourceWithUriOfLength(
                    PASSWORD_DESCRIPTION_TOTP_URI_MAX_LENGTH + 1,
                    testedResourceTypeUuid,
                ),
                resourceWithUsernameOfLength(
                    PASSWORD_DESCRIPTION_TOTP_USERNAME_MAX_LENGTH + 1,
                    testedResourceTypeUuid,
                ),
            )

        val listJson = gson.toJson(invalidResources)
        val resulList =
            gson.fromJson<List<ResourceResponseDto>>(
                listJson,
                object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type,
            )

        assertThat(resulList).isEmpty()
    }

    @Test
    fun `optional fields should pass validation`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to PasswordAndDescription.slug),
                ),
            )
        }
        val validResources =
            listOf(
                ResourceResponseV4Dto(
                    id = UUID.randomUUID(),
                    resourceTypeId = testedResourceTypeUuid,
                    description = null,
                    resourceFolderId = null,
                    name = "",
                    uri = null,
                    username = null,
                    permission = PermissionDto(UUID.randomUUID(), 1, "", UUID.randomUUID(), "", UUID.randomUUID(), "", ""),
                    favorite = null,
                    modified = "",
                    tags = emptyList(),
                    permissions = emptyList(),
                    expired = null,
                ),
            )

        val listJson = gson.toJson(validResources)
        val resulList =
            gson.fromJson<List<ResourceResponseDto>>(
                listJson,
                object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type,
            )

        assertThat(resulList.size).isEqualTo(1)
    }

    @Test
    fun `resource with valid fields for password string type should not be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to PasswordString.slug),
                ),
            )
        }
        val validResources =
            listOf(
                ResourceResponseV4Dto(
                    id = UUID.randomUUID(),
                    resourceTypeId = testedResourceTypeUuid,
                    description = null,
                    resourceFolderId = null,
                    name = "",
                    uri = null,
                    username = null,
                    permission = PermissionDto(UUID.randomUUID(), 1, "", UUID.randomUUID(), "", UUID.randomUUID(), "", ""),
                    favorite = null,
                    modified = "",
                    tags = emptyList(),
                    permissions = emptyList(),
                    expired = null,
                ),
            )

        val listJson = gson.toJson(validResources)
        val resulList =
            gson.fromJson<List<ResourceResponseDto>>(
                listJson,
                object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type,
            )

        assertThat(resulList).hasSize(1)
        assertThat(resulList[0].id).isEqualTo(validResources[0].id)
    }

    @Test
    fun `resource with valid fields for v5 password string type should not be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to V5PasswordString.slug),
                ),
            )
        }
        val validResources =
            listOf(
                ResourceResponseV5Dto(
                    id = UUID.randomUUID(),
                    resourceTypeId = testedResourceTypeUuid,
                    resourceFolderId = null,
                    permission = PermissionDto(UUID.randomUUID(), 1, "", UUID.randomUUID(), "", UUID.randomUUID(), "", ""),
                    favorite = null,
                    modified = "",
                    tags = emptyList(),
                    permissions = emptyList(),
                    expired = null,
                    metadataKeyType = MetadataKeyTypeDto.SHARED,
                    metadata = "encrypted metadata",
                    metadataKeyId = UUID.randomUUID(),
                ),
            )
        mockMetadataDecryptor.stub {
            onBlocking { decryptMetadata(any()) }.doReturn(
                MetadataDecryptor.Output.Success(
                    """
                    {
                        "object_type": "PASSBOLT_RESOURCE_METADATA",
                        "name": "name",
                        "uris": ["uri1"],
                        "username": "username",
                        "description": "description"
                    }
                    """.trimIndent(),
                ),
            )
        }

        val listJson = gson.toJson(validResources)
        val resulList =
            gson.fromJson<List<ResourceResponseDto>>(
                listJson,
                object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type,
            )

        assertThat(resulList).hasSize(1)
        assertThat(resulList[0].id).isEqualTo(validResources[0].id)
    }

    @Test
    fun `resources with valid fields for password and description type should not be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to PasswordAndDescription.slug),
                ),
            )
        }
        val validResources =
            listOf(
                ResourceResponseV4Dto(
                    id = UUID.randomUUID(),
                    resourceTypeId = testedResourceTypeUuid,
                    description = null,
                    resourceFolderId = null,
                    name = "",
                    uri = null,
                    username = null,
                    permission = PermissionDto(UUID.randomUUID(), 1, "", UUID.randomUUID(), "", UUID.randomUUID(), "", ""),
                    favorite = null,
                    modified = "",
                    tags = emptyList(),
                    permissions = emptyList(),
                    expired = null,
                ),
            )

        val listJson = gson.toJson(validResources)
        val resulList =
            gson.fromJson<List<ResourceResponseDto>>(
                listJson,
                object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type,
            )

        assertThat(resulList).hasSize(1)
        assertThat(resulList[0].id).isEqualTo(validResources[0].id)
    }

    @Test
    fun `resources with valid fields for v5 default type should not be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to V5Default.slug),
                ),
            )
        }
        val validResources =
            listOf(
                ResourceResponseV5Dto(
                    id = UUID.randomUUID(),
                    resourceTypeId = testedResourceTypeUuid,
                    resourceFolderId = null,
                    permission = PermissionDto(UUID.randomUUID(), 1, "", UUID.randomUUID(), "", UUID.randomUUID(), "", ""),
                    favorite = null,
                    modified = "",
                    tags = emptyList(),
                    permissions = emptyList(),
                    expired = null,
                    metadataKeyType = MetadataKeyTypeDto.SHARED,
                    metadata = "encrypted metadata",
                    metadataKeyId = UUID.randomUUID(),
                ),
            )
        mockMetadataDecryptor.stub {
            onBlocking { decryptMetadata(any()) }.doReturn(
                MetadataDecryptor.Output.Success(
                    """
                    {
                        "object_type": "PASSBOLT_RESOURCE_METADATA",
                        "name": "name",
                        "uris": ["uri1"],
                        "username": "username",
                        "description": "description"
                    }
                    """.trimIndent(),
                ),
            )
        }

        val listJson = gson.toJson(validResources)
        val resulList =
            gson.fromJson<List<ResourceResponseDto>>(
                listJson,
                object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type,
            )

        assertThat(resulList).hasSize(1)
        assertThat(resulList[0].id).isEqualTo(validResources[0].id)
    }

    @Test
    fun `resources with valid fields for totp type should not be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to Totp.slug),
                ),
            )
        }
        val validResources =
            listOf(
                ResourceResponseV4Dto(
                    id = UUID.randomUUID(),
                    resourceTypeId = testedResourceTypeUuid,
                    description = null,
                    resourceFolderId = null,
                    name = "",
                    uri = null,
                    username = null,
                    permission = PermissionDto(UUID.randomUUID(), 1, "", UUID.randomUUID(), "", UUID.randomUUID(), "", ""),
                    favorite = null,
                    modified = "",
                    tags = emptyList(),
                    permissions = emptyList(),
                    expired = null,
                ),
            )

        val listJson = gson.toJson(validResources)
        val resulList =
            gson.fromJson<List<ResourceResponseDto>>(
                listJson,
                object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type,
            )

        assertThat(resulList).hasSize(1)
        assertThat(resulList[0].id).isEqualTo(validResources[0].id)
    }

    @Test
    fun `resources with valid fields for v5 totp standalone type should not be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to V5TotpStandalone.slug),
                ),
            )
        }
        val validResources =
            listOf(
                ResourceResponseV5Dto(
                    id = UUID.randomUUID(),
                    resourceTypeId = testedResourceTypeUuid,
                    resourceFolderId = null,
                    permission = PermissionDto(UUID.randomUUID(), 1, "", UUID.randomUUID(), "", UUID.randomUUID(), "", ""),
                    favorite = null,
                    modified = "",
                    tags = emptyList(),
                    permissions = emptyList(),
                    expired = null,
                    metadataKeyType = MetadataKeyTypeDto.SHARED,
                    metadata = "encrypted metadata",
                    metadataKeyId = UUID.randomUUID(),
                ),
            )
        mockMetadataDecryptor.stub {
            onBlocking { decryptMetadata(any()) }.doReturn(
                MetadataDecryptor.Output.Success(
                    """
                    {
                        "object_type": "PASSBOLT_RESOURCE_METADATA",
                        "name": "name",
                        "uris": ["uri1"],
                        "username": "username",
                        "description": "description"
                    }
                    """.trimIndent(),
                ),
            )
        }

        val listJson = gson.toJson(validResources)
        val resulList =
            gson.fromJson<List<ResourceResponseDto>>(
                listJson,
                object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type,
            )

        assertThat(resulList).hasSize(1)
        assertThat(resulList[0].id).isEqualTo(validResources[0].id)
    }

    @Test
    fun `resources with valid fields for password description totp type should not be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to PasswordDescriptionTotp.slug),
                ),
            )
        }
        val validResources =
            listOf(
                ResourceResponseV4Dto(
                    id = UUID.randomUUID(),
                    resourceTypeId = testedResourceTypeUuid,
                    description = null,
                    resourceFolderId = null,
                    name = "",
                    uri = null,
                    username = null,
                    permission = PermissionDto(UUID.randomUUID(), 1, "", UUID.randomUUID(), "", UUID.randomUUID(), "", ""),
                    favorite = null,
                    modified = "",
                    tags = emptyList(),
                    permissions = emptyList(),
                    expired = null,
                ),
            )

        val listJson = gson.toJson(validResources)
        val resulList =
            gson.fromJson<List<ResourceResponseDto>>(
                listJson,
                object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type,
            )

        assertThat(resulList).hasSize(1)
        assertThat(resulList[0].id).isEqualTo(validResources[0].id)
    }

    @Test
    fun `resources with valid fields for v5 default with totp type should not be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to V5DefaultWithTotp.slug),
                ),
            )
        }
        val validResources =
            listOf(
                ResourceResponseV5Dto(
                    id = UUID.randomUUID(),
                    resourceTypeId = testedResourceTypeUuid,
                    resourceFolderId = null,
                    permission = PermissionDto(UUID.randomUUID(), 1, "", UUID.randomUUID(), "", UUID.randomUUID(), "", ""),
                    favorite = null,
                    modified = "",
                    tags = emptyList(),
                    permissions = emptyList(),
                    expired = null,
                    metadataKeyType = MetadataKeyTypeDto.SHARED,
                    metadata = "encrypted metadata",
                    metadataKeyId = UUID.randomUUID(),
                ),
            )
        mockMetadataDecryptor.stub {
            onBlocking { decryptMetadata(any()) }.doReturn(
                MetadataDecryptor.Output.Success(
                    """
                    {
                        "object_type": "PASSBOLT_RESOURCE_METADATA",
                        "name": "name",
                        "uris": ["uri1"],
                        "username": "username",
                        "description": "description"
                    }
                    """.trimIndent(),
                ),
            )
        }

        val listJson = gson.toJson(validResources)
        val resulList =
            gson.fromJson<List<ResourceResponseDto>>(
                listJson,
                object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type,
            )

        assertThat(resulList).hasSize(1)
        assertThat(resulList[0].id).isEqualTo(validResources[0].id)
    }

    private companion object {
        private val testedResourceTypeUuid = UUID.randomUUID()

        const val PASSWORD_AND_DESCRIPTION_NAME_MIN_LENGTH = 0
        const val PASSWORD_AND_DESCRIPTION_NAME_MAX_LENGTH = 255
        const val PASSWORD_AND_DESCRIPTION_USERNAME_MIN_LENGTH = 0
        const val PASSWORD_AND_DESCRIPTION_USERNAME_MAX_LENGTH = 255
        const val PASSWORD_AND_DESCRIPTION_URI_MIN_LENGTH = 0
        const val PASSWORD_AND_DESCRIPTION_URI_MAX_LENGTH = 1024

        const val PASSWORD_DESCRIPTION_TOTP_NAME_MIN_LENGTH = 0
        const val PASSWORD_DESCRIPTION_TOTP_NAME_MAX_LENGTH = 255
        const val PASSWORD_DESCRIPTION_TOTP_USERNAME_MIN_LENGTH = 0
        const val PASSWORD_DESCRIPTION_TOTP_USERNAME_MAX_LENGTH = 255
        const val PASSWORD_DESCRIPTION_TOTP_URI_MIN_LENGTH = 0
        const val PASSWORD_DESCRIPTION_TOTP_URI_MAX_LENGTH = 1024

        const val PASSWORD_STRING_NAME_MIN_LENGTH = 0
        const val PASSWORD_STRING_NAME_MAX_LENGTH = 255
        const val PASSWORD_STRING_USERNAME_MIN_LENGTH = 0
        const val PASSWORD_STRING_USERNAME_MAX_LENGTH = 255
        const val PASSWORD_STRING_URI_MIN_LENGTH = 0
        const val PASSWORD_STRING_URI_MAX_LENGTH = 1024
        const val PASSWORD_STRING_DESCRIPTION_MIN_LENGTH = 0
        const val PASSWORD_STRING_DESCRIPTION_MAX_LENGTH = 10_000

        const val TOTP_NAME_MIN_LENGTH = 0
        const val TOTP_NAME_MAX_LENGTH = 255
        const val TOTP_URI_MIN_LENGTH = 0
        const val TOTP_URI_MAX_LENGTH = 1024
    }
}
