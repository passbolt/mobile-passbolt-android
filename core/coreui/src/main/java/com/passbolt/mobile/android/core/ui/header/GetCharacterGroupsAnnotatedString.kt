package com.passbolt.mobile.android.core.ui.header

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

internal fun getCharacterGroupsAnnotatedString(
    text: String,
    digitColor: Color,
    specialCharColor: Color,
): AnnotatedString =
    buildAnnotatedString {
        text.forEach { char ->
            val color =
                when {
                    char.isDigit() -> digitColor
                    !char.isLetterOrDigit() -> specialCharColor
                    else -> null
                }

            if (color != null) {
                withStyle(SpanStyle(color = color)) {
                    append(char)
                }
            } else {
                append(char)
            }
        }
    }
