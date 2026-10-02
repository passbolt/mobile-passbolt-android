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

import com.passbolt.mobile.android.common.locale.LocaleProvider
import com.passbolt.mobile.android.common.usecase.UseCase
import com.passbolt.mobile.android.domain.accounts.usecase.GetCurrentApiUrlUseCase
import java.net.URLEncoder

class GetPasskeyCeremonyUrlUseCase(
    private val getCurrentApiUrlUseCase: GetCurrentApiUrlUseCase,
    private val localeProvider: LocaleProvider,
) : UseCase<GetPasskeyCeremonyUrlUseCase.Input, GetPasskeyCeremonyUrlUseCase.Output> {
    override fun execute(input: Input): Output {
        val baseUrl = getCurrentApiUrlUseCase.execute(Unit).currentUrl.trimEnd('/')
        val query =
            buildList {
                add("$STATE_QUERY=${encode(input.state)}")
                serverLocale()?.let { add("$LOCALE_QUERY=${encode(it)}") }
            }.joinToString("&")
        return Output("$baseUrl/$CEREMONY_PATH?$query")
    }

    private fun serverLocale(): String? = SERVER_LOCALES_BY_LANGUAGE[localeProvider.getCurrentLocale().language]

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    data class Input(
        val state: String,
    )

    data class Output(
        val url: String,
    )

    private companion object {
        private const val CEREMONY_PATH = "mfa/verify/webauthn/delegated"
        private const val STATE_QUERY = "mfa_delegated_state"
        private const val LOCALE_QUERY = "locale"
        private val SERVER_LOCALES_BY_LANGUAGE =
            mapOf(
                "cs" to "cs-CZ",
                "de" to "de-DE",
                "en" to "en-UK",
                "es" to "es-ES",
                "fr" to "fr-FR",
                "it" to "it-IT",
                "ja" to "ja-JP",
                "ko" to "ko-KR",
                "lt" to "lt-LT",
                "nl" to "nl-NL",
                "pl" to "pl-PL",
                "pt" to "pt-BR",
                "ro" to "ro-RO",
                "ru" to "ru-RU",
                "sl" to "sl-SI",
                "sv" to "sv-SE",
                "uk" to "uk-UA",
            )
    }
}
