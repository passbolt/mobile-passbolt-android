/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

package com.passbolt.mobile.android.core.ui.compose.progresstoolbar

import PassboltTheme
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Composable
fun ProgressToolbar(
    progress: Float,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes endIcon: Int? = null,
    onEndIconClick: (() -> Unit)? = null,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = PROGRESS_ANIMATION_DURATION_MILLIS),
        label = "progress",
    )

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackNavigationIcon(onBackClick = onBackClick)

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorResource(R.color.divider)),
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                    color = colorResource(R.color.red),
                    trackColor = Color.Transparent,
                    strokeCap = StrokeCap.Round,
                    drawStopIndicator = {},
                )
            }

            if (endIcon != null) {
                IconButton(
                    onClick = { onEndIconClick?.invoke() },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter = painterResource(endIcon),
                        contentDescription = stringResource(LocalizationR.string.help_button_description),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}

private const val PROGRESS_ANIMATION_DURATION_MILLIS = 250

@Preview(showBackground = true)
@Composable
private fun ProgressToolbarEmptyPreview() {
    PassboltTheme {
        ProgressToolbar(
            progress = 0f,
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProgressToolbar50Preview() {
    PassboltTheme {
        ProgressToolbar(
            progress = 0.5f,
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProgressToolbarCompletePreview() {
    PassboltTheme {
        ProgressToolbar(
            progress = 1f,
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProgressToolbarWithEndIconPreview() {
    PassboltTheme {
        ProgressToolbar(
            progress = 0.5f,
            onBackClick = {},
            endIcon = R.drawable.ic_help,
            onEndIconClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProgressToolbarDarkPreview() {
    PassboltTheme(darkTheme = true) {
        ProgressToolbar(
            progress = 0.5f,
            onBackClick = {},
            endIcon = R.drawable.ic_help,
        )
    }
}
