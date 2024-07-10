package com.passbolt.mobile.android.core.passwordgenerator.usecase

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.core.passwordgenerator.mockPwnedPasswordRepository
import com.passbolt.mobile.android.core.passwordgenerator.passwordGeneratorTestModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.stub

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
class CheckPasswordPropertiesUseCaseTest : KoinTest {
    @ExperimentalCoroutinesApi
    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(passwordGeneratorTestModule)
    }

    private val checkPasswordPropertiesUseCase: CheckPasswordPropertiesUseCase by inject()

    @Test
    fun `pwned password is evaluated correct`() = runTest {
        val password = "test1234567890"
        mockPwnedPasswordRepository.stub {
            onBlocking { getPwnedPasswordsSuffixes(any()) }.thenReturn(
                NetworkResult.Success(
                    "08f70a062457f0763adc66e0c0fe17a150a:10"
                )
            )
        }

        val result = checkPasswordPropertiesUseCase.execute(CheckPasswordPropertiesUseCase.Input(password))

        assertThat(result).isInstanceOf(CheckPasswordPropertiesUseCase.Output.Pwned::class.java)
        assertThat((result as CheckPasswordPropertiesUseCase.Output.Pwned).dataBreachesCount).isEqualTo(10)
    }

    @Test
    fun `not pwned password is evaluated correct`() = runTest {
        val password = "test1234567890"
        mockPwnedPasswordRepository.stub {
            onBlocking { getPwnedPasswordsSuffixes(any()) }.thenReturn(
                NetworkResult.Success(
                    "08f70a062457f0763adc66e0c0fe17a150b:10\n" +
                            "08f70a062457f0763adc66e0c0fe17a150c:10\n" +
                            "08f70a062457f0763adc66e0c0fe17a150d:10"
                )
            )
        }

        val result = checkPasswordPropertiesUseCase.execute(CheckPasswordPropertiesUseCase.Input(password))

        assertThat(result).isInstanceOf(CheckPasswordPropertiesUseCase.Output.Fine::class.java)
    }

    @Test
    fun `weak password is evaluated correct`() = runTest {
        val password = "test"

        val result = checkPasswordPropertiesUseCase.execute(CheckPasswordPropertiesUseCase.Input(password))

        assertThat(result).isInstanceOf(CheckPasswordPropertiesUseCase.Output.Weak::class.java)
    }
}
