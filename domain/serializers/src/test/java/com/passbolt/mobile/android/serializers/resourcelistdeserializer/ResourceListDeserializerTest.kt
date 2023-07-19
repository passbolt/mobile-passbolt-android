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
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetResourceTypeIdToSlugMappingUseCase
import com.passbolt.mobile.android.dto.response.PermissionDto
import com.passbolt.mobile.android.dto.response.ResourceResponseDto
import com.passbolt.mobile.android.serializers.SupportedContentTypes
import com.passbolt.mobile.android.serializers.gson.validation.PasswordAndDescriptionResourceValidation
import com.passbolt.mobile.android.serializers.gson.validation.PasswordDescriptionTotpResourceValidation
import com.passbolt.mobile.android.serializers.gson.validation.PasswordStringResourceValidation
import com.passbolt.mobile.android.serializers.gson.validation.TotpResourceValidation
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
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
    }

    @Test
    fun `resources with invalid fields for password string type should be filtered`() {
        mockIdToSlugMappingUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to SupportedContentTypes.PASSWORD_STRING_SLUG)
                )
            )
        }
        val invalidResources = listOf(
            resourceWithNameOfLength(
                PasswordStringResourceValidation.PASSWORD_STRING_NAME_MAX_LENGTH + 1, testedResourceTypeUuid
            ),
            resourceWithUriOfLength(
                PasswordStringResourceValidation.PASSWORD_STRING_URI_MAX_LENGTH + 1,
                testedResourceTypeUuid
            ),
            resourceWithDescriptionOfLength(
                PasswordStringResourceValidation.PASSWORD_STRING_DESCRIPTION_MAX_LENGTH + 1,
                testedResourceTypeUuid
            ),
            resourceWithUsernameOfLength(
                PasswordStringResourceValidation.PASSWORD_STRING_USERNAME_MAX_LENGTH + 1,
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
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to SupportedContentTypes.PASSWORD_AND_DESCRIPTION_SLUG)
                )
            )
        }
        val invalidResources = listOf(
            resourceWithNameOfLength(
                PasswordAndDescriptionResourceValidation.PASSWORD_AND_DESCRIPTION_NAME_MAX_LENGTH + 1,
                testedResourceTypeUuid
            ),
            resourceWithUriOfLength(
                PasswordAndDescriptionResourceValidation.PASSWORD_AND_DESCRIPTION_URI_MAX_LENGTH + 1,
                testedResourceTypeUuid
            ),
            resourceWithUsernameOfLength(
                PasswordAndDescriptionResourceValidation.PASSWORD_AND_DESCRIPTION_USERNAME_MAX_LENGTH + 1,
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
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to SupportedContentTypes.TOTP_SLUG)
                )
            )
        }
        val invalidResources = listOf(
            resourceWithNameOfLength(
                TotpResourceValidation.TOTP_NAME_MAX_LENGTH + 1,
                testedResourceTypeUuid
            ),
            resourceWithUriOfLength(
                TotpResourceValidation.TOTP_URI_MAX_LENGTH + 1,
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
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to SupportedContentTypes.PASSWORD_DESCRIPTION_TOTP_SLUG)
                )
            )
        }
        val invalidResources = listOf(
            resourceWithNameOfLength(
                PasswordDescriptionTotpResourceValidation.PASSWORD_DESCRIPTION_TOTP_NAME_MAX_LENGTH + 1,
                testedResourceTypeUuid
            ),
            resourceWithUriOfLength(
                PasswordDescriptionTotpResourceValidation.PASSWORD_DESCRIPTION_TOTP_URI_MAX_LENGTH + 1,
                testedResourceTypeUuid
            ),
            resourceWithUsernameOfLength(
                PasswordDescriptionTotpResourceValidation.PASSWORD_DESCRIPTION_TOTP_USERNAME_MAX_LENGTH + 1,
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
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    mapOf(testedResourceTypeUuid to SupportedContentTypes.PASSWORD_AND_DESCRIPTION_SLUG)
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
                permissions = emptyList()
            )
        )

        val listJson = gson.toJson(validResources)
        val resulList = gson.fromJson<List<ResourceResponseDto>>(
            listJson,
            object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type
        )

        assertThat(resulList.size).isEqualTo(1)
    }

    private companion object {
        private val testedResourceTypeUuid = UUID.randomUUID()
    }
}
