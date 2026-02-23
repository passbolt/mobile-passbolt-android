package com.passbolt.mobile.android.core.ui.compose.button

import PassboltTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.R

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    colors: ButtonColors =
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            disabledContainerColor = colorResource(R.color.primary_disabled),
            disabledContentColor = Color.White,
        ),
) {
    Button(
        shape = RoundedCornerShape(4.dp),
        onClick = onClick,
        enabled = isEnabled,
        colors = colors,
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp),
    ) {
        Text(text = text, color = Color.White)
    }
}

@Preview(showBackground = true)
@Composable
private fun PrimaryButtonPreview() {
    PassboltTheme {
        PrimaryButton(text = "Primary Button", onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun DisabledPrimaryButtonPreview() {
    PassboltTheme {
        PrimaryButton(text = "Primary Button", isEnabled = false, onClick = {})
    }
}
