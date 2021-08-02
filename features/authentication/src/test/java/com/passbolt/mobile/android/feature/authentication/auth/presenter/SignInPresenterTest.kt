package com.passbolt.mobile.android.feature.authentication.auth.presenter

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.stub
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import com.passbolt.mobile.android.core.navigation.AuthenticationType
import com.passbolt.mobile.android.dto.response.ChallengeResponseDto
import com.passbolt.mobile.android.feature.authentication.auth.AuthContract
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeProvider
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeVerifier
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetServerPublicPgpKeyUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetServerPublicRsaKeyUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SiginInUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.CheckIfPassphraseFileExistsUseCase
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.qualifier.named
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject

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

class SignInPresenterTest : KoinTest {

    private val presenter: AuthContract.Presenter by inject(named(AuthenticationType.SignIn.javaClass.simpleName))
    private val mockView = mock<AuthContract.View>()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testAuthModule)
    }

    @Test
    fun `view should show error when server public keys cannot be fetched`() {
        mockGetServerPublicPgpKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(GetServerPublicPgpKeyUseCase.Output.Failure)
        }
        whenever(mockCheckIfPassphraseExistsUseCase.execute(anyOrNull()))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = false))

        presenter.argsRetrieved(ACCOUNT)
        presenter.attach(mockView)
        presenter.signInClick(SAMPLE_PASSPHRASE)

        verify(mockView).showTitle()
        verify(mockView).hideKeyboard()
        verify(mockView).showProgress()
        verify(mockView).hideProgress()
        verify(mockView).showGenericError()
        verifyNoMoreInteractions(mockView)
    }

    @Test
    fun `view should show auth success when sign in in succeeds`() {
        mockGetServerPublicPgpKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(GetServerPublicPgpKeyUseCase.Output.Success("publickKey"))
        }
        mockGetServerPublicRsaKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(GetServerPublicRsaKeyUseCase.Output.Success("publicRsa"))
        }
        mockChallengeProvider.stub {
            onBlocking { get(any(), any(), any(), any(), any()) }.doReturn(
                ChallengeProvider.Output.Success("challenge")
            )
        }
        mockSignInUseCase.stub {
            onBlocking { execute(any()) }
                .doReturn(SiginInUseCase.Output.Success("challenge"))
        }
        mockChallengeDecryptor.stub {
            onBlocking { decrypt(any(), any(), any(), any()) }.doReturn(
                ChallengeResponseDto("1", "domain", "token", "accessToken", "refreshToken")
            )
        }
        mockChallengeVerifier.stub {
            onBlocking { verify(any(), any()) }.doReturn(
                ChallengeVerifier.Output.Verified("accessToken", "refreshToken")
            )
        }
        whenever(mockCheckIfPassphraseExistsUseCase.execute(anyOrNull()))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = false))

        presenter.argsRetrieved(ACCOUNT)
        presenter.attach(mockView)
        presenter.signInClick(SAMPLE_PASSPHRASE)

        verify(mockView).showProgress()
        verify(mockView).hideProgress()
        verify(mockView).authSuccess()
    }

    @Test
    fun `view should show wrong passphrase when challenge provider cannot encrypt challenge`() {
        mockGetServerPublicPgpKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(GetServerPublicPgpKeyUseCase.Output.Success("publickKey"))
        }
        mockGetServerPublicRsaKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(GetServerPublicRsaKeyUseCase.Output.Success("publicRsa"))
        }
        mockChallengeProvider.stub {
            onBlocking { get(any(), any(), any(), any(), any()) }.doReturn(
                ChallengeProvider.Output.WrongPassphrase
            )
        }
        whenever(mockCheckIfPassphraseExistsUseCase.execute(anyOrNull()))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = false))

        presenter.argsRetrieved(ACCOUNT)
        presenter.attach(mockView)
        presenter.signInClick(SAMPLE_PASSPHRASE)

        verify(mockView).showProgress()
        verify(mockView).hideProgress()
        verify(mockView).showWrongPassphrase()
    }

    @Test
    fun `view should show server error message when sign in fails`() {
        mockGetServerPublicPgpKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(GetServerPublicPgpKeyUseCase.Output.Success("publickKey"))
        }
        mockGetServerPublicRsaKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(GetServerPublicRsaKeyUseCase.Output.Success("publicRsa"))
        }
        mockChallengeProvider.stub {
            onBlocking { get(any(), any(), any(), any(), any()) }.doReturn(
                ChallengeProvider.Output.Success("challenge")
            )
        }
        mockSignInUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(SiginInUseCase.Output.Failure(ERROR_MESSAGE))
        }
        whenever(mockCheckIfPassphraseExistsUseCase.execute(anyOrNull()))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = false))

        presenter.argsRetrieved(ACCOUNT)
        presenter.attach(mockView)
        presenter.signInClick(SAMPLE_PASSPHRASE)

        verify(mockView).showProgress()
        verify(mockView).hideProgress()
        verify(mockView).showError(ERROR_MESSAGE)
    }

    @Test
    fun `view should show generic error when challenge cannot be verified`() {
        mockGetServerPublicPgpKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(GetServerPublicPgpKeyUseCase.Output.Success("publickKey"))
        }
        mockGetServerPublicRsaKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(GetServerPublicRsaKeyUseCase.Output.Success("publicRsa"))
        }
        mockChallengeProvider.stub {
            onBlocking { get(any(), any(), any(), any(), any()) }.doReturn(
                ChallengeProvider.Output.Success("challenge")
            )
        }
        mockSignInUseCase.stub {
            onBlocking { execute(any()) }
                .doReturn(SiginInUseCase.Output.Success("challenge"))
        }
        mockChallengeDecryptor.stub {
            onBlocking { decrypt(any(), any(), any(), any()) }.doReturn(
                ChallengeResponseDto("1", "domain", "token", "accessToken", "refreshToken")
            )
        }
        mockChallengeVerifier.stub {
            onBlocking { verify(any(), any()) }.doReturn(
                ChallengeVerifier.Output.Failure
            )
        }
        whenever(mockCheckIfPassphraseExistsUseCase.execute(anyOrNull()))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = false))

        presenter.argsRetrieved(ACCOUNT)
        presenter.attach(mockView)
        presenter.signInClick(SAMPLE_PASSPHRASE)

        verify(mockView).showProgress()
        verify(mockView).hideProgress()
        verify(mockView).showGenericError()
    }


    private companion object {
        private const val ERROR_MESSAGE = "error"
        private val SAMPLE_PASSPHRASE = "pass".toByteArray()
    }
}
