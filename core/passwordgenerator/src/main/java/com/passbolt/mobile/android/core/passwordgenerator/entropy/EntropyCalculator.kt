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

package com.passbolt.mobile.android.core.passwordgenerator.entropy

import com.passbolt.mobile.android.core.passwordgenerator.Alphabets
import com.passbolt.mobile.android.core.passwordgenerator.codepoints.Codepoint
import com.passbolt.mobile.android.core.passwordgenerator.codepoints.CodepointSet
import com.passbolt.mobile.android.core.passwordgenerator.codepoints.toCodepoints
import kotlin.math.ln

class EntropyCalculator {
    fun getEntropy(
        password: List<Codepoint>,
        alphabets: Set<CodepointSet>
    ): Double {
        if (password.isEmpty() || alphabets.isEmpty()) {
            return 0.0
        }
        val usedKnownAlphabet = mutableSetOf<Codepoint>()
        val usedUnknownAlphabet = mutableSetOf<Codepoint>()

        password.forEach { codepoint ->
            val alphabet = alphabets.find { it.codepoints.contains(codepoint) }?.codepoints ?: emptyList()
            if (alphabet.isNotEmpty()) {
                usedKnownAlphabet.addAll(alphabet)
            } else {
                usedUnknownAlphabet.add(codepoint)
            }
        }

        return password.size.toDouble() * (
                ln(usedKnownAlphabet.size.toDouble() + usedUnknownAlphabet.size.toDouble()) /
                        ln(2.0)
                )
    }

    fun getEntropy(password: String): Double {
        return getEntropy(password.toCodepoints(), Alphabets.all.values.toSet())
    }
}
