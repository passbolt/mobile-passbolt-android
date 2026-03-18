package com.passbolt.mobile.android.feature.authentication.mfa.totp

import PassboltTheme
import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import com.passbolt.mobile.android.feature.authentication.mfa.totp.compose.PinInput
import com.passbolt.mobile.android.feature.authentication.mfa.totp.compose.PinInputState
import com.passbolt.mobile.android.feature.authentication.mfa.totp.compose.rememberPinInputState
import com.passbolt.mobile.android.core.ui.R as CoreUiR

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

class PinEntryEditText
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : AbstractComposeView(context, attrs, defStyleAttr) {
        private var digitColor by mutableIntStateOf(context.getColor(CoreUiR.color.text_primary))
        private var onPinEnteredListener: OnPinEnteredListener? = null

        private var pinInputState: PinInputState? = null

        fun interface OnPinEnteredListener {
            operator fun invoke(pin: String)
        }

        fun setCustomTextColor(color: Int) {
            digitColor = color
        }

        fun setText(text: String?) {
            pinInputState?.setText(text)
        }

        fun setOnPinEnteredListener(listener: OnPinEnteredListener) {
            onPinEnteredListener = listener
        }

        @Composable
        override fun Content() {
            pinInputState =
                rememberPinInputState(
                    onPinComplete = {
                        onPinEnteredListener?.invoke(it)
                    },
                )

            val state = pinInputState ?: return

            PassboltTheme {
                PinInput(
                    state = state,
                    textColor = Color(digitColor),
                )
            }
        }
    }
