package com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.conflict

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun AutofillConflictBanner(modifier: Modifier = Modifier) {
    Surface(
        color = colorResource(CoreUiR.color.warning),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(CoreUiR.drawable.ic_alert_triangle),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.background,
            )
            Text(
                text = stringResource(LocalizationR.string.autofill_conflict_message_detailed),
                color = MaterialTheme.colorScheme.background,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
