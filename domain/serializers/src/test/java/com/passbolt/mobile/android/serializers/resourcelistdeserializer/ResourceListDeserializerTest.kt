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
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetResourceTypeIdToSlugMappingUseCase.Output
import com.passbolt.mobile.android.dto.response.PermissionDto
import com.passbolt.mobile.android.dto.response.ResourceResponseDto
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.PASSWORD_AND_DESCRIPTION_SLUG
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.PASSWORD_DESCRIPTION_TOTP_SLUG
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.PASSWORD_STRING_SLUG
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.TOTP_SLUG
import net.jimblackler.jsonschemafriend.SchemaStore
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import java.util.UUID

class ResourceListDeserializerTest : KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(resourceListDeserializationTestModule)
    }

    private val gson: Gson by inject()

    @Before
    fun setup() {
        mockGetSelectedAccountUseCase.stub {
            onBlocking { execute(Unit) } doReturn GetSelectedAccountUseCase.Output("selectedAccountId")
        }
        mockJSFSchemaRepository.stub {
            on { schemaForResource(PASSWORD_STRING_SLUG) } doReturn SchemaStore().loadSchema(
                this::class.java.getResource("/password-string-resource-schema.json")
            )
            on { schemaForResource(PASSWORD_AND_DESCRIPTION_SLUG) } doReturn SchemaStore().loadSchema(
                this::class.java.getResource("/password-and-description-resource-schema.json")
            )
            on { schemaForResource(PASSWORD_DESCRIPTION_TOTP_SLUG) } doReturn SchemaStore().loadSchema(
                this::class.java.getResource("/password-description-totp-resource-schema.json")
            )
            on { schemaForResource(TOTP_SLUG) } doReturn SchemaStore().loadSchema(
                this::class.java.getResource("/totp-resource-schema.json")
            )
        }
    }

    @Test
    fun `resources with invalid fields for password string type should be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                Output(
                    mapOf(testedResourceTypeUuid to PASSWORD_STRING_SLUG)
                )
            )
        }
        val invalidResources = listOf(
            resourceWithNameOfLength(
                PASSWORD_STRING_NAME_MAX_LENGTH + 1, testedResourceTypeUuid
            ),
            resourceWithUriOfLength(
                PASSWORD_STRING_URI_MAX_LENGTH + 1,
                testedResourceTypeUuid
            ),
            resourceWithDescriptionOfLength(
                PASSWORD_STRING_DESCRIPTION_MAX_LENGTH + 1,
                testedResourceTypeUuid
            ),
            resourceWithUsernameOfLength(
                PASSWORD_STRING_USERNAME_MAX_LENGTH + 1,
                testedResourceTypeUuid
            )
        )

        val listJson = gson.toJson(invalidResources)
        val resulList = gson.fromJson<List<ResourceResponseDto>>(
            listJson,
            object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type
        )

        assertThat(resulList).isEmpty()
    }

    @Test
    fun `resources with invalid fields for password and description type should be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                Output(
                    mapOf(testedResourceTypeUuid to PASSWORD_AND_DESCRIPTION_SLUG)
                )
            )
        }
        val invalidResources = listOf(
            resourceWithNameOfLength(
                PASSWORD_AND_DESCRIPTION_NAME_MAX_LENGTH + 1,
                testedResourceTypeUuid
            ),
            resourceWithUriOfLength(
                PASSWORD_AND_DESCRIPTION_URI_MAX_LENGTH + 1,
                testedResourceTypeUuid
            ),
            resourceWithUsernameOfLength(
                PASSWORD_AND_DESCRIPTION_USERNAME_MAX_LENGTH + 1,
                testedResourceTypeUuid
            )
        )

        val listJson = gson.toJson(invalidResources)
        val resulList = gson.fromJson<List<ResourceResponseDto>>(
            listJson,
            object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type
        )

        assertThat(resulList).isEmpty()
    }

    @Test
    fun `resources with invalid fields for totp type should be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                Output(
                    mapOf(testedResourceTypeUuid to SupportedContentTypes.TOTP_SLUG)
                )
            )
        }
        val invalidResources = listOf(
            resourceWithNameOfLength(
                TOTP_NAME_MAX_LENGTH + 1,
                testedResourceTypeUuid
            ),
            resourceWithUriOfLength(
                TOTP_URI_MAX_LENGTH + 1,
                testedResourceTypeUuid
            ),
        )

        val listJson = gson.toJson(invalidResources)
        val resulList = gson.fromJson<List<ResourceResponseDto>>(
            listJson,
            object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type
        )

        assertThat(resulList).isEmpty()
    }

    @Test
    fun `resources with invalid fields for password description totp type should be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                Output(
                    mapOf(testedResourceTypeUuid to PASSWORD_DESCRIPTION_TOTP_SLUG)
                )
            )
        }
        val invalidResources = listOf(
            resourceWithNameOfLength(
                PASSWORD_DESCRIPTION_TOTP_NAME_MAX_LENGTH + 1,
                testedResourceTypeUuid
            ),
            resourceWithUriOfLength(
                PASSWORD_DESCRIPTION_TOTP_URI_MAX_LENGTH + 1,
                testedResourceTypeUuid
            ),
            resourceWithUsernameOfLength(
                PASSWORD_DESCRIPTION_TOTP_USERNAME_MAX_LENGTH + 1,
                testedResourceTypeUuid
            )
        )

        val listJson = gson.toJson(invalidResources)
        val resulList = gson.fromJson<List<ResourceResponseDto>>(
            listJson,
            object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type
        )

        assertThat(resulList).isEmpty()
    }

    @Test
    fun `optional fields should pass validation`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                Output(
                    mapOf(testedResourceTypeUuid to PASSWORD_AND_DESCRIPTION_SLUG)
                )
            )
        }
        val validResources = listOf(
            ResourceResponseDto(
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
                expired = null
            )
        )

        val listJson = gson.toJson(validResources)
        val resulList = gson.fromJson<List<ResourceResponseDto>>(
            listJson,
            object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type
        )

        assertThat(resulList.size).isEqualTo(1)
    }

    @Test
    fun `resource with valid fields for password string type should not be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                Output(
                    mapOf(testedResourceTypeUuid to PASSWORD_STRING_SLUG)
                )
            )
        }
        val validResources = listOf(
            ResourceResponseDto(
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
                expired = null
            )
        )

        val listJson = gson.toJson(validResources)
        val resulList = gson.fromJson<List<ResourceResponseDto>>(
            listJson,
            object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type
        )

        assertThat(resulList).hasSize(1)
        assertThat(resulList[0]).isEqualTo(validResources[0])
    }

    @Test
    fun `resources with valid fields for password and description type should not be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                Output(
                    mapOf(testedResourceTypeUuid to PASSWORD_AND_DESCRIPTION_SLUG)
                )
            )
        }
        val validResources = listOf(
            ResourceResponseDto(
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
                expired = null
            )
        )

        val listJson = gson.toJson(validResources)
        val resulList = gson.fromJson<List<ResourceResponseDto>>(
            listJson,
            object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type
        )

        assertThat(resulList).hasSize(1)
        assertThat(resulList[0]).isEqualTo(validResources[0])
    }

    @Test
    fun `resources with valid fields for totp type should not be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                Output(
                    mapOf(testedResourceTypeUuid to SupportedContentTypes.TOTP_SLUG)
                )
            )
        }
        val validResources = listOf(
            ResourceResponseDto(
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
                expired = null
            )
        )

        val listJson = gson.toJson(validResources)
        val resulList = gson.fromJson<List<ResourceResponseDto>>(
            listJson,
            object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type
        )

        assertThat(resulList).hasSize(1)
        assertThat(resulList[0]).isEqualTo(validResources[0])
    }

    @Test
    fun `resources with valid fields for password description totp type should not be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                Output(
                    mapOf(testedResourceTypeUuid to PASSWORD_DESCRIPTION_TOTP_SLUG)
                )
            )
        }
        val validResources = listOf(
            ResourceResponseDto(
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
                expired = null
            )
        )

        val listJson = gson.toJson(validResources)
        val resulList = gson.fromJson<List<ResourceResponseDto>>(
            listJson,
            object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type
        )

        assertThat(resulList).hasSize(1)
        assertThat(resulList[0]).isEqualTo(validResources[0])
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
