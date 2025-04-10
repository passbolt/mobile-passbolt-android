package com.passbolt.mobile.android.feature.resourceform.main.resourcemodelhandler.v4.leadingtotp

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.fulldatarefresh.DataRefreshStatus
import com.passbolt.mobile.android.core.fulldatarefresh.HomeDataInteractor.Output.Success
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormContract
import com.passbolt.mobile.android.feature.resourceform.main.ResourceModelHandler
import com.passbolt.mobile.android.feature.resourceform.main.mockFullDataRefreshExecutor
import com.passbolt.mobile.android.feature.resourceform.main.mockGetDefaultCreateContentTypeUseCase
import com.passbolt.mobile.android.feature.resourceform.main.testResourceFormModule
import com.passbolt.mobile.android.feature.resourceform.usecase.GetDefaultCreateContentTypeUseCase
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.MetadataTypeModel
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.PasswordUiModel
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

class V4TotpResourceFormPresenterTest : KoinTest {

    private val presenter: ResourceFormContract.Presenter by inject()
    private val view: ResourceFormContract.View = mock()
    private val resourceModelHandler: ResourceModelHandler by inject()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testResourceFormModule)
    }

    @Before
    fun setUp() = runTest {
        mockFullDataRefreshExecutor.stub {
            onBlocking { dataRefreshStatusFlow }.doReturn(flowOf(DataRefreshStatus.Finished(Success)))
        }
        mockGetDefaultCreateContentTypeUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(
                GetDefaultCreateContentTypeUseCase.Output(
                    metadataType = MetadataTypeModel.V4,
                    contentType = ContentType.Totp
                )
            )
        }
        presenter.attach(view)
        presenter.argsRetrieved(
            ResourceFormMode.Create(
                leadingContentType = LeadingContentType.TOTP,
                parentFolderId = null
            )
        )
    }

    @Test
    fun `leading content type totp should initialize empty totp model`() = runTest {
        assertThat(resourceModelHandler.contentType).isEqualTo(ContentType.Totp)
        assertThat(resourceModelHandler.metadataType).isEqualTo(MetadataTypeModel.V4)

        JSONAssert.assertEquals(
            """
                {
                    "name": ""
                }
            """.trimIndent(),
            resourceModelHandler.resourceMetadata.json, STRICT_MODE_ENABLED
        )
        JSONAssert.assertEquals(
            """
                {
                    "totp": {
                        "secret_key": "",
                        "period": ${OtpParseResult.OtpQr.TotpQr.DEFAULT_PERIOD_SECONDS},
                        "digits": ${OtpParseResult.OtpQr.TotpQr.DEFAULT_DIGITS},
                        "algorithm": ${OtpParseResult.OtpQr.Algorithm.DEFAULT.name}
                    }
                }
            """.trimIndent(),
            resourceModelHandler.resourceSecret.json, STRICT_MODE_ENABLED
        )
    }

    @Test
    fun `edit metadata should not change content type and apply changes`() = runTest {
        val mockName = "test name"
        val mockMainUri = "test uri"

        presenter.nameTextChanged(mockName)
        presenter.totpUrlChanged(mockMainUri)

        JSONAssert.assertEquals(
            """
                {
                    "name": "$mockName",
                    "uri": "$mockMainUri"
                }
            """.trimIndent(),
            resourceModelHandler.resourceMetadata.json, STRICT_MODE_ENABLED
        )
    }

    @Test
    fun `add totp should not change content type and apply changes`() = runTest {
        val mockTotpSecret = "test secret"
        val mockPeriod = "123"
        val mockDigits = "456"
        val mockAlgorithm = OtpParseResult.OtpQr.Algorithm.SHA512.name

        presenter.totpSecretChanged(mockTotpSecret)
        presenter.totpAdvancedSettingsChanged(
            TotpUiModel(
                secret = "",
                issuer = "",
                algorithm = mockAlgorithm,
                expiry = mockPeriod,
                length = mockDigits
            )
        )

        JSONAssert.assertEquals(
            """
                {
                    "totp": {
                        "secret_key": "$mockTotpSecret",
                        "period": $mockPeriod,
                        "digits": $mockDigits,
                        "algorithm": "$mockAlgorithm"
                    }
                }
            """.trimIndent(),
            resourceModelHandler.resourceSecret.json, STRICT_MODE_ENABLED
        )
    }

    @Test
    fun `add note should change content type and apply changes`() = runTest {
        val mockName = "test name"
        val mockUrl = "test url"
        val mockTotpSecret = "test secret"
        val mockPeriod = "123"
        val mockDigits = "456"
        val mockAlgorithm = OtpParseResult.OtpQr.Algorithm.SHA512.name
        val mockNote = "test note"

        presenter.nameTextChanged(mockName)
        presenter.totpUrlChanged(mockUrl)
        presenter.totpSecretChanged(mockTotpSecret)
        presenter.totpAdvancedSettingsChanged(
            TotpUiModel(
                secret = "",
                issuer = "",
                algorithm = mockAlgorithm,
                expiry = mockPeriod,
                length = mockDigits
            )
        )
        presenter.noteChanged(mockNote)

        assertThat(resourceModelHandler.contentType).isEqualTo(ContentType.PasswordDescriptionTotp)
        JSONAssert.assertEquals(
            """
                {
                    "name": "$mockName",
                    "uri": "$mockUrl"
                }
            """.trimIndent(),
            resourceModelHandler.resourceMetadata.json, STRICT_MODE_ENABLED
        )
        JSONAssert.assertEquals(
            """
                {
                    "description": "$mockNote",
                    "password": "",
                    "totp": {
                        "secret_key": "$mockTotpSecret",
                        "period": $mockPeriod,
                        "digits": $mockDigits,
                        "algorithm": "$mockAlgorithm"
                    }
                }
            """.trimIndent(),
            resourceModelHandler.resourceSecret.json, STRICT_MODE_ENABLED
        )
    }

    @Test
    fun `add password should change content type and apply changes`() = runTest {
        val mockName = "test name"
        val mockUrl = "test url"
        val mockTotpSecret = "test secret"
        val mockPeriod = "123"
        val mockDigits = "456"
        val mockAlgorithm = OtpParseResult.OtpQr.Algorithm.SHA512.name
        val mockPassword = "test password"
        val mockMainUri = "test url changed"
        val mockUsername = "test username"

        presenter.nameTextChanged(mockName)
        presenter.totpUrlChanged(mockUrl)
        presenter.totpSecretChanged(mockTotpSecret)
        presenter.totpAdvancedSettingsChanged(
            TotpUiModel(
                secret = "",
                issuer = "",
                algorithm = mockAlgorithm,
                expiry = mockPeriod,
                length = mockDigits
            )
        )
        presenter.passwordChanged(
            PasswordUiModel(
                password = mockPassword,
                mainUri = mockMainUri,
                username = mockUsername
            )
        )

        assertThat(resourceModelHandler.contentType).isEqualTo(ContentType.PasswordDescriptionTotp)
        JSONAssert.assertEquals(
            """
                {
                    "name": "$mockName",
                    "username": "$mockUsername",
                    "uri": "$mockMainUri"
                }
            """.trimIndent(),
            resourceModelHandler.resourceMetadata.json, STRICT_MODE_ENABLED
        )
        JSONAssert.assertEquals(
            """
                {
                    "description": "",
                    "password": "$mockPassword",
                    "totp": {
                        "secret_key": "$mockTotpSecret",
                        "period": $mockPeriod,
                        "digits": $mockDigits,
                        "algorithm": "$mockAlgorithm"
                    }
                }
            """.trimIndent(),
            resourceModelHandler.resourceSecret.json, STRICT_MODE_ENABLED
        )
    }

    @Test
    fun `scan totp should apply model changes`() = runTest {
        val scannedTotp = OtpParseResult.OtpQr.TotpQr(
            label = "label",
            secret = "secret",
            issuer = "issuer",
            algorithm = OtpParseResult.OtpQr.Algorithm.SHA1,
            digits = 6,
            period = 30
        )

        presenter.totpScanned(isManualCreationChosen = false, scannedTotp = scannedTotp)

        assertThat(resourceModelHandler.contentType).isEqualTo(ContentType.Totp)
        JSONAssert.assertEquals(
            """
                {
                    "name": "${scannedTotp.label}",
                    "uri": "${scannedTotp.issuer}"
                }
            """.trimIndent(),
            resourceModelHandler.resourceMetadata.json, STRICT_MODE_ENABLED
        )
        JSONAssert.assertEquals(
            """
                {
                    "totp": {
                        "secret_key": "${scannedTotp.secret}",
                        "period": ${scannedTotp.period},
                        "digits": ${scannedTotp.digits},
                        "algorithm": "${scannedTotp.algorithm.name}"
                    }
                }
            """.trimIndent(),
            resourceModelHandler.resourceSecret.json, STRICT_MODE_ENABLED
        )
    }

    private companion object {
        private const val STRICT_MODE_ENABLED = true
    }
}
