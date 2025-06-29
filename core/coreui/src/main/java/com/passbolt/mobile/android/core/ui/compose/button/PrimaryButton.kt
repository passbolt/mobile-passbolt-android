package com.passbolt.mobile.android.core.ui.compose.button

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        shape = RoundedCornerShape(4.dp),
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp),
    ) {
        Text(text = text)
    }
}

@Preview(showBackground = true)
@Composable
private fun PrimaryButtonPreview() {
    PrimaryButton(text = "Primary Button", onClick = {})
}
