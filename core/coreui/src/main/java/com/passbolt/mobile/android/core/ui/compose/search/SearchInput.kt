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
package com.passbolt.mobile.android.core.ui.compose.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.compose.circularimage.CircularProfileImage
import com.passbolt.mobile.android.core.ui.compose.search.SearchInputEndIconMode.AVATAR
import com.passbolt.mobile.android.core.ui.compose.search.SearchInputEndIconMode.CLEAR

@Composable
fun SearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    avatarUrl: String?,
    endIconMode: SearchInputEndIconMode,
    modifier: Modifier = Modifier,
    onEndIconClick: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            when (endIconMode) {
                AVATAR ->
                    CircularProfileImage(
                        imageUrl = avatarUrl,
                        width = 28.dp,
                        height = 28.dp,
                        modifier = Modifier.clickable { onEndIconClick?.invoke() },
                    )
                CLEAR ->
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        modifier =
                            Modifier
                                .clickable { onEndIconClick?.invoke() },
                    )
            }
        },
        placeholder = { Text(placeholder) },
        textStyle = MaterialTheme.typography.displayMedium,
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = colorResource(R.color.input_box_interior),
                unfocusedContainerColor = colorResource(R.color.input_box_interior),
                focusedIndicatorColor = colorResource(R.color.input_box_stroke),
                unfocusedIndicatorColor = colorResource(R.color.input_box_stroke),
            ),
        modifier =
            modifier
                .fillMaxWidth(),
    )
}
