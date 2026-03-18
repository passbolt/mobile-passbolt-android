package com.passbolt.mobile.android.feature.authentication.mfa.totp.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
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

private const val DEFAULT_MAX_LENGTH = 6

@Composable
fun rememberPinInputState(
    maxLength: Int = DEFAULT_MAX_LENGTH,
    initialValue: String = "",
    sanitizer: PinInputSanitizer =
        koinInject(
            parameters = {
                parametersOf(maxLength)
            },
        ),
    onPinComplete: ((String) -> Unit)? = null,
): PinInputState =
    rememberSaveable(
        saver = PinInputState.saver(maxLength, sanitizer, onPinComplete),
    ) {
        PinInputState(initialValue, maxLength, sanitizer, onPinComplete)
    }

@Stable
class PinInputState internal constructor(
    initialValue: String = "",
    val maxLength: Int = DEFAULT_MAX_LENGTH,
    private val sanitizer: PinInputSanitizer,
    private val onPinComplete: ((String) -> Unit)? = null,
) {
    var textFieldValue by mutableStateOf(
        createTextFieldValue(sanitizer.sanitize(initialValue)),
    )
        private set

    val text: String
        get() = textFieldValue.text

    fun onValueChange(newValue: TextFieldValue) {
        val sanitizedText = sanitizer.sanitize(newValue.text)
        textFieldValue = createTextFieldValue(sanitizedText)
        notifyIfComplete(sanitizedText)
    }

    fun setText(text: String?) {
        val sanitizedText = sanitizer.sanitize(text.orEmpty())
        textFieldValue = createTextFieldValue(sanitizedText)
        notifyIfComplete(sanitizedText)
    }

    private fun createTextFieldValue(text: String) =
        TextFieldValue(
            text = text,
            selection = TextRange(text.length),
        )

    private fun notifyIfComplete(text: String) {
        if (text.length == maxLength) {
            onPinComplete?.invoke(text)
        }
    }

    companion object {
        fun saver(
            maxLength: Int,
            sanitizer: PinInputSanitizer,
            onPinComplete: ((String) -> Unit)?,
        ): Saver<PinInputState, String> =
            Saver(
                save = { it.text },
                restore =
                    {
                        PinInputState(
                            initialValue = it,
                            maxLength = maxLength,
                            sanitizer = sanitizer,
                            onPinComplete = onPinComplete,
                        )
                    },
            )
    }
}
