package com.passbolt.mobile.android.storage.repository.passphrase

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.passbolt.mobile.android.storage.cache.passphrase.PotentialPassphrase
import com.passbolt.mobile.android.storage.usecase.passphrase.GetPassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
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
class PassphraseRepositoryTest : KoinTest {

    private val passphraseRepository: PassphraseRepository by inject()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(passphraseRepositoryTestModule)
    }

    @Test
    fun `test if repository uses cached value first`() {
        whenever(passphraseMemoryCacheMock.hasPassphrase()).doReturn(true)
        whenever(passphraseMemoryCacheMock.get()).doReturn(PotentialPassphrase.Passphrase(PASSPHRASE))
        whenever(getSelectedAccountUseCaseMock.execute(Unit)).doReturn(
            GetSelectedAccountUseCase.Output(ACCOUNT_ID)
        )

        val passphrase = passphraseRepository.getPotentialPassphrase()
        assertThat(passphrase).isInstanceOf(PotentialPassphrase.Passphrase::class.java)
        assertThat((passphrase as PotentialPassphrase.Passphrase).passphrase).isEqualTo(PASSPHRASE)
    }

    @Test
    fun `test if repository uses stored value when no cached version available`() {
        whenever(passphraseMemoryCacheMock.hasPassphrase()).doReturn(false)
        whenever(getSelectedAccountUseCaseMock.execute(Unit)).doReturn(
            GetSelectedAccountUseCase.Output(ACCOUNT_ID)
        )
        whenever(getPassphraseUseCaseMock.execute(any())).doReturn(
            GetPassphraseUseCase.Output(PotentialPassphrase.Passphrase(PASSPHRASE))
        )

        val passphrase = passphraseRepository.getPotentialPassphrase()
        assertThat(passphrase).isInstanceOf(PotentialPassphrase.Passphrase::class.java)
        assertThat((passphrase as PotentialPassphrase.Passphrase).passphrase).isEqualTo(PASSPHRASE)
    }

    private companion object {
        private val PASSPHRASE = "passphrase".toCharArray()
        private const val ACCOUNT_ID = "accountId"
    }
}
