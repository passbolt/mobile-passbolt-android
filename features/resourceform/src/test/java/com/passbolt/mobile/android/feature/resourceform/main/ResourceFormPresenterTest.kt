package com.passbolt.mobile.android.feature.resourceform.main

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.fulldatarefresh.DataRefreshStatus
import com.passbolt.mobile.android.core.fulldatarefresh.HomeDataInteractor.Output.Success
import com.passbolt.mobile.android.core.passwordgenerator.codepoints.toCodepoints
import com.passbolt.mobile.android.feature.resourceform.usecase.GetDefaultCreateContentTypeUseCase
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.MetadataTypeModel
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.PasswordStrength
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.ResourceFormUiModel
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Metadata.DESCRIPTION
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Secret.PASSWORD
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Secret.NOTE
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Secret.TOTP
import com.passbolt.mobile.android.ui.TotpUiModel
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

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

class ResourceFormPresenterTest : KoinTest {

    private val presenter: ResourceFormContract.Presenter by inject()
    private val view: ResourceFormContract.View = mock()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testResourceFormModule)
    }

    @Before
    fun setUp() {
        mockFullDataRefreshExecutor.stub {
            onBlocking { dataRefreshStatusFlow }.doReturn(flowOf(DataRefreshStatus.Finished(Success)))
        }
    }

    @Test
    fun `view should show correct ui for create totp`() {
        mockGetDefaultCreateContentTypeUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(
                GetDefaultCreateContentTypeUseCase.Output(
                    metadataType = MetadataTypeModel.V5,
                    contentType = ContentType.V5TotpStandalone
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

        verify(view).showInitializationProgress()
        verify(view).showCreateTotpTitle()
        verify(view).showName("")
        verify(view).showCreateButton()
        verify(view).showTotpIssuer("")
        verify(view).showTotpSecret("")
        argumentCaptor<TotpUiModel> {
            verify(view).addTotpLeadingForm(capture())
            assertThat(firstValue.secret).isEqualTo("")
            assertThat(firstValue.issuer).isEqualTo("")
            assertThat(firstValue.algorithm).isEqualTo(OtpParseResult.OtpQr.Algorithm.DEFAULT.name)
            assertThat(firstValue.expiry).isEqualTo(OtpParseResult.OtpQr.TotpQr.DEFAULT_PERIOD_SECONDS.toString())
            assertThat(firstValue.length).isEqualTo(OtpParseResult.OtpQr.TotpQr.DEFAULT_DIGITS.toString())
        }
        verify(view).hideInitializationProgress()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `view should show correct ui for create password`() {
        mockGetDefaultCreateContentTypeUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(
                GetDefaultCreateContentTypeUseCase.Output(
                    metadataType = MetadataTypeModel.V5,
                    contentType = ContentType.V5Default
                )
            )
        }
        mockEntropyCalculator.stub {
            onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
        }

        presenter.attach(view)
        presenter.argsRetrieved(
            ResourceFormMode.Create(
                leadingContentType = LeadingContentType.PASSWORD,
                parentFolderId = null
            )
        )

        verify(view).showInitializationProgress()
        verify(view).showCreatePasswordTitle()
        verify(view).showName("")
        verify(view).showCreateButton()
        verify(view).showPasswordMainUri("")
        verify(view).showPasswordUsername("")
        verify(view).addPasswordLeadingForm("", PasswordStrength.Empty, 0.0)
        verify(view).showPassword("".toCodepoints(), 0.0, PasswordStrength.Empty)
        verify(view).hideInitializationProgress()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `advanced settings should show additional password sections`() {
        mockGetDefaultCreateContentTypeUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(
                GetDefaultCreateContentTypeUseCase.Output(
                    metadataType = MetadataTypeModel.V5,
                    contentType = ContentType.V5Default
                )
            )
        }

        presenter.attach(view)
        presenter.argsRetrieved(
            ResourceFormMode.Create(
                leadingContentType = LeadingContentType.PASSWORD,
                parentFolderId = null
            )
        )
        presenter.advancedSettingsClick()

        argumentCaptor<List<ResourceFormUiModel.Secret>> {
            verify(view).setupAdditionalSecrets(capture())
            assertThat(firstValue).containsExactly(NOTE, TOTP)
        }
        argumentCaptor<List<ResourceFormUiModel.Metadata>> {
            verify(view).setupMetadata(capture())
            assertThat(firstValue).containsExactly(DESCRIPTION)
        }
        verify(view).hideAdvancedSettings()
    }

    @Test
    fun `advanced settings should show additional totp sections`() {
        mockGetDefaultCreateContentTypeUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(
                GetDefaultCreateContentTypeUseCase.Output(
                    metadataType = MetadataTypeModel.V5,
                    contentType = ContentType.V5TotpStandalone
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
        presenter.advancedSettingsClick()

        argumentCaptor<List<ResourceFormUiModel.Secret>> {
            verify(view).setupAdditionalSecrets(capture())
            assertThat(firstValue).containsExactly(PASSWORD, NOTE)
        }
        argumentCaptor<List<ResourceFormUiModel.Metadata>> {
            verify(view).setupMetadata(capture())
            assertThat(firstValue).containsExactly(DESCRIPTION)
        }
        verify(view).hideAdvancedSettings()
    }

    @Test
    fun `advanced settings should always once expanded`() {
        presenter.attach(view)
        presenter.argsRetrieved(
            ResourceFormMode.Create(
                leadingContentType = LeadingContentType.PASSWORD,
                parentFolderId = null
            )
        )
        presenter.advancedSettingsClick()

        presenter.argsRetrieved(
            ResourceFormMode.Create(
                leadingContentType = LeadingContentType.PASSWORD,
                parentFolderId = null
            )
        )

        verify(view, times(2)).hideAdvancedSettings()
        verify(view, times(2)).setupAdditionalSecrets(any())
        verify(view, times(2)).setupMetadata(any())
    }

    @Test
    fun `password change should trigger entropy recalculation`() {
        mockGetDefaultCreateContentTypeUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(
                GetDefaultCreateContentTypeUseCase.Output(
                    metadataType = MetadataTypeModel.V5,
                    contentType = ContentType.V5Default
                )
            )
        }
        mockEntropyCalculator.stub {
            onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
        }

        presenter.attach(view)
        presenter.argsRetrieved(
            ResourceFormMode.Create(
                leadingContentType = LeadingContentType.PASSWORD,
                parentFolderId = null
            )
        )
        presenter.passwordTextChanged("t")
        presenter.passwordTextChanged("te")
        presenter.passwordTextChanged("tes")
        presenter.passwordTextChanged("test")

        verify(view, times(4)).showPasswordStrength(any(), any())
    }
}
