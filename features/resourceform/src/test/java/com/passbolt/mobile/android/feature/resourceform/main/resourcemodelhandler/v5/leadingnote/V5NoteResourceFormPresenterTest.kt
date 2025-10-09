package com.passbolt.mobile.android.feature.resourceform.main.resourcemodelhandler.v5.leadingnote

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.resources.usecase.GetDefaultCreateContentTypeUseCase
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormContract
import com.passbolt.mobile.android.feature.resourceform.main.ResourceModelHandler
import com.passbolt.mobile.android.feature.resourceform.main.mockGetDefaultCreateContentTypeUseCase
import com.passbolt.mobile.android.feature.resourceform.main.testResourceFormModule
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.MetadataTypeModel
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.PasswordUiModel
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.TotpUiModel
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.skyscreamer.jsonassert.JSONAssert

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

class V5NoteResourceFormPresenterTest : KoinTest {
    private val presenter: ResourceFormContract.Presenter by inject()
    private val view: ResourceFormContract.View = mock()
    private val resourceModelHandler: ResourceModelHandler by inject()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testResourceFormModule)
        }

    @Before
    fun setUp() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Note,
                    ),
                )
            }

            presenter.attach(view)
            presenter.argsRetrieved(
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.STANDALONE_NOTE,
                    parentFolderId = null,
                ),
            )
        }

    @Test
    fun `leading content type note should initialize empty description model`() =
        runTest {
            assertThat(resourceModelHandler.contentType).isEqualTo(ContentType.V5Note)
            assertThat(resourceModelHandler.metadataType).isEqualTo(MetadataTypeModel.V5)

            JSONAssert.assertEquals(
                """
                {
                    "name": ""
                }
                """.trimIndent(),
                resourceModelHandler.resourceMetadata.json,
                STRICT_MODE_ENABLED,
            )
            JSONAssert.assertEquals(
                """
                {
                    "description": ""
                }
                """.trimIndent(),
                resourceModelHandler.resourceSecret.json,
                STRICT_MODE_ENABLED,
            )
        }

    @Test
    fun `edit metadata should not change content type and apply changes`() =
        runTest {
            val mockName = "test name"
            val mockMainUri = "test uri"

            presenter.nameTextChanged(mockName)
            presenter.passwordMainUriTextChanged(mockMainUri)

            JSONAssert.assertEquals(
                """
                {
                    "name": "$mockName",
                    "uris": ["$mockMainUri"]
                }
                """.trimIndent(),
                resourceModelHandler.resourceMetadata.json,
                STRICT_MODE_ENABLED,
            )
            JSONAssert.assertEquals(
                """
                {
                    "description": "",
                }
                """.trimIndent(),
                resourceModelHandler.resourceSecret.json,
                STRICT_MODE_ENABLED,
            )
        }

    @Test
    fun `add note should not change content type and apply changes`() =
        runTest {
            val mockNote = "note"

            presenter.noteChanged(mockNote)

            JSONAssert.assertEquals(
                """
                {
                    "description": "$mockNote",
                }
                """.trimIndent(),
                resourceModelHandler.resourceSecret.json,
                STRICT_MODE_ENABLED,
            )
        }

    @Test
    fun `add totp should change content type and apply changes`() =
        runTest {
            val mockName = "test name"
            val mockUrl = "test url"
            val mockTotpSecret = "test secret"
            val mockPeriod = "123"
            val mockDigits = "456"
            val mockAlgorithm = OtpParseResult.OtpQr.Algorithm.SHA512.name

            presenter.nameTextChanged(mockName)
            presenter.totpChanged(
                TotpUiModel(
                    secret = mockTotpSecret,
                    issuer = mockUrl,
                    algorithm = mockAlgorithm,
                    expiry = mockPeriod,
                    length = mockDigits,
                ),
            )

            assertThat(resourceModelHandler.contentType).isEqualTo(ContentType.V5DefaultWithTotp)
            JSONAssert.assertEquals(
                """
                {
                    "name": "$mockName",
                    "uris": ["$mockUrl"]
                }
                """.trimIndent(),
                resourceModelHandler.resourceMetadata.json,
                STRICT_MODE_ENABLED,
            )
            JSONAssert.assertEquals(
                """
                {
                    "description": "",
                    "password": "",
                    "totp": {
                        "secret_key": "$mockTotpSecret",
                        "period": $mockPeriod,
                        "digits": $mockDigits,
                        "algorithm": "$mockAlgorithm"
                    },
                    "custom_fields": []
                }
                """.trimIndent(),
                resourceModelHandler.resourceSecret.json,
                STRICT_MODE_ENABLED,
            )
        }

    @Test
    fun `add password should change content type and apply changes`() =
        runTest {
            val mockName = "test name"

            val mockPassword = "password"
            val mockUrl = "test url 1"
            val mockUsername = "username"
            val mockNote = "description"

            presenter.nameTextChanged(mockName)
            presenter.noteChanged(mockNote)
            presenter.passwordChanged(
                PasswordUiModel(
                    password = mockPassword,
                    mainUri = mockUrl,
                    username = mockUsername,
                ),
            )

            assertThat(resourceModelHandler.contentType).isEqualTo(ContentType.V5Default)
            JSONAssert.assertEquals(
                """
                {
                    "name": "$mockName",
                    "uris": ["$mockUrl"],
                    "username": "$mockUsername"
                }
                """.trimIndent(),
                resourceModelHandler.resourceMetadata.json,
                STRICT_MODE_ENABLED,
            )
            JSONAssert.assertEquals(
                """
                {
                    "description": "$mockNote",
                    "password": "$mockPassword",
                    "custom_fields": []
                }
                """.trimIndent(),
                resourceModelHandler.resourceSecret.json,
                STRICT_MODE_ENABLED,
            )
        }

    @Test
    fun `add metadata description to password should apply changes`() =
        runTest {
            val mockName = "test name"
            val mockMetadataDescription = "md description"

            presenter.nameTextChanged(mockName)
            presenter.metadataDescriptionChanged(mockMetadataDescription)

            assertThat(resourceModelHandler.contentType).isEqualTo(ContentType.V5Note)
            JSONAssert.assertEquals(
                """
                {
                    "name": "$mockName",
                    "description": "$mockMetadataDescription",
                }
                """.trimIndent(),
                resourceModelHandler.resourceMetadata.json,
                STRICT_MODE_ENABLED,
            )
            JSONAssert.assertEquals(
                """
                {
                    "description": "",
                }
                """.trimIndent(),
                resourceModelHandler.resourceSecret.json,
                STRICT_MODE_ENABLED,
            )
        }

    private companion object {
        private const val STRICT_MODE_ENABLED = true
    }
}
