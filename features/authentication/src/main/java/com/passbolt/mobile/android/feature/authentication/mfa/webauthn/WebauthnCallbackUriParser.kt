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

package com.passbolt.mobile.android.feature.authentication.mfa.webauthn

import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.Cancelled
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.Completed
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.Failed
import java.net.URI
import java.net.URLDecoder

private const val CALLBACK_SCHEME = "passbolt"
private const val CALLBACK_HOST = "mfa"
private const val CALLBACK_PATH = "/webauthn/callback"
private const val QUERY_EXCHANGE_CODE = "exchange_code"
private const val QUERY_ERROR = "error"
private const val ERROR_USER_CANCELLED = "user_cancelled"
private const val ERROR_INVALID_CALLBACK_URI = "invalid_callback_uri"

internal fun parseWebauthnCallbackUri(uri: String?): WebauthnCeremonyResult =
    runCatching { parseValidCallbackUri(URI(requireNotNull(uri))) }
        .getOrElse { Failed(ERROR_INVALID_CALLBACK_URI) }

private fun parseValidCallbackUri(uri: URI): WebauthnCeremonyResult {
    require(uri.scheme == CALLBACK_SCHEME && uri.host == CALLBACK_HOST && uri.path == CALLBACK_PATH)
    val params = parseQuery(uri.rawQuery.orEmpty())
    val exchangeCode = params[QUERY_EXCHANGE_CODE]
    val error = params[QUERY_ERROR]

    return if (!exchangeCode.isNullOrBlank() && error == null) {
        Completed(exchangeCode)
    } else if (error != null) {
        if (error == ERROR_USER_CANCELLED) Cancelled else Failed(error)
    } else {
        Failed(null)
    }
}

private fun parseQuery(rawQuery: String): Map<String, String> {
    val pairs =
        rawQuery
            .split('&')
            .filter { it.isNotEmpty() }
            .map { pair ->
                val key = URLDecoder.decode(pair.substringBefore('='), Charsets.UTF_8.name())
                val value = URLDecoder.decode(pair.substringAfter('=', ""), Charsets.UTF_8.name())
                key to value
            }
    require(pairs.map { it.first }.toSet().size == pairs.size)
    return pairs.toMap()
}
