package com.passbolt.mobile.android.core.ui.header

import android.util.Patterns
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink

internal fun getLinkifiedAnnotatedString(
    text: String,
    linkColor: Color,
): AnnotatedString {
    val regex = Patterns.WEB_URL.pattern().toRegex()

    return buildAnnotatedString {
        val finalIndex =
            regex.findAll(text).fold(0) { currentIndex, match ->
                append(text.substring(currentIndex, match.range.first))
                withLink(
                    LinkAnnotation.Url(
                        url = match.value,
                        styles =
                            TextLinkStyles(
                                style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                            ),
                    ),
                ) {
                    append(match.value)
                }
                match.range.last + 1
            }
        append(text.substring(finalIndex))
    }
}
