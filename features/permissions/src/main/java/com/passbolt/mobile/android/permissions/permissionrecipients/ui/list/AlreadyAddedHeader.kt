package com.passbolt.mobile.android.permissions.permissionrecipients.ui.list

import PassboltTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.localization.R

@Composable
internal fun AlreadyAddedHeader(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.permission_recipients_already_added),
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
        modifier = modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun AlreadyAddedHeaderPreview() {
    PassboltTheme {
        AlreadyAddedHeader()
    }
}
