package com.passbolt.mobile.android.feature.resourceform.main.resourcemodelhandler.v5.leadingnote

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.resources.usecase.GetDefaultCreateContentTypeUseCase
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.DescriptionResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.NameTextChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.NoteChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.PasswordMainUriTextChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.PasswordResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.TotpResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormViewModel
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.parameter.parametersOf
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.koin.test.inject
import org.mockito.kotlin.any
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

@OptIn(ExperimentalCoroutinesApi::class)
class V5NoteResourceFormViewModelTest : KoinTest {
    private lateinit var viewModel: ResourceFormViewModel
    private val resourceModelHandler: ResourceModelHandler by inject()

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testResourceFormModule)
        }

    @Before
    fun setUp() =
        runTest {
            Dispatchers.setMain(testDispatcher)
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Note,
                    ),
                )
            }

            viewModel = get { parametersOf(mode) }
            testScheduler.advanceUntilIdle()
        }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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

            viewModel.onIntent(NameTextChanged(mockName))
            viewModel.onIntent(PasswordMainUriTextChanged(mockMainUri))

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

            viewModel.onIntent(NoteChanged(mockNote))

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

            viewModel.onIntent(NameTextChanged(mockName))
            viewModel.onIntent(
                TotpResult(
                    TotpUiModel(
                        secret = mockTotpSecret,
                        issuer = mockUrl,
                        algorithm = mockAlgorithm,
                        expiry = mockPeriod,
                        length = mockDigits,
                    ),
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

            viewModel.onIntent(NameTextChanged(mockName))
            viewModel.onIntent(NoteChanged(mockNote))
            viewModel.onIntent(
                PasswordResult(
                    PasswordUiModel(
                        password = mockPassword,
                        mainUri = mockUrl,
                        username = mockUsername,
                    ),
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

            viewModel.onIntent(NameTextChanged(mockName))
            viewModel.onIntent(DescriptionResult(mockMetadataDescription))

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

        private val mode =
            ResourceFormMode.Create(
                leadingContentType = LeadingContentType.STANDALONE_NOTE,
                parentFolderId = null,
            )
    }
}
