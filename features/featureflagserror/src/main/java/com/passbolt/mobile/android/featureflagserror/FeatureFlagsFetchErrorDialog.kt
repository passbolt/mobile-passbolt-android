package com.passbolt.mobile.android.featureflagserror

import PassboltTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.passbolt.mobile.android.core.ui.button.PrimaryButton
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreR

@Composable
fun FeatureFlagsFetchErrorDialog(
    isVisible: Boolean,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = {},
            properties =
                DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false,
                ),
        ) {
            FeatureFlagsFetchErrorContent(
                onRetry = onRetry,
                onSignOut = onSignOut,
            )
        }
    }
}

@Composable
private fun FeatureFlagsFetchErrorContent(
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.spacedBy(
                        space = 16.dp,
                        alignment = Alignment.CenterVertically,
                    ),
            ) {
                Text(
                    text = stringResource(id = LocalizationR.string.common_startup_configuration_fetch_error),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier.padding(
                            start = 72.dp,
                            end = 72.dp,
                            bottom = 16.dp,
                        ),
                )

                Image(
                    painter = painterResource(id = CoreR.drawable.apps_list),
                    contentDescription = null,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            PrimaryButton(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 72.dp),
                onClick = onRetry,
                text = stringResource(id = LocalizationR.string.common_refresh),
            )

            TextButton(
                onClick = onSignOut,
                modifier = Modifier.padding(bottom = 32.dp),
            ) {
                Text(
                    text = stringResource(id = LocalizationR.string.common_sign_out),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FeatureFlagsFetchErrorUiLightPreview() {
    PassboltTheme {
        FeatureFlagsFetchErrorContent(
            onRetry = {},
            onSignOut = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FeatureFlagsFetchErrorUiDarkPreview() {
    PassboltTheme(darkTheme = true) {
        FeatureFlagsFetchErrorContent(
            onRetry = {},
            onSignOut = {},
        )
    }
}
