package com.passbolt.mobile.android.feature.resourceform.main.resourcemodelhandler.v4.leadingpassword

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.fulldatarefresh.DataRefreshStatus
import com.passbolt.mobile.android.core.fulldatarefresh.HomeDataInteractor.Output.Success
import com.passbolt.mobile.android.core.resources.usecase.GetDefaultCreateContentTypeUseCase
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormContract
import com.passbolt.mobile.android.feature.resourceform.main.ResourceModelHandler
import com.passbolt.mobile.android.feature.resourceform.main.mockEntropyCalculator
import com.passbolt.mobile.android.feature.resourceform.main.mockFullDataRefreshExecutor
import com.passbolt.mobile.android.feature.resourceform.main.mockGetDefaultCreateContentTypeUseCase
import com.passbolt.mobile.android.feature.resourceform.main.testResourceFormModule
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.MetadataTypeModel
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.TotpUiModel
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

class V4PasswordResourceFormPresenterTest : KoinTest {
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
            mockFullDataRefreshExecutor.stub {
                onBlocking { dataRefreshStatusFlow }.doReturn(flowOf(DataRefreshStatus.Finished(Success)))
            }
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V4,
                        contentType = ContentType.PasswordAndDescription,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            presenter.attach(view)
            presenter.argsRetrieved(
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                ),
            )
        }

    @Test
    fun `leading content type password should initialize empty password model`() =
        runTest {
            assertThat(resourceModelHandler.contentType).isEqualTo(ContentType.PasswordAndDescription)
            assertThat(resourceModelHandler.metadataType).isEqualTo(MetadataTypeModel.V4)

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
                    "password": ""
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
            val mockPassword = "test password"

            presenter.nameTextChanged(mockName)
            presenter.passwordMainUriTextChanged(mockMainUri)
            presenter.passwordTextChanged(mockPassword)

            JSONAssert.assertEquals(
                """
                {
                    "name": "$mockName",
                    "uri": "$mockMainUri"
                }
                """.trimIndent(),
                resourceModelHandler.resourceMetadata.json,
                STRICT_MODE_ENABLED,
            )
            JSONAssert.assertEquals(
                """
                {
                    "password": "$mockPassword",
                    "description": ""
                }
                """.trimIndent(),
                resourceModelHandler.resourceSecret.json,
                STRICT_MODE_ENABLED,
            )
        }

    @Test
    fun `add password should not change content type and apply changes`() =
        runTest {
            val mockPassword = "test password"

            presenter.passwordTextChanged(mockPassword)

            JSONAssert.assertEquals(
                """
                {
                    "password": "$mockPassword",
                    "description": ""
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
                    "password": "",
                    "description": "$mockNote"
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

            assertThat(resourceModelHandler.contentType).isEqualTo(ContentType.PasswordDescriptionTotp)
            JSONAssert.assertEquals(
                """
                {
                    "name": "$mockName",
                    "uri": "$mockUrl"
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
                    }
                }
                """.trimIndent(),
                resourceModelHandler.resourceSecret.json,
                STRICT_MODE_ENABLED,
            )
        }

    @Test
    fun `add metadata description to password should not be possible`() =
        runTest {
            val mockName = "test name"
            val mockMetadataDescription = "md description"

            presenter.nameTextChanged(mockName)
            presenter.metadataDescriptionChanged(mockMetadataDescription)

            assertThat(resourceModelHandler.contentType).isEqualTo(ContentType.PasswordAndDescription)
            JSONAssert.assertEquals(
                """
                {
                    "name": "$mockName",
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
