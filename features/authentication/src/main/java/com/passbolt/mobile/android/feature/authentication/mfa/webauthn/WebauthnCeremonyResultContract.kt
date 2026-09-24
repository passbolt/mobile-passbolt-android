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

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.BrowserUnavailable
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.Cancelled
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.Completed
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.Failed

internal class WebauthnCeremonyResultContract : ActivityResultContract<String, WebauthnCeremonyResult>() {
    override fun createIntent(
        context: Context,
        input: String,
    ): Intent = Intent(context, WebauthnCeremonyActivity::class.java).putExtra(EXTRA_CEREMONY_URL, input)

    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): WebauthnCeremonyResult =
        when (resultCode) {
            Activity.RESULT_OK -> {
                val exchangeCode = intent?.getStringExtra(EXTRA_EXCHANGE_CODE)
                if (exchangeCode.isNullOrBlank()) Failed(null) else Completed(exchangeCode)
            }
            RESULT_FAILED -> Failed(intent?.getStringExtra(EXTRA_ERROR))
            RESULT_BROWSER_UNAVAILABLE -> BrowserUnavailable
            else -> Cancelled
        }

    companion object {
        const val EXTRA_CEREMONY_URL = "CEREMONY_URL"
        private const val EXTRA_EXCHANGE_CODE = "EXCHANGE_CODE"
        private const val EXTRA_ERROR = "ERROR"
        private const val RESULT_FAILED = Activity.RESULT_FIRST_USER
        private const val RESULT_BROWSER_UNAVAILABLE = Activity.RESULT_FIRST_USER + 1

        fun resultCodeOf(result: WebauthnCeremonyResult): Int =
            when (result) {
                is Completed -> Activity.RESULT_OK
                is Failed -> RESULT_FAILED
                is BrowserUnavailable -> RESULT_BROWSER_UNAVAILABLE
                is Cancelled -> Activity.RESULT_CANCELED
            }

        fun resultIntentOf(result: WebauthnCeremonyResult): Intent =
            Intent().apply {
                when (result) {
                    is Completed -> putExtra(EXTRA_EXCHANGE_CODE, result.exchangeCode)
                    is Failed -> putExtra(EXTRA_ERROR, result.error)
                    is BrowserUnavailable, is Cancelled -> Unit
                }
            }
    }
}
