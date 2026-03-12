package com.passbolt.mobile.android.feature.resourcedetails.details.ui

import PassboltTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.localization.R
import com.passbolt.mobile.android.core.ui.header.ActionIcon
import com.passbolt.mobile.android.core.ui.header.ItemWithHeader
import com.passbolt.mobile.android.core.ui.header.ValueStyle
import com.passbolt.mobile.android.core.ui.section.Section
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.CopyPassword
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.CopyUrl
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.CopyUsername
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.TogglePasswordVisibility

@Composable
internal fun PasswordSection(
    username: String,
    password: String,
    showPassword: Boolean,
    isPasswordUnmasked: Boolean,
    showPasswordEyeIcon: Boolean,
    mainUri: String,
    onIntent: (ResourceDetailsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUsernameEmpty = username.isBlank()

    Section(title = stringResource(R.string.resource_details_password_header), modifier = modifier) {
        Column {
            ItemWithHeader(
                headerText = stringResource(R.string.resource_details_username_header),
                value = if (isUsernameEmpty) stringResource(R.string.no_username) else username,
                actionIcon = if (isUsernameEmpty) ActionIcon.NONE else ActionIcon.COPY,
                valueFontStyle = if (isUsernameEmpty) FontStyle.Italic else FontStyle.Normal,
                onItemClick = { if (!isUsernameEmpty) onIntent(CopyUsername) },
                onActionClick = { if (!isUsernameEmpty) onIntent(CopyUsername) },
            )
            if (showPassword) {
                ItemWithHeader(
                    headerText = stringResource(R.string.resource_details_password_header),
                    value = if (isPasswordUnmasked) password else "",
                    valueStyle = ValueStyle.Secret(differentiateCharacters = isPasswordUnmasked),
                    actionIcon =
                        if (showPasswordEyeIcon) {
                            if (isPasswordUnmasked) ActionIcon.HIDE else ActionIcon.VIEW
                        } else {
                            ActionIcon.NONE
                        },
                    onItemClick = { onIntent(CopyPassword) },
                    onActionClick = { onIntent(TogglePasswordVisibility) },
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            ItemWithHeader(
                headerText = stringResource(R.string.resource_details_url_header),
                value = mainUri,
                valueStyle = ValueStyle.Linkified,
                actionIcon =
                    if (mainUri.isNotBlank()) {
                        ActionIcon.COPY
                    } else {
                        ActionIcon.NONE
                    },
                onItemClick = { onIntent(CopyUrl) },
                onActionClick = { onIntent(CopyUrl) },
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PasswordSectionPreview() {
    PassboltTheme {
        PasswordSection(
            username = "testuser@example.com",
            password = "secretPassword123",
            showPassword = true,
            isPasswordUnmasked = false,
            showPasswordEyeIcon = true,
            mainUri = "https://example.com",
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PasswordSectionNoUsernamePreview() {
    PassboltTheme {
        PasswordSection(
            username = "",
            password = "secretPassword123",
            showPassword = true,
            isPasswordUnmasked = false,
            showPasswordEyeIcon = true,
            mainUri = "https://example.com",
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PasswordSectionPasswordVisiblePreview() {
    PassboltTheme {
        PasswordSection(
            username = "testuser@example.com",
            password = "secretPassword123",
            showPassword = true,
            isPasswordUnmasked = true,
            showPasswordEyeIcon = true,
            mainUri = "",
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PasswordSectionDarkPreview() {
    PassboltTheme(darkTheme = true) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            PasswordSection(
                username = "testuser@example.com",
                password = "secretPassword123",
                showPassword = true,
                isPasswordUnmasked = false,
                showPasswordEyeIcon = true,
                mainUri = "https://example.com",
                onIntent = {},
            )
        }
    }
}
