package com.passbolt.mobile.android.core.ui.header

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Composable
internal fun ActionIconButton(
    actionIcon: ActionIcon,
    onActionClick: () -> Unit,
) {
    if (actionIcon == ActionIcon.NONE) return

    IconButton(
        onClick = onActionClick,
        modifier = Modifier.size(30.dp),
    ) {
        Image(
            painter =
                painterResource(
                    when (actionIcon) {
                        ActionIcon.VIEW -> R.drawable.ic_eye_visible
                        ActionIcon.COPY -> R.drawable.ic_copy
                        ActionIcon.HIDE -> R.drawable.ic_eye_invisible
                        ActionIcon.NONE -> error("Icon is not shown for ActionIcon.NONE")
                    },
                ),
            contentDescription =
                stringResource(
                    when (actionIcon) {
                        ActionIcon.VIEW -> LocalizationR.string.action_show
                        ActionIcon.COPY -> LocalizationR.string.action_copy
                        ActionIcon.HIDE -> LocalizationR.string.action_hide
                        ActionIcon.NONE -> error("Icon is not shown for ActionIcon.NONE")
                    },
                ),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
        )
    }
}
