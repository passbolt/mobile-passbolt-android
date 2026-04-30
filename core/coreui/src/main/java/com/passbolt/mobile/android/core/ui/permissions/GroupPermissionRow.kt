package com.passbolt.mobile.android.core.ui.permissions

import PassboltTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission

@Composable
fun GroupPermissionRow(
    permission: PermissionModelUi.GroupPermissionModel,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Image(
            painter = painterResource(R.drawable.ic_filled_group_with_bg),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = permission.group.groupName,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(getPermissionNameRes(permission.permission)),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier =
                Modifier
                    .background(
                        color = colorResource(R.color.divider),
                        shape = RoundedCornerShape(4.dp),
                    ).padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun GroupPermissionRowPreview() {
    PassboltTheme {
        GroupPermissionRow(
            permission =
                PermissionModelUi.GroupPermissionModel(
                    permission = ResourcePermission.OWNER,
                    permissionId = "1",
                    group =
                        GroupModel(
                            groupId = "1",
                            groupName = "Engineering Team",
                        ),
                ),
        )
    }
}
