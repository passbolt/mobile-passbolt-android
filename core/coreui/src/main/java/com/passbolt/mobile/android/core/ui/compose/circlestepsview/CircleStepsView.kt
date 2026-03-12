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

package com.passbolt.mobile.android.core.ui.compose.circlestepsview

import PassboltTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.passbolt.mobile.android.core.ui.R

@Composable
fun CircleStepsView(
    steps: List<CircleStepItemModel>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        steps.forEachIndexed { index, step ->
            CircleStepRow(
                stepNumber = index + 1,
                text = step.text,
                icon = step.icon,
                showLine = index < steps.size - 1,
            )
        }
    }
}

@Composable
private fun CircleStepRow(
    stepNumber: Int,
    text: androidx.compose.ui.text.AnnotatedString,
    icon: CircleStepIcon?,
    showLine: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ImageTextCircle(
                stepNumber = stepNumber,
                icon = icon,
            )

            if (showLine) {
                Box(
                    modifier =
                        Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .padding(vertical = 4.dp)
                            .background(color = colorResource(R.color.divider)),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CircleStepsViewPreview() {
    PassboltTheme {
        CircleStepsView(
            steps =
                listOf(
                    CircleStepItemModel(
                        text = buildAnnotatedString { append("First step") },
                    ),
                    CircleStepItemModel(
                        text = buildAnnotatedString { append("Second step") },
                    ),
                    CircleStepItemModel(
                        text = buildAnnotatedString { append("Third step") },
                        icon = CircleStepIcon.Drawable(R.drawable.view_green_dot),
                    ),
                ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
