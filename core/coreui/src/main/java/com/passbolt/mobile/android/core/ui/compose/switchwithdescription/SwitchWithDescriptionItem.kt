package com.passbolt.mobile.android.core.ui.compose.switchwithdescription

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.R

@Composable
fun SwitchWithDescriptionItem(
    title: String,
    description: String,
    isChecked: Boolean,
    modifier: Modifier = Modifier.Companion,
    additionalDescription: String? = null,
    onClick: () -> Unit = {},
    isEnabled: Boolean = true,
) {
    @Suppress("MagicNumber")
    val alpha = if (isEnabled) 1f else 0.5f

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp)
                .clickable(enabled = isEnabled) { onClick() }
                .alpha(alpha),
    ) {
        Column(
            modifier =
                Modifier.Companion
                    .padding(end = 8.dp)
                    .weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colorResource(R.color.primary),
            )

            Text(
                text = description,
                modifier = Modifier.Companion.padding(top = 4.dp),
                color = colorResource(R.color.text_secondary),
            )

            if (additionalDescription != null) {
                Text(
                    text = additionalDescription,
                    modifier = Modifier.Companion.padding(top = 4.dp),
                    color = colorResource(R.color.text_secondary),
                )
            }
        }

        Switch(
            checked = isChecked,
            onCheckedChange = null,
            enabled = isEnabled,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SwitchWithDescriptionItemPreview() {
    SwitchWithDescriptionItem(
        title = "Autofill switch title",
        description = "Autofill switch description",
        additionalDescription = "Additional description for the switch",
        isChecked = true,
    )
}
