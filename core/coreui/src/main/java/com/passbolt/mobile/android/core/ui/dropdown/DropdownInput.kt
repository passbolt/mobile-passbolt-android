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

package com.passbolt.mobile.android.core.ui.dropdown

import PassboltTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType.Companion.PrimaryNotEditable
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownInput(
    title: String,
    items: List<String>,
    selectedItem: String,
    onItemSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }

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
        Text(text = label, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = selectedItem,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .menuAnchor(PrimaryNotEditable),
                colors =
                    MaterialTheme.colorScheme.surfaceVariant.let {
                        OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = it,
                            unfocusedContainerColor = it,
                        )
                    },
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(text = item) },
                        onClick = {
                            onItemSelect(item)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DropdownInputPreview() {
    PassboltTheme {
        DropdownInput(
            title = "Algorithm",
            items = listOf("SHA1", "SHA256", "SHA512"),
            selectedItem = "SHA1",
            onItemSelect = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DropdownInputRequiredPreview() {
    PassboltTheme {
        DropdownInput(
            title = "Algorithm",
            items = listOf("SHA1", "SHA256", "SHA512"),
            selectedItem = "SHA256",
            onItemSelect = {},
            isRequired = true,
        )
    }
}
