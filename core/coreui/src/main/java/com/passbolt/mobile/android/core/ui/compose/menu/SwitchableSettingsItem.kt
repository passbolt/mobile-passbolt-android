package com.passbolt.mobile.android.core.ui.compose.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.compose.AppTypography
import com.passbolt.mobile.android.core.ui.R

@Composable
fun SwitchableSettingsItem(
    iconPainter: Painter?,
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = colorResource(R.color.icon_tint),
    isEnabled: Boolean = true,
) {
    @Suppress("MagicNumber")
    val alpha = if (isEnabled) 1f else 0.5f

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(alpha)
                .height(64.dp)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconPainter != null) {
            Image(
                painter = iconPainter,
                contentDescription = null,
                colorFilter = ColorFilter.tint(iconTint),
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = AppTypography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = isChecked,
            onCheckedChange = { if (isEnabled) onCheckedChange(it) },
            enabled = isEnabled,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SwitchableSettingsItemPreview() {
    SwitchableSettingsItem(
        iconPainter = painterResource(R.drawable.ic_bug),
        title = "Enable debug logs",
        isChecked = true,
        onCheckedChange = {},
    )
}
