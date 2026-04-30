package com.passbolt.mobile.android.feature.authentication.auth

import PassboltTheme
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.passbolt.mobile.android.core.formatter.FingerprintFormatter
import com.passbolt.mobile.android.core.ui.button.PrimaryButton
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
fun ServerFingerprintChangedDialog(
    fingerprint: String,
    onAcceptNewKey: (String) -> Unit,
    onBack: () -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties =
            DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
    ) {
        BackHandler { onBack() }

        ServerFingerprintChangedContent(
            fingerprint = fingerprint,
            onAcceptNewKey = { onAcceptNewKey(fingerprint) },
        )
    }
}

@Composable
private fun ServerFingerprintChangedContent(
    fingerprint: String,
    onAcceptNewKey: () -> Unit,
    modifier: Modifier = Modifier,
    fingerprintFormatter: FingerprintFormatter = koinInject(),
) {
    var isChecked by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Image(
                painter = painterResource(CoreUiR.drawable.logo_text_icon),
                contentDescription = null,
                modifier = Modifier.height(24.dp),
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = stringResource(LocalizationR.string.auth_server_fingerprint_changed_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(LocalizationR.string.auth_server_fingerprint_changed_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp),
            )

            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = fingerprintFormatter.formatWithRawFallback(fingerprint, appendMiddleSpacing = true),
                fontFamily = FontFamily(Font(CoreUiR.font.inconsolata)),
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { isChecked = it },
                )
                Text(
                    text = stringResource(LocalizationR.string.auth_server_fingerprint_changed_checkbox),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            PrimaryButton(
                text = stringResource(LocalizationR.string.auth_server_fingerprint_changed_button),
                onClick = onAcceptNewKey,
                isEnabled = isChecked,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ServerFingerprintChangedLightPreview() {
    PassboltTheme {
        ServerFingerprintChangedContent(
            fingerprint = "9FDC781BE55539D4CB50FF4286DB2BDD17D334B1",
            onAcceptNewKey = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ServerFingerprintChangedDarkPreview() {
    PassboltTheme(darkTheme = true) {
        ServerFingerprintChangedContent(
            fingerprint = "9FDC781BE55539D4CB50FF4286DB2BDD17D334B1",
            onAcceptNewKey = {},
        )
    }
}
