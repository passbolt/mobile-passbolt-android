package com.passbolt.mobile.android.core.ui.formatter

import org.jetbrains.annotations.VisibleForTesting

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
class FingerprintFormatter {

    fun format(fingerprint: String, appendMiddleSpacing: Boolean): String? {
        if (fingerprint.length != FINGERPRINT_LENGTH) {
            return null
        }

        val parsedString = buildString {
            append(fingerprint.substring(0, fingerprint.length / 2).chunked(FINGERPRINT_BLOCK_LENGTH).joinToString(" "))
            appendLine()
            if (appendMiddleSpacing) appendLine()
            append(fingerprint.substring(fingerprint.length / 2).chunked(FINGERPRINT_BLOCK_LENGTH).joinToString(" "))
        }

        return parsedString
    }

    fun formatWithRawFallback(fingerprint: String, appendMiddleSpacing: Boolean) =
        format(fingerprint, appendMiddleSpacing) ?: fingerprint

    companion object {
        @VisibleForTesting
        const val FINGERPRINT_LENGTH = 40

        private const val FINGERPRINT_BLOCK_LENGTH = 4
    }
}
