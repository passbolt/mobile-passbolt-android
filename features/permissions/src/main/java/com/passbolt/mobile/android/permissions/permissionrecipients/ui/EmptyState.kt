package com.passbolt.mobile.android.permissions.permissionrecipients.ui

import PassboltTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(horizontal = 80.dp),
    ) {
        Text(
            text = stringResource(LocalizationR.string.permission_recipients_user_or_group_not_found),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
            fontSize = 28.sp,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Image(
            painter = painterResource(CoreUiR.drawable.ic_empty_state),
            contentDescription = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    PassboltTheme {
        EmptyState(modifier = Modifier.fillMaxSize())
    }
}
