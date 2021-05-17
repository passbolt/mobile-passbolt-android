package com.passbolt.mobile.android.feature.setup.scanqr.qrparser

import com.passbolt.mobile.android.common.extension.eraseArray
import com.passbolt.mobile.android.common.extension.findPosition
import com.passbolt.mobile.android.common.extension.toCharArray
import okio.Buffer

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
class KeyAssembler {

    fun assemblePrivateKey(contentBytes: Buffer): CharArray {
        val charArray = contentBytes.readByteArray().toCharArray()
        val keyStartPosition =
            charArray.findPosition(ARMORED_KEY_TEXT.toCharArray()) + ARMORED_KEY_TEXT.length
        val keyEndPosition = charArray.lastIndexOf(ARMORED_KEY_END_CHAR) - 1

        val privateKey = charArray.slice(IntRange(keyStartPosition, keyEndPosition)).toCharArray()
        charArray.eraseArray()

        return privateKey
    }

    private companion object {
        private const val ARMORED_KEY_TEXT = "\"armored_key\":\""
        private const val ARMORED_KEY_END_CHAR = '}'
    }
}
