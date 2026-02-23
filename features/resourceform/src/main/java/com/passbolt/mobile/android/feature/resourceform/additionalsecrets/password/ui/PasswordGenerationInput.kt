package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password.ui

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.compose.button.SecondaryIconButton
import com.passbolt.mobile.android.core.ui.compose.text.PasswordInput
import com.passbolt.mobile.android.ui.PasswordStrength
import com.passbolt.mobile.android.ui.PasswordStrength.Empty
import com.passbolt.mobile.android.ui.PasswordStrength.Fair
import com.passbolt.mobile.android.ui.PasswordStrength.Strong
import com.passbolt.mobile.android.ui.PasswordStrength.VeryStrong
import com.passbolt.mobile.android.ui.PasswordStrength.VeryWeak
import com.passbolt.mobile.android.ui.PasswordStrength.Weak
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun PasswordGenerationInput(
    password: String,
    passwordStrength: PasswordStrength,
    entropy: Double,
    onPasswordChange: (String) -> Unit,
    onGenerateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entropyShape = RoundedCornerShape(4.dp)

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            PasswordInput(
                title = stringResource(LocalizationR.string.resource_form_password),
                text = password,
                onTextChange = onPasswordChange,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))

            SecondaryIconButton(
                modifier = Modifier.size(56.dp),
                onClick = onGenerateClick,
                icon = painterResource(CoreUiR.drawable.ic_password_generate),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { passwordStrength.progress / 100f },
            modifier =
                Modifier
                    .padding(end = 62.dp)
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(entropyShape)
                    .border(width = 0.5.dp, color = Color.LightGray, shape = entropyShape),
            gapSize = 0.dp,
            drawStopIndicator = {},
            color = colorResource(getProgressColor(passwordStrength)),
            strokeCap = StrokeCap.Butt,
            trackColor = Color.Transparent,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = getStrengthDescription(LocalContext.current, passwordStrength, entropy),
            style = MaterialTheme.typography.bodyMedium,
            color = colorResource(getTextColor(passwordStrength)),
        )
    }
}

private fun getProgressColor(passwordStrength: PasswordStrength): Int =
    when (passwordStrength) {
        Empty -> android.R.color.transparent
        Fair, Strong -> CoreUiR.color.orange
        VeryStrong -> CoreUiR.color.green
        VeryWeak, Weak -> CoreUiR.color.red
    }

private fun getTextColor(passwordStrength: PasswordStrength): Int =
    when (passwordStrength) {
        Empty -> CoreUiR.color.text_tertiary
        else -> CoreUiR.color.text_primary
    }

private fun getStrengthDescription(
    context: Context,
    passwordStrength: PasswordStrength,
    entropy: Double,
): String =
    if (entropy > Double.NEGATIVE_INFINITY && entropy < Double.POSITIVE_INFINITY) {
        context.getString(
            LocalizationR.string.password_generator_known_strength_format,
            context.getString(getEntropyText(passwordStrength)),
            entropy,
        )
    } else {
        context.getString(LocalizationR.string.password_generator_unknown_strength_format)
    }

private fun getEntropyText(passwordStrength: PasswordStrength): Int =
    when (passwordStrength) {
        Empty -> LocalizationR.string.password_strength_empty
        Fair -> LocalizationR.string.password_strength_fair
        Strong -> LocalizationR.string.password_strength_strong
        VeryStrong -> LocalizationR.string.password_strength_very_strong
        VeryWeak -> LocalizationR.string.password_strength_very_weak
        Weak -> LocalizationR.string.password_strength_weak
    }
