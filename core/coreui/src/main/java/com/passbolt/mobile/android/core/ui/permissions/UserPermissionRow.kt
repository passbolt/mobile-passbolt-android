package com.passbolt.mobile.android.core.ui.permissions

import PassboltTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.circularimage.CircularProfileImage
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.UserWithAvatar
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
fun UserPermissionRow(
    permission: PermissionModelUi.UserPermissionModel,
    modifier: Modifier = Modifier,
    disabledUserAlpha: Float = 0.5f,
) {
    val isDisabled = permission.user.isDisabled
    val alpha = if (isDisabled) disabledUserAlpha else 1f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        CircularProfileImage(
            imageUrl = permission.user.avatarUrl,
            width = 40.dp,
            height = 40.dp,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text =
                    if (isDisabled) {
                        stringResource(LocalizationR.string.name_suspended, permission.user.fullName)
                    } else {
                        permission.user.fullName
                    },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.alpha(alpha),
            )
            Text(
                text = permission.user.userName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.alpha(alpha),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(getPermissionNameRes(permission.permission)),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier =
                Modifier
                    .background(
                        color = colorResource(CoreUiR.color.divider),
                        shape = RoundedCornerShape(4.dp),
                    ).padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun UserPermissionRowPreview() {
    PassboltTheme {
        UserPermissionRow(
            permission =
                PermissionModelUi.UserPermissionModel(
                    permission = ResourcePermission.READ,
                    permissionId = "1",
                    user =
                        UserWithAvatar(
                            userId = "1",
                            firstName = "John",
                            lastName = "Doe",
                            userName = "john@passbolt.com",
                            isDisabled = false,
                            avatarUrl = null,
                        ),
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UserPermissionRowDisabledPreview() {
    PassboltTheme {
        UserPermissionRow(
            permission =
                PermissionModelUi.UserPermissionModel(
                    permission = ResourcePermission.UPDATE,
                    permissionId = "2",
                    user =
                        UserWithAvatar(
                            userId = "2",
                            firstName = "Jane",
                            lastName = "Smith",
                            userName = "jane@passbolt.com",
                            isDisabled = true,
                            avatarUrl = null,
                        ),
                ),
        )
    }
}
