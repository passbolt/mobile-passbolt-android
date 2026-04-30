package com.passbolt.mobile.android.feature.resourcedetails.details.ui

import PassboltTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.header.ItemWithHeader
import com.passbolt.mobile.android.core.ui.sharedwith.SharedWithSection
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.UserWithAvatar
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun SharedWithSection(
    permissions: List<PermissionModelUi>,
    modifier: Modifier = Modifier,
    onShareWithClick: (() -> Unit)? = null,
) {
    ItemWithHeader(headerText = stringResource(LocalizationR.string.shared_with), modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onShareWithClick?.invoke() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SharedWithSection(
                permissions = permissions,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(top = 16.dp),
            )
            if (onShareWithClick != null) {
                Image(
                    painter = painterResource(CoreUiR.drawable.ic_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
        }
    }
}

private fun createPreviewUser(
    firstName: String,
    lastName: String,
) = UserWithAvatar(
    userId = "user-${firstName.lowercase()}",
    firstName = firstName,
    lastName = lastName,
    userName = "${firstName.lowercase()}.${lastName.lowercase()}@example.com",
    isDisabled = false,
    avatarUrl = null,
)

@Preview(showBackground = true)
@Composable
private fun SharedWithSectionPreview() {
    PassboltTheme {
        SharedWithSection(
            permissions =
                listOf(
                    PermissionModelUi.UserPermissionModel(
                        permission = ResourcePermission.OWNER,
                        permissionId = "perm-1",
                        user = createPreviewUser("John", "Doe"),
                    ),
                    PermissionModelUi.UserPermissionModel(
                        permission = ResourcePermission.UPDATE,
                        permissionId = "perm-2",
                        user = createPreviewUser("Jane", "Smith"),
                    ),
                    PermissionModelUi.UserPermissionModel(
                        permission = ResourcePermission.READ,
                        permissionId = "perm-3",
                        user = createPreviewUser("Bob", "Wilson"),
                    ),
                ),
            onShareWithClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun SharedWithSectionDarkPreview() {
    PassboltTheme(darkTheme = true) {
        SharedWithSection(
            permissions =
                listOf(
                    PermissionModelUi.UserPermissionModel(
                        permission = ResourcePermission.OWNER,
                        permissionId = "perm-1",
                        user = createPreviewUser("John", "Doe"),
                    ),
                    PermissionModelUi.UserPermissionModel(
                        permission = ResourcePermission.UPDATE,
                        permissionId = "perm-2",
                        user = createPreviewUser("Jane", "Smith"),
                    ),
                ),
            onShareWithClick = {},
        )
    }
}
