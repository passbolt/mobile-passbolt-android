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

package com.passbolt.mobile.android.feature.settings.accounts.keyinspector

import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.core.users.user.FetchCurrentUserUseCase
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorContract
import com.passbolt.mobile.android.storage.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.ui.GpgKeyModel
import com.passbolt.mobile.android.ui.UserModel
import com.passbolt.mobile.android.ui.UserProfileModel
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.net.UnknownHostException
import java.time.ZonedDateTime


class KeyInspectorPresenterTest : KoinTest {

    private val presenter: KeyInspectorContract.Presenter by inject()
    private val view = mock<KeyInspectorContract.View>()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testKeyInspectorModule)
    }

    @Before
    fun setup() {
        whenever(mockGetSelectedAccountDataUseCase.execute(Unit)).thenReturn(accountData)
        mockFetchCurrentUserUseCase.stub {
            onBlocking { execute(Unit) }.thenReturn(userData)
        }
    }

    @Test
    fun `view should show error when fetching key data failed`() {
        val errorMessage = "errorMessage"
        mockFetchCurrentUserUseCase.stub {
            onBlocking { execute(Unit) }.thenReturn(
                FetchCurrentUserUseCase.Output.Failure(
                    NetworkResult.Failure.NetworkError(
                        UnknownHostException(), errorMessage
                    ),
                    errorMessage
                )
            )
        }

        presenter.attach(view)

        verify(view).showError(errorMessage)
    }

    @Test
    fun `view should show key and account data information`() {
        val mockFingerprint = "AAAA"
        whenever(mockFingerprintFormatter.format(any(), any())).thenReturn(mockFingerprint)
        val mockDate = "DateString"
        whenever((mockDateFormatter.format(any()))).doReturn(mockDate)

        presenter.attach(view)

        verify(view).showProgress()
        verify(view).showAvatar(accountData.avatarUrl.toString())
        verify(view).showLabel(accountData.label.toString())
        verify(view).showFingerprint(mockFingerprint)
        verify(view).showLength(userData.userModel.gpgKey.bits.toString())
        verify(view).showUid(userData.userModel.gpgKey.uid.toString())
        verify(view).showExpirationDate(mockDate)
        verify(view).showCreationDate(mockDate)
        verify(view).showAlgorithm(userData.userModel.gpgKey.type.toString())
        verify(view).hideProgress()
        verifyNoMoreInteractions(view)
    }

    private companion object {
        private val now = ZonedDateTime.now()
        private val accountData = GetSelectedAccountDataUseCase.Output(
            firstName = "first",
            lastName = "last",
            email = "email",
            avatarUrl = "avatar_url",
            url = "url",
            serverId = "serverId",
            label = "label",
            role = "user"
        )
        private val userData = FetchCurrentUserUseCase.Output.Success(
            UserModel(
                id = "newUserId",
                userName = "newUserName",
                gpgKey = GpgKeyModel(
                    armoredKey = "keyData",
                    fingerprint = "fingerprint",
                    bits = 1,
                    uid = "uid",
                    keyId = "keyid",
                    type = "rsa",
                    keyExpirationDate = now,
                    keyCreationDate = now
                ),
                profile = UserProfileModel(
                    username = "username",
                    firstName = "first",
                    lastName = "last",
                    avatarUrl = "avatar_url"
                )
            )
        )
    }
}
