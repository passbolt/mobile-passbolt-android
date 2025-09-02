package com.passbolt.mobile.android.core.ui.compose.text

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Default
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Error

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

@Composable
fun TextInput(
    title: String,
    modifier: Modifier = Modifier,
    hint: String = "",
    isRequired: Boolean = false,
    state: StatefulInput.State = Default,
    text: String = "",
    onTextChange: (String) -> Unit = {},
) {
    val titleColor = if (state is Error) colorResource(R.color.red) else MaterialTheme.colorScheme.onBackground
    val label =
        if (isRequired) {
            buildAnnotatedString {
                append("$title ")
                withStyle(SpanStyle(color = colorResource(R.color.red))) { append("*") }
            }
        } else {
            buildAnnotatedString { append(title) }
        }

    Column(modifier = modifier) {
        Text(text = label, color = titleColor, style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { onTextChange(it) },
            placeholder = { Text(hint) },
            isError = state is Error,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state is Error) {
            Text(
                text = state.message,
                color = colorResource(R.color.red),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TextInputViewPreview_Default() {
    MaterialTheme {
        TextInput(
            title = "Name",
            hint = "Enter your name",
            isRequired = true,
            text = "John Doe",
            state = Default,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TextInputViewPreview_Error() {
    MaterialTheme {
        TextInput(
            title = "Name",
            hint = "Enter your name",
            isRequired = true,
            text = "",
            state = Error("This field is required"),
        )
    }
}
