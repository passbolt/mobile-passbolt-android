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

package com.passbolt.mobile.android.feature.authentication.session.base

import android.app.Activity
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.navigation.AppForegroundListener
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetSessionExpiryUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.RefreshSessionUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.test.KoinTestRule
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class AuthenticatedPresenterTest {
    private val coroutineLaunchContext = TestCoroutineLaunchContext()
    private val presenter = DummyAuthPresenter(coroutineLaunchContext)
    private val view = mock<DummyAuthContract.View>()

    private val mockPassphraseMemoryCache = mock<PassphraseMemoryCache>()
    private val mockGetSessionExpiryUseCase = mock<GetSessionExpiryUseCase>()
    private val mockRefreshSessionUseCase = mock<RefreshSessionUseCase>()
    private val appForegroundListener = mock<AppForegroundListener>()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mockGetSessionExpiryUseCase }
                    single { mockPassphraseMemoryCache }
                    single { mockRefreshSessionUseCase }
                    single { appForegroundListener }
                },
            )
        }

    @Test
    fun `ui sign-in authentication should be invoked when session is expired`() =
        runTest {
            mockGetSessionExpiryUseCase.stub {
                onBlocking { execute(Unit) }.thenReturn(
                    GetSessionExpiryUseCase.Output.JwtAlreadyExpired,
                )
            }
            mockRefreshSessionUseCase.stub {
                onBlocking { execute(Unit) }.thenReturn(
                    RefreshSessionUseCase.Output.Failure,
                )
            }
            whenever(appForegroundListener.isForeground()) doReturn true
            whenever(mockPassphraseMemoryCache.getSessionDurationSeconds()).thenReturn(null)

            presenter.attach(view)
            val job =
                launch {
                    presenter.authenticatedOperation()
                }
            delay(1)
            job.cancel()

            verify(view).showSignInAuth()
        }

    @Test
    fun `ui passphrase authentication should be invoked when session is expired`() =
        runTest {
            mockGetSessionExpiryUseCase.stub {
                onBlocking { execute(Unit) }.thenReturn(
                    GetSessionExpiryUseCase.Output.JwtWillExpire(
                        ZonedDateTime.now().plusSeconds(60),
                    ),
                )
            }
            whenever(appForegroundListener.isForeground()) doReturn true
            whenever(mockPassphraseMemoryCache.getSessionDurationSeconds()).thenReturn(null)

            presenter.attach(view)
            val job =
                launch {
                    presenter.authenticatedOperation()
                }
            delay(1)
            job.cancel()

            verify(view).showRefreshPassphraseAuth()
        }

    @Test
    fun `ui sign-in authentication not should be invoked when app is in background`() =
        runTest {
            mockGetSessionExpiryUseCase.stub {
                onBlocking { execute(Unit) }.thenReturn(
                    GetSessionExpiryUseCase.Output.JwtAlreadyExpired,
                )
            }
            mockRefreshSessionUseCase.stub {
                onBlocking { execute(Unit) }.thenReturn(
                    RefreshSessionUseCase.Output.Failure,
                )
            }
            val appForegroundFlow = MutableSharedFlow<Activity>()
            whenever(appForegroundListener.isForeground()) doReturn false
            whenever(appForegroundListener.appWentForegroundFlow) doReturn appForegroundFlow
            whenever(mockPassphraseMemoryCache.getSessionDurationSeconds()).thenReturn(null)

            presenter.attach(view)
            val job =
                launch {
                    presenter.authenticatedOperation()
                }
            delay(1)
            job.cancel()

            verify(view, never()).showSignInAuth()
        }

    @Test
    fun `ui passphrase authentication should not be invoked when app is in background`() =
        runTest {
            mockGetSessionExpiryUseCase.stub {
                onBlocking { execute(Unit) }.thenReturn(
                    GetSessionExpiryUseCase.Output.JwtWillExpire(
                        ZonedDateTime.now().plusSeconds(60),
                    ),
                )
            }
            val appForegroundFlow = MutableSharedFlow<Activity>()
            whenever(appForegroundListener.isForeground()) doReturn false
            whenever(appForegroundListener.appWentForegroundFlow) doReturn appForegroundFlow
            whenever(mockPassphraseMemoryCache.getSessionDurationSeconds()).thenReturn(null)

            presenter.attach(view)
            val job =
                launch {
                    presenter.authenticatedOperation()
                }
            delay(1)
            job.cancel()

            verify(view, never()).showRefreshPassphraseAuth()
        }
}
