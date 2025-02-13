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

package com.passbolt.mobile.android.feature.authentication.session

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Passphrase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Session
import com.passbolt.mobile.android.core.mvp.authentication.UnauthenticatedReason
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetSessionExpiryUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.RefreshSessionUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.test.KoinTestRule
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
class AuthOperationRunnerTest {
    private var sessionRefreshFlow = MutableStateFlow<Unit?>(null)
    private var needSessionRefreshFlow = MutableStateFlow<UnauthenticatedReason?>(null)

    private val mockPassphraseMemoryCache = mock<PassphraseMemoryCache>()
    private val mockGetSessionExpiryUseCase = mock<GetSessionExpiryUseCase>()
    private val mockRefreshSessionUseCase = mock<RefreshSessionUseCase>()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(
            module {
                single { mockGetSessionExpiryUseCase }
                single { mockPassphraseMemoryCache }
                single { mockRefreshSessionUseCase }
            }
        )
    }

    @Test
    fun `operation should complete successfully when both sessions are valid`() = runTest {
        val sampleAuthenticatedOperation = object : () -> AuthenticatedUseCaseOutput {
            override fun invoke(): AuthenticatedUseCaseOutput {
                return object : AuthenticatedUseCaseOutput {
                    override val authenticationState = AuthenticationState.Authenticated
                }
            }
        }
        val operationSpy = spy(sampleAuthenticatedOperation)
        mockGetSessionExpiryUseCase.stub {
            onBlocking { execute(Unit) }.thenReturn(
                GetSessionExpiryUseCase.Output.JwtWillExpire(
                    ZonedDateTime.now().plusSeconds(60L)
                )
            )
        }
        whenever(mockPassphraseMemoryCache.getSessionDurationSeconds()).thenReturn(60L)

        runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshFlow, operationSpy)

        verify(operationSpy).invoke()
    }

    @Test
    fun `operation should trigger session refresh before operation when backend session expired`() = runTest {
        val sampleAuthenticatedOperation = object : () -> AuthenticatedUseCaseOutput {
            override fun invoke(): AuthenticatedUseCaseOutput {
                return object : AuthenticatedUseCaseOutput {
                    override val authenticationState = AuthenticationState.Authenticated
                }
            }
        }
        val operationSpy = spy(sampleAuthenticatedOperation)
        mockGetSessionExpiryUseCase.stub {
            onBlocking { execute(Unit) }.thenReturn(
                GetSessionExpiryUseCase.Output.JwtAlreadyExpired
            )
        }
        mockRefreshSessionUseCase.stub {
            onBlocking { execute(Unit) }.thenReturn(
                RefreshSessionUseCase.Output.Success
            )
        }
        whenever(mockPassphraseMemoryCache.getSessionDurationSeconds()).thenReturn(60L)

        runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshFlow, operationSpy)

        verify(mockRefreshSessionUseCase).execute(Unit)
        verify(operationSpy).invoke()
    }

    @Test
    fun `operation should trigger session refresh before operation when backend session is about to expire`() =
        runTest {
            val sampleAuthenticatedOperation = object : () -> AuthenticatedUseCaseOutput {
                override fun invoke(): AuthenticatedUseCaseOutput {
                    return object : AuthenticatedUseCaseOutput {
                        override val authenticationState = AuthenticationState.Authenticated
                    }
                }
            }
            val operationSpy = spy(sampleAuthenticatedOperation)
            mockGetSessionExpiryUseCase.stub {
                onBlocking { execute(Unit) }.thenReturn(
                    GetSessionExpiryUseCase.Output.JwtWillExpire(
                        ZonedDateTime.now().plusSeconds(5L)
                    )
                )
            }
            mockRefreshSessionUseCase.stub {
                onBlocking { execute(Unit) }.thenReturn(
                    RefreshSessionUseCase.Output.Success
                )
            }
            whenever(mockPassphraseMemoryCache.getSessionDurationSeconds()).thenReturn(60L)

            runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshFlow, operationSpy)

            verify(mockRefreshSessionUseCase).execute(Unit)
            verify(operationSpy).invoke()
        }

    @Test
    fun `operation should trigger passphrase session refresh before operation when expired`() = runTest {
        val sampleAuthenticatedOperation = object : () -> AuthenticatedUseCaseOutput {
            override fun invoke(): AuthenticatedUseCaseOutput {
                return object : AuthenticatedUseCaseOutput {
                    override val authenticationState = AuthenticationState.Authenticated
                }
            }
        }
        val operationSpy = spy(sampleAuthenticatedOperation)
        mockGetSessionExpiryUseCase.stub {
            onBlocking { execute(Unit) }.thenReturn(
                GetSessionExpiryUseCase.Output.JwtWillExpire(
                    ZonedDateTime.now().plusSeconds(60L)
                )
            )
        }
        whenever(mockPassphraseMemoryCache.getSessionDurationSeconds()).thenReturn(null)

        val operationJob = launch {
            runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshFlow, operationSpy)
        }

        needSessionRefreshFlow
            .drop(1)
            .take(1)
            .test {
                assertThat(expectItem()).isEqualTo(Passphrase)
                expectComplete()
            }
        operationJob.cancel()

        verify(operationSpy, never()).invoke()
    }

    @Test
    fun `operation should trigger full sign in if session refresh fails`() = runTest {
        val sampleAuthenticatedOperation = object : () -> AuthenticatedUseCaseOutput {
            override fun invoke(): AuthenticatedUseCaseOutput {
                return object : AuthenticatedUseCaseOutput {
                    override val authenticationState = AuthenticationState.Authenticated
                }
            }
        }
        val operationSpy = spy(sampleAuthenticatedOperation)
        mockGetSessionExpiryUseCase.stub {
            onBlocking { execute(Unit) }.thenReturn(
                GetSessionExpiryUseCase.Output.JwtAlreadyExpired
            )
        }
        mockRefreshSessionUseCase.stub {
            onBlocking { execute(Unit) }.thenReturn(
                RefreshSessionUseCase.Output.Failure
            )
        }
        whenever(mockPassphraseMemoryCache.getSessionDurationSeconds()).thenReturn(60L)

        val operationJob = launch {
            runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshFlow, operationSpy)
        }

        needSessionRefreshFlow
            .drop(1)
            .take(1)
            .test {
                assertThat(expectItem()).isEqualTo(Session)
                expectComplete()
            }
        operationJob.cancel()

        verify(operationSpy, never()).invoke()
    }

    @Test
    fun `operation should trigger full sign in if session refresh failsa`() = runTest {
        val sampleAuthenticatedOperation = object : () -> AuthenticatedUseCaseOutput {
            override fun invoke(): AuthenticatedUseCaseOutput {
                return object : AuthenticatedUseCaseOutput {
                    override val authenticationState = AuthenticationState.Authenticated
                }
            }
        }
        val operationSpy = spy(sampleAuthenticatedOperation)
        mockGetSessionExpiryUseCase.stub {
            onBlocking { execute(Unit) }.thenReturn(
                GetSessionExpiryUseCase.Output.JwtAlreadyExpired
            )
        }
        mockRefreshSessionUseCase.stub {
            onBlocking { execute(Unit) }.thenReturn(
                RefreshSessionUseCase.Output.Failure
            )
        }
        whenever(mockPassphraseMemoryCache.getSessionDurationSeconds()).thenReturn(60L)

        val operationJob = launch {
            runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshFlow, operationSpy)
        }
        delay(100)
        sessionRefreshFlow.tryEmit(Unit)
        operationJob.join()

        verify(operationSpy).invoke()
    }
}

