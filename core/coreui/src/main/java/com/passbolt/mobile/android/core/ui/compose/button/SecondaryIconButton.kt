package com.passbolt.mobile.android.core.ui.compose.button

import PassboltTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.R

@Composable
fun SecondaryIconButton(
    onClick: () -> Unit,
    icon: Painter,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    OutlinedIconButton(
        onClick = onClick,
        colors =
            IconButtonDefaults.outlinedIconButtonColors(
                containerColor = colorResource(R.color.secondary_button_background),
                contentColor = colorResource(R.color.icon_tint),
            ),
        border = BorderStroke(width = 1.dp, Color.LightGray),
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
    ) {
        Icon(
            painter = icon,
            contentDescription = contentDescription,
            modifier = iconModifier,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SecondaryIconButtonPreview() {
    PassboltTheme {
        SecondaryIconButton(
            onClick = {},
            icon = painterResource(R.drawable.ic_trash),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SecondaryIconButtonWithCustomSizePreview() {
    PassboltTheme {
        SecondaryIconButton(
            onClick = {},
            icon = painterResource(R.drawable.ic_trash),
            iconModifier = Modifier.size(22.dp),
        )
    }
}
