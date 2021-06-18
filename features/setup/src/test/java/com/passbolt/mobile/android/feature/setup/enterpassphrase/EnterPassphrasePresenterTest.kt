package com.passbolt.mobile.android.feature.setup.enterpassphrase

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.passbolt.mobile.android.feature.setup.base.testModule
import com.passbolt.mobile.android.storage.usecase.GetAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.GetSelectedUserPrivateKeyUseCase
import com.passbolt.mobile.android.storage.usecase.GetSelectedAccountUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import kotlin.time.ExperimentalTime

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
@ExperimentalTime
@ExperimentalCoroutinesApi
class EnterPassphrasePresenterTest : KoinTest {

    private val presenter: EnterPassphraseContract.Presenter by inject()
    private var view: EnterPassphraseContract.View = mock()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testModule, enterPassphraseModule)
    }

    @Before
    fun setUp() {
        whenever(getSelectedAccountUseCase.execute(Unit)).thenReturn(GetSelectedAccountUseCase.Output("1"))
        whenever(getAccountDataUseCase.execute(anyOrNull())).thenReturn(
            GetAccountDataUseCase.Output(
                firstName = "John",
                lastName = "Done",
                url = "https://example.com",
                email = "mail@example.com",
                avatarUrl = "https://example.com/avatar.png"
            )
        )
        presenter.attach(view)
    }

    @Test
    fun `entered password should enable sign in button`() {
        presenter.passwordChanged(false)
        view.setButtonEnabled(true)
    }

    @Test
    fun `empty password should disable sign in button`() {
        presenter.passwordChanged(true)
        view.setButtonEnabled(false)
    }

    @Test
    fun `clicking sign in when no biometrics hardware on device should skip this step`() = runBlockingTest {
        whenever(fingerprintInformationProvider.hasBiometricHardware()).thenReturn(false)
        presenter.singInClick("password".toCharArray())
        // TODO
    }

    @Test
    fun `clicking sign in when biometrics hardware on device should open biometrics setup`() = runBlockingTest {
        whenever(fingerprintInformationProvider.hasBiometricHardware()).thenReturn(true)
        whenever(getPrivateKeyUseCase.execute(Unit)).thenReturn(GetSelectedUserPrivateKeyUseCase.Output(""))
        whenever(verifyPassphraseUseCase.execute(anyOrNull())).thenReturn(VerifyPassphraseUseCase.Output(true))
        presenter.singInClick("password".toCharArray())
        verify(view).navigateToBiometricSetup()
    }

    @Test
    fun `clicking sign in when password is incorrect should show error`() = runBlockingTest {
        whenever(fingerprintInformationProvider.hasBiometricHardware()).thenReturn(true)
        whenever(getPrivateKeyUseCase.execute(Unit)).thenReturn(GetSelectedUserPrivateKeyUseCase.Output(""))
        whenever(verifyPassphraseUseCase.execute(anyOrNull())).thenReturn(VerifyPassphraseUseCase.Output(false))
        presenter.singInClick("password".toCharArray())
        verify(view).showWrongPassphraseError()
    }

    @Test
    fun `loaded avatar should be saved`() {
        val byteArray = ByteArray(1)
        presenter.onImageLoaded(byteArray)
        verify(saveUserAvatarUseCase).execute(anyOrNull())
    }

    @Test
    fun `forgot password button click should open forgot password dialog`() {
        presenter.forgotPasswordClick()
        verify(view).showForgotPasswordDialog()
    }

    @Test
    fun `entering screen should display user first and last name`() {
        verify(view).displayName("John Done")
    }

    @Test
    fun `entering screen should display user email`() {
        verify(view).displayEmail("mail@example.com")
    }

    @Test
    fun `entering screen should display user url`() {
        verify(view).displayUrl("https://example.com")
    }

    @Test
    fun `entering screen should display user avatar when it is available`() {
        verify(view).displayAvatar("https://example.com/avatar.png")
    }
}

