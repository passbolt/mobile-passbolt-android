package com.passbolt.mobile.android.core.ui.compose.circlestepsview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.passbolt.mobile.android.core.ui.R

@Composable
fun ImageTextCircle(
    stepNumber: Int,
    icon: CircleStepIcon?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(24.dp)
                .border(width = 1.dp, color = colorResource(R.color.divider), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when (icon) {
            is CircleStepIcon.Content -> icon.content()
            is CircleStepIcon.Drawable ->
                Image(
                    painter = painterResource(id = icon.drawableRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            null ->
                Text(
                    text = stepNumber.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ImageTextCirclePreview() {
    MaterialTheme {
        Box(
            modifier =
                Modifier
                    .background(Color.White)
                    .padding(16.dp),
        ) {
            ImageTextCircle(
                stepNumber = 1,
                icon = null,
            )
        }
    }
}
