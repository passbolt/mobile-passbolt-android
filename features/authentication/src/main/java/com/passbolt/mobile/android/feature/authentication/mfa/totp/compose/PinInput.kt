package com.passbolt.mobile.android.feature.authentication.mfa.totp.compose

import PassboltTheme
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

private const val ANIMATION_PIN_DIGIT_SCALE_KEY = "PinDigitScale"
private const val ANIMATION_START_DIGIT_SCALE = 0.7f
private const val ANIMATION_END_DIGIT_SCALE = 1f

@Composable
fun PinInput(
    state: PinInputState,
    textColor: Color,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = true,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (autoFocus) {
            focusRequester.requestFocus()
        }
    }

    BasicTextField(
        value = state.textFieldValue,
        onValueChange = state::onValueChange,
        modifier = modifier.focusRequester(focusRequester),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = { PinInputDecorationBox(value = state.textFieldValue, textColor = textColor, length = state.maxLength) },
        cursorBrush = SolidColor(Color.Transparent),
        textStyle = TextStyle(color = Color.Transparent),
    )
}

@Composable
private fun PinInputDecorationBox(
    value: TextFieldValue,
    textColor: Color,
    length: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(length) { index ->
            val char =
                value.text
                    .getOrNull(index)
                    ?.toString()
                    .orEmpty()

            PinDigitCell(
                char = char,
                isFocused = index == value.text.length,
                textColor = textColor,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PinDigitCell(
    char: String,
    isFocused: Boolean,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val isFilled = char.isNotEmpty()
    val underlineColor = if (isFilled || isFocused) MaterialTheme.colorScheme.primary else Color.LightGray
    val targetScale = if (isFilled) ANIMATION_END_DIGIT_SCALE else ANIMATION_START_DIGIT_SCALE

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(),
        label = ANIMATION_PIN_DIGIT_SCALE_KEY,
    )

    Column(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = char,
            style =
                TextStyle(
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    color = textColor,
                    fontSize = 45.sp,
                ),
            modifier = Modifier.scale(scale),
        )

        HorizontalDivider(
            modifier = Modifier.width(38.dp),
            color = underlineColor,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PinInputPreview() {
    PassboltTheme {
        PinInput(
            state = rememberPinInputState(initialValue = "123"),
            textColor = Color.Black,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PinInputEmptyPreview() {
    PassboltTheme {
        val state = rememberPinInputState()
        PinInput(
            state = state,
            textColor = Color.Black,
        )
    }
}
