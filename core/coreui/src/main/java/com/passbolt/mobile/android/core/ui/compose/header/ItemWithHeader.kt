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

package com.passbolt.mobile.android.core.ui.compose.header

import PassboltTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.passbolt.mobile.android.core.ui.compose.header.ActionIcon.COPY
import com.passbolt.mobile.android.core.ui.compose.header.ActionIcon.HIDE
import com.passbolt.mobile.android.core.ui.compose.header.ActionIcon.NONE
import com.passbolt.mobile.android.core.ui.compose.header.ActionIcon.VIEW
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
fun ItemWithHeader(
    headerText: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    valueStyle: ValueStyle = ValueStyle.Plain,
    actionIcon: ActionIcon = NONE,
    isTextSelectable: Boolean = false,
    valueFontStyle: FontStyle = FontStyle.Normal,
    onItemClick: () -> Unit = {},
    onActionClick: () -> Unit = {},
    content: (@Composable () -> Unit)? = null,
) {
    val secretFont = FontFamily(Font(CoreUiR.font.inconsolata))
    val regularFont = FontFamily(Font(CoreUiR.font.inter))
    val isSecret = valueStyle is ValueStyle.Secret

    val displayValue =
        when {
            value == null -> null
            value.isEmpty() && isSecret -> stringResource(LocalizationR.string.hidden_secret)
            else -> value
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onItemClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (valueStyle is ValueStyle.Concealed) {
            ConcealedContent(modifier = Modifier.weight(1f))
        } else {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                HeaderText(headerText)

                if (displayValue != null) {
                    ValueText(
                        displayValue = displayValue,
                        valueStyle = valueStyle,
                        valueFontStyle = valueFontStyle,
                        isTextSelectable = isTextSelectable,
                        secretFont = secretFont,
                        regularFont = regularFont,
                    )
                }

                content?.invoke()
            }
        }

        ActionIconButton(
            actionIcon = actionIcon,
            onActionClick = onActionClick,
        )
    }
}

@Composable
private fun HeaderText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ConcealedContent(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(CoreUiR.drawable.image_concealed),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier =
            modifier
                .heightIn(min = 64.dp)
                .background(colorResource(CoreUiR.color.section_background)),
    )
}

@Composable
private fun ValueText(
    displayValue: String,
    valueStyle: ValueStyle,
    valueFontStyle: FontStyle,
    isTextSelectable: Boolean,
    secretFont: FontFamily,
    regularFont: FontFamily,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val isSecret = valueStyle is ValueStyle.Secret
    val textStyle =
        MaterialTheme.typography.bodyLarge.copy(
            fontFamily = if (isSecret) secretFont else regularFont,
            fontSize = if (isSecret) 18.sp else 14.sp,
            fontStyle = valueFontStyle,
            color = MaterialTheme.colorScheme.onSurface,
        )

    val digitColor = colorResource(CoreUiR.color.red)
    val specialCharColor = colorResource(CoreUiR.color.primary)

    val annotatedText =
        when (valueStyle) {
            is ValueStyle.Linkified -> getLinkifiedAnnotatedString(displayValue, linkColor)
            is ValueStyle.Secret ->
                if (valueStyle.differentiateCharacters) {
                    getCharacterGroupsAnnotatedString(displayValue, digitColor, specialCharColor)
                } else {
                    AnnotatedString(displayValue)
                }
            else -> AnnotatedString(displayValue)
        }

    val textContent: @Composable () -> Unit = {
        Text(
            text = annotatedText,
            style = textStyle,
        )
    }

    if (isTextSelectable) {
        SelectionContainer { textContent() }
    } else {
        textContent()
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemWithHeaderPreview() {
    PassboltTheme {
        ItemWithHeader(
            headerText = "Sample Header",
            modifier = Modifier.padding(16.dp),
            content = {
                Text(
                    text = "Custom content",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemWithHeaderPasswordPreview() {
    PassboltTheme {
        ItemWithHeader(
            headerText = "Password",
            value = "MyP@ssw0rd!",
            valueStyle = ValueStyle.Secret(differentiateCharacters = true),
            actionIcon = HIDE,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemWithHeaderHiddenPreview() {
    PassboltTheme {
        ItemWithHeader(
            headerText = "Password",
            value = "",
            valueStyle = ValueStyle.Secret(),
            actionIcon = VIEW,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemWithHeaderUsernamePreview() {
    PassboltTheme {
        ItemWithHeader(
            headerText = "Username",
            value = "john.doe@example.com",
            actionIcon = COPY,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemWithHeaderLinkifiedPreview() {
    PassboltTheme {
        ItemWithHeader(
            headerText = "URL",
            value = "Visit https://example.com for more info",
            valueStyle = ValueStyle.Linkified,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemWithHeaderNoUsernamePreview() {
    PassboltTheme {
        ItemWithHeader(
            headerText = "Username",
            value = "no username",
            actionIcon = NONE,
            valueFontStyle = FontStyle.Italic,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemWithHeaderConcealedPreview() {
    PassboltTheme {
        ItemWithHeader(
            headerText = "Password",
            value = "secret",
            valueStyle = ValueStyle.Concealed,
            actionIcon = VIEW,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemWithHeaderDarkPreview() {
    PassboltTheme(darkTheme = true) {
        ItemWithHeader(
            headerText = "Sample Header",
            modifier = Modifier.padding(16.dp),
            content = {
                Text(
                    text = "Custom content",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemWithHeaderPasswordDarkPreview() {
    PassboltTheme(darkTheme = true) {
        ItemWithHeader(
            headerText = "Password",
            value = "MyP@ssw0rd!",
            valueStyle = ValueStyle.Secret(differentiateCharacters = true),
            actionIcon = HIDE,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemWithHeaderHiddenDarkPreview() {
    PassboltTheme(darkTheme = true) {
        ItemWithHeader(
            headerText = "Password",
            value = "",
            valueStyle = ValueStyle.Secret(),
            actionIcon = VIEW,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemWithHeaderUsernameDarkPreview() {
    PassboltTheme(darkTheme = true) {
        ItemWithHeader(
            headerText = "Username",
            value = "john.doe@example.com",
            actionIcon = COPY,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemWithHeaderConcealedDarkPreview() {
    PassboltTheme(darkTheme = true) {
        ItemWithHeader(
            headerText = "Password",
            value = "secret",
            valueStyle = ValueStyle.Concealed,
            actionIcon = VIEW,
            modifier = Modifier.padding(16.dp),
        )
    }
}
