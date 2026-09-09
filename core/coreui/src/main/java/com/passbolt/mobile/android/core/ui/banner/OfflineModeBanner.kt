package com.passbolt.mobile.android.core.ui.banner

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.passbolt.mobile.android.core.compose.PassboltTheme
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.localization.R as LocalizationR

/**
 * Shown while the app runs an offline session: the server was unreachable at sign-in and
 * the user is reading from the local encrypted cache.
 */
@Composable
fun OfflineModeBanner(
    lastSyncEpochMillis: Long?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colorResource(R.color.yellow))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lock),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = stringResource(LocalizationR.string.offline_banner_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text =
                    if (lastSyncEpochMillis != null) {
                        stringResource(
                            LocalizationR.string.offline_banner_last_sync,
                            DateUtils.getRelativeTimeSpanString(lastSyncEpochMillis).toString(),
                        )
                    } else {
                        stringResource(LocalizationR.string.offline_banner_read_only)
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OfflineModeBannerPreview() {
    PassboltTheme {
        OfflineModeBanner(lastSyncEpochMillis = System.currentTimeMillis())
    }
}
