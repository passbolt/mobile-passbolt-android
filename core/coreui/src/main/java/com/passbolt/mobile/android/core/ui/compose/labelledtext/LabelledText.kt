package com.passbolt.mobile.android.core.ui.compose.labelledtext

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.passbolt.mobile.android.core.compose.Inconsolata
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
fun LabelledText(
    label: String,
    text: String,
    modifier: Modifier = Modifier,
    useMonospaceFont: Boolean = false,
    endAction: LabelledTextEndAction? = null,
) {
    val textStyle =
        if (useMonospaceFont) {
            TextStyle(
                fontFamily = Inconsolata,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
            )
        } else {
            LocalTextStyle.current
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(text = label)
            Spacer(Modifier.height(8.dp))
            Text(text = text, style = textStyle, color = colorResource(CoreUiR.color.text_secondary))
        }

        endAction?.let {
            IconButton(onClick = it.action) {
                Icon(
                    painter = painterResource(id = it.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
