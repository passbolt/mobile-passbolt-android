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

package com.passbolt.mobile.android.domain.mfa.usecase

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.common.locale.LocaleProvider
import com.passbolt.mobile.android.domain.accounts.usecase.GetCurrentApiUrlUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Locale

class GetPasskeyCeremonyUrlUseCaseTest {
    private val getCurrentApiUrlUseCase = mock<GetCurrentApiUrlUseCase>()
    private val localeProvider = mock<LocaleProvider>()

    private val useCase = GetPasskeyCeremonyUrlUseCase(getCurrentApiUrlUseCase, localeProvider)

    @Before
    fun setUp() {
        whenever(getCurrentApiUrlUseCase.execute(Unit)) doReturn GetCurrentApiUrlUseCase.Output(BASE_URL)
        whenever(localeProvider.getCurrentLocale()) doReturn Locale.forLanguageTag("zz")
    }

    @Test
    fun `should build delegated ceremony url with state query`() {
        val output = useCase.execute(GetPasskeyCeremonyUrlUseCase.Input(STATE))

        assertThat(output.url).isEqualTo("$PAGE?mfa_delegated_state=$STATE")
    }

    @Test
    fun `should not double slash when api url has trailing slash`() {
        whenever(getCurrentApiUrlUseCase.execute(Unit)) doReturn GetCurrentApiUrlUseCase.Output("$BASE_URL/")

        val output = useCase.execute(GetPasskeyCeremonyUrlUseCase.Input(STATE))

        assertThat(output.url).isEqualTo("$PAGE?mfa_delegated_state=$STATE")
    }

    @Test
    fun `should url encode the state`() {
        val output = useCase.execute(GetPasskeyCeremonyUrlUseCase.Input("a b&c"))

        assertThat(output.url).isEqualTo("$PAGE?mfa_delegated_state=a+b%26c")
    }

    @Test
    fun `should append the server locale for the device language`() {
        whenever(localeProvider.getCurrentLocale()) doReturn Locale.forLanguageTag("pl-PL")

        val output = useCase.execute(GetPasskeyCeremonyUrlUseCase.Input(STATE))

        assertThat(output.url).isEqualTo("$PAGE?mfa_delegated_state=$STATE&locale=pl-PL")
    }

    @Test
    fun `should map any english region to the server english locale`() {
        whenever(localeProvider.getCurrentLocale()) doReturn Locale.forLanguageTag("en-US")

        val output = useCase.execute(GetPasskeyCeremonyUrlUseCase.Input(STATE))

        assertThat(output.url).isEqualTo("$PAGE?mfa_delegated_state=$STATE&locale=en-UK")
    }

    @Test
    fun `should map any portuguese region to the server portuguese locale`() {
        whenever(localeProvider.getCurrentLocale()) doReturn Locale.forLanguageTag("pt-PT")

        val output = useCase.execute(GetPasskeyCeremonyUrlUseCase.Input(STATE))

        assertThat(output.url).isEqualTo("$PAGE?mfa_delegated_state=$STATE&locale=pt-BR")
    }

    @Test
    fun `should omit the locale when the device language is not supported by the server`() {
        whenever(localeProvider.getCurrentLocale()) doReturn Locale.forLanguageTag("zh-CN")

        val output = useCase.execute(GetPasskeyCeremonyUrlUseCase.Input(STATE))

        assertThat(output.url).isEqualTo("$PAGE?mfa_delegated_state=$STATE")
    }

    private companion object {
        const val BASE_URL = "https://passbolt.example.com"
        const val PAGE = "$BASE_URL/mfa/verify/webauthn/delegated"
        const val STATE = "delegated-state-123"
    }
}
