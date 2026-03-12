package com.passbolt.mobile.android.core.ui.menu

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.compose.AppTypography
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
fun OpenableSettingsItem(
    iconPainter: Painter?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = colorResource(R.color.icon_tint),
    isEnabled: Boolean = true,
    opensInternally: Boolean = true,
    hasWarningBadge: Boolean = false,
) {
    @Suppress("MagicNumber")
    val alpha = if (isEnabled) 1f else 0.5f

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(alpha)
                .height(64.dp)
                .clickable(enabled = isEnabled) { onClick() }
                .padding(horizontal = 16.dp)
                .testTag(OpenableSettingsItem.TestTags.ITEM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconPainter != null) {
            Image(
                painter = iconPainter,
                contentDescription = title,
                colorFilter = ColorFilter.tint(iconTint),
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.width(16.dp))

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = AppTypography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            if (hasWarningBadge) {
                Image(
                    modifier =
                        Modifier
                            .padding(start = 16.dp)
                            .size(20.dp),
                    painter = painterResource(CoreUiR.drawable.ic_excl_indicator),
                    contentDescription = null,
                )
            }
        }

        if (opensInternally) {
            Image(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                colorFilter = ColorFilter.tint(iconTint),
                modifier =
                    Modifier
                        .size(16.dp)
                        .padding(start = 8.dp)
                        .testTag(OpenableSettingsItem.TestTags.ARROW),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OpenableSettingsItemPreview() {
    OpenableSettingsItem(
        iconPainter = painterResource(R.drawable.ic_app_settings),
        title = "App settings",
        onClick = { },
    )
}

@Preview(showBackground = true)
@Composable
private fun OpenableSettingsItemWithWarningPreview() {
    OpenableSettingsItem(
        iconPainter = painterResource(R.drawable.ic_app_settings),
        title = "App settings",
        onClick = { },
        hasWarningBadge = true,
    )
}

@VisibleForTesting
object OpenableSettingsItem {
    object TestTags {
        const val ITEM = "OpenableSettingsItem"
        const val ARROW = "OpenableSettingsItemArrow"
    }
}
