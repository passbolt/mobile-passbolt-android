package com.passbolt.mobile.android.feature.authentication.auth.accountdoesnotexist

import PassboltTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.passbolt.mobile.android.core.ui.button.PrimaryButton
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
fun AccountDoesNotExistDialog(
    label: String,
    email: String?,
    url: String,
    onConnectToExistingAccount: () -> Unit,
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
        AccountDoesNotExistContent(
            label = label,
            email = email,
            url = url,
            onConnectToExistingAccount = onConnectToExistingAccount,
        )
    }
}

@Composable
private fun AccountDoesNotExistContent(
    label: String,
    email: String?,
    url: String,
    onConnectToExistingAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
        ) {
            IconButton(
                onClick = onConnectToExistingAccount,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            ) {
                Image(
                    painter = painterResource(CoreUiR.drawable.ic_close),
                    contentDescription = null,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(56.dp))

                Image(
                    painter = painterResource(CoreUiR.drawable.ic_failed),
                    contentDescription = null,
                    modifier = Modifier.size(144.dp),
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(LocalizationR.string.dialog_account_does_not_exist_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(LocalizationR.string.dialog_account_does_not_exist_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(8.dp),
                            ).padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )

                    email?.let {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            PrimaryButton(
                text = stringResource(LocalizationR.string.dialog_account_does_not_exist_connect_account),
                onClick = onConnectToExistingAccount,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountDoesNotExistLightPreview() {
    PassboltTheme {
        AccountDoesNotExistContent(
            label = "John Doe",
            email = "john.doe@passbolt.com",
            url = "https://passbolt.com/johndoeorg",
            onConnectToExistingAccount = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountDoesNotExistDarkPreview() {
    PassboltTheme(darkTheme = true) {
        AccountDoesNotExistContent(
            label = "John Doe",
            email = "john.doe@passbolt.com",
            url = "https://passbolt.com/johndoeorg",
            onConnectToExistingAccount = {},
        )
    }
}
