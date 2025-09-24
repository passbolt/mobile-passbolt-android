package com.passbolt.mobile.android.core.ui.compose.button

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.R

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    text: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    iconSize: Int = 16,
    iconSpacing: Int = 8,
) {
    TextButton(
        onClick = onClick,
        modifier =
            modifier
                .border(
                    width = 1.dp,
                    color = colorResource(R.color.divider),
                    shape = RoundedCornerShape(8.dp),
                ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = colorResource(R.color.icon_tint),
                modifier = Modifier.size(iconSize.dp),
            )
            Spacer(modifier = Modifier.width(iconSpacing.dp))
            Text(text = text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OutlinedButtonPreview() {
    OutlinedButton(
        onClick = {},
        text = "Sign out",
        icon = painterResource(id = R.drawable.ic_sign_out),
    )
}
