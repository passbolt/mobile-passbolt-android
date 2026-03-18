package com.passbolt.mobile.android.core.ui.compose.permissions

import PassboltTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
fun PermissionSelector(
    selectedPermission: ResourcePermission,
    onPermissionSelect: (ResourcePermission) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ResourcePermission.entries.forEach { permission ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = permission == selectedPermission,
                            onClick = { onPermissionSelect(permission) },
                            role = Role.RadioButton,
                        ).padding(vertical = 8.dp),
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
                    modifier = Modifier.weight(1f),
                )

                RadioButton(
                    selected = permission == selectedPermission,
                    onClick = null,
                )

                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionSelectorReadPreview() {
    PassboltTheme {
        PermissionSelector(
            selectedPermission = ResourcePermission.READ,
            onPermissionSelect = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionSelectorOwnerPreview() {
    PassboltTheme(darkTheme = true) {
        PermissionSelector(
            selectedPermission = ResourcePermission.OWNER,
            onPermissionSelect = {},
        )
    }
}
