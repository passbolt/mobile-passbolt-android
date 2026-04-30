package com.passbolt.mobile.android.core.ui.permissions

import PassboltTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
fun PermissionLabel(
    permission: ResourcePermission,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(getPermissionIconRes(permission)),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = colorResource(CoreUiR.color.icon_tint),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = stringResource(getPermissionNameRes(permission)),
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionLabelReadPreview() {
    PassboltTheme {
        PermissionLabel(permission = ResourcePermission.READ)
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionLabelUpdatePreview() {
    PassboltTheme {
        PermissionLabel(permission = ResourcePermission.UPDATE)
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionLabelOwnerPreview() {
    PassboltTheme {
        PermissionLabel(permission = ResourcePermission.OWNER)
    }
}
