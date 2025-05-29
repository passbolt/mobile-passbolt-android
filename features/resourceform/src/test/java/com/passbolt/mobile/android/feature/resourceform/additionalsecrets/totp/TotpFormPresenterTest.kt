package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.TotpUiModel
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
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

class TotpFormPresenterTest : KoinTest {
    private val presenter: TotpFormContract.Presenter by inject()
    private val view: TotpFormContract.View = mock()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(totpFormModule)
        }

    @Test
    fun `view should show correct create title and totp on attach`() {
        presenter.attach(view)
        presenter.argsRetrieved(
            ResourceFormMode.Create(
                leadingContentType = LeadingContentType.TOTP,
                parentFolderId = null,
            ),
            totp,
        )

        verify(view).showCreateTitle()
        verify(view).showSecret(totp.secret)
        verify(view).showUrl(totp.issuer)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `secret and issuer changes should be applied`() {
        val changedSecret = "changed secret"
        val changedIssuer = "changed issuer"

        presenter.attach(view)
        presenter.argsRetrieved(
            ResourceFormMode.Create(
                leadingContentType = LeadingContentType.TOTP,
                parentFolderId = null,
            ),
            totp,
        )
        presenter.totpSecretChanged(changedSecret)
        presenter.totpUrlChanged(changedIssuer)
        presenter.applyClick()

        argumentCaptor<TotpUiModel> {
            verify(view).goBackWithResult(capture())
            assertThat(firstValue.secret).isEqualTo(changedSecret)
            assertThat(firstValue.issuer).isEqualTo(changedIssuer)
        }
    }

    @Test
    fun `advanced settings changes should be applied`() {
        val changedExpiry = "31"
        val changedLength = "7"
        val changedAlgorithm = OtpParseResult.OtpQr.Algorithm.SHA1.name
        val changedAdvancedSettings =
            totp.copy(
                expiry = changedExpiry,
                length = changedLength,
                algorithm = changedAlgorithm,
            )

        presenter.attach(view)
        presenter.argsRetrieved(
            ResourceFormMode.Create(
                leadingContentType = LeadingContentType.TOTP,
                parentFolderId = null,
            ),
            totp,
        )
        presenter.totpAdvancedSettingsChanged(changedAdvancedSettings)
        presenter.applyClick()

        argumentCaptor<TotpUiModel> {
            verify(view).goBackWithResult(capture())
            assertThat(firstValue.expiry).isEqualTo(changedExpiry)
            assertThat(firstValue.length).isEqualTo(changedLength)
            assertThat(firstValue.algorithm).isEqualTo(changedAlgorithm)
        }
    }

    @Test
    fun `scan totp should apply changes and update ui`() {
        val scannedTotp =
            OtpParseResult.OtpQr.TotpQr(
                label = "label",
                secret = "secret",
                issuer = "issuer",
                algorithm = OtpParseResult.OtpQr.Algorithm.SHA1,
                digits = 6,
                period = 30,
            )

        presenter.attach(view)
        presenter.argsRetrieved(
            ResourceFormMode.Create(
                leadingContentType = LeadingContentType.TOTP,
                parentFolderId = null,
            ),
            totp,
        )
        presenter.totpScanned(isManualCreationChosen = false, scannedTotp)
        presenter.applyClick()

        verify(view).showSecret(scannedTotp.secret)
        verify(view).showUrl(scannedTotp.issuer.orEmpty())
        argumentCaptor<TotpUiModel> {
            verify(view).goBackWithResult(capture())
            assertThat(firstValue.secret).isEqualTo(scannedTotp.secret)
            assertThat(firstValue.issuer).isEqualTo(scannedTotp.issuer)
            assertThat(firstValue.algorithm).isEqualTo(scannedTotp.algorithm.name)
            assertThat(firstValue.length).isEqualTo(scannedTotp.digits.toString())
            assertThat(firstValue.expiry).isEqualTo(scannedTotp.period.toString())
        }
    }

    private companion object {
        private const val MOCK_SECRET = "mock secret"
        private const val MOCK_ISSUER = "mock issuer"
        private const val MOCK_EXPIRY = "30"
        private const val MOCK_LENGTH = "6"
        private val MOCK_ALGORITHM = OtpParseResult.OtpQr.Algorithm.SHA1.name

        private val totp =
            TotpUiModel(
                secret = MOCK_SECRET,
                issuer = MOCK_ISSUER,
                expiry = MOCK_EXPIRY,
                length = MOCK_LENGTH,
                algorithm = MOCK_ALGORITHM,
            )
    }
}
