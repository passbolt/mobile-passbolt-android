package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.passwordgenerator.codepoints.toCodepoints
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.PasswordStrength
import com.passbolt.mobile.android.ui.PasswordUiModel
import com.passbolt.mobile.android.ui.ResourceFormMode
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
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

class PasswordFormPresenterTest : KoinTest {
    private val presenter: PasswordFormContract.Presenter by inject()
    private val view: PasswordFormContract.View = mock()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testPasswordFormModule)
        }

    @Test
    fun `view should show correct create title and totp on attach`() {
        mockEntropyCalculator.stub {
            onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
        }

        presenter.attach(view)
        presenter.argsRetrieved(
            ResourceFormMode.Create(
                leadingContentType = LeadingContentType.PASSWORD,
                parentFolderId = null,
            ),
            password,
        )

        verify(view).showCreateTitle()
        verify(view).showPassword(password.password.toCodepoints(), 0.0, PasswordStrength.Empty)
        verify(view).showPasswordUsername(password.username)
        verify(view).showPasswordMainUri(password.mainUri)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `password change should trigger entropy recalculation`() {
        mockEntropyCalculator.stub {
            onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
        }

        presenter.attach(view)
        presenter.argsRetrieved(
            ResourceFormMode.Create(
                leadingContentType = LeadingContentType.PASSWORD,
                parentFolderId = null,
            ),
            password,
        )
        presenter.passwordTextChanged("t")
        presenter.passwordTextChanged("te")
        presenter.passwordTextChanged("tes")
        presenter.passwordTextChanged("test")

        verify(view, times(4)).showPasswordStrength(any(), any())
    }

    @Test
    fun `password changes should be applied`() {
        val changedPassword = "changed password"
        val changedMainUri = "changed main uri"
        val changedMainUsername = "changed main username"

        presenter.attach(view)
        presenter.argsRetrieved(
            ResourceFormMode.Create(
                leadingContentType = LeadingContentType.PASSWORD,
                parentFolderId = null,
            ),
            password,
        )
        presenter.passwordTextChanged(changedPassword)
        presenter.passwordUsernameTextChanged(changedMainUsername)
        presenter.passwordMainUriTextChanged(changedMainUri)
        presenter.applyClick()

        argumentCaptor<PasswordUiModel> {
            verify(view).goBackWithResult(capture())
            assertThat(firstValue.password).isEqualTo(changedPassword)
            assertThat(firstValue.mainUri).isEqualTo(changedMainUri)
            assertThat(firstValue.username).isEqualTo(changedMainUsername)
        }
    }

    private companion object {
        private const val MOCK_PASSWORD = "mock password"
        private const val MOCK_MAIN_URI = "mock main uri"
        private const val MOCK_USERNAME = "mock username"

        private val password =
            PasswordUiModel(
                password = MOCK_PASSWORD,
                mainUri = MOCK_MAIN_URI,
                username = MOCK_USERNAME,
            )
    }
}
