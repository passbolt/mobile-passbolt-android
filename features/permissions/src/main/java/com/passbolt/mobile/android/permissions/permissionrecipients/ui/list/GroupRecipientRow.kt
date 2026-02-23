package com.passbolt.mobile.android.permissions.permissionrecipients.ui.list

import PassboltTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.ui.GroupModel

@Composable
internal fun GroupRecipientRow(
    group: GroupModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Image(
            painter = painterResource(R.drawable.ic_filled_group_with_bg),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = group.groupName,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        RadioButton(
            selected = isSelected,
            onClick = null,
        )
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun GroupRecipientRowSelectedPreview() {
    PassboltTheme {
        GroupRecipientRow(
            group = GroupModel(groupId = "1", groupName = "Engineering Team"),
            isSelected = true,
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GroupRecipientRowUnselectedPreview() {
    PassboltTheme {
        GroupRecipientRow(
            group = GroupModel(groupId = "1", groupName = "Engineering Team"),
            isSelected = false,
            onClick = {},
        )
    }
}
