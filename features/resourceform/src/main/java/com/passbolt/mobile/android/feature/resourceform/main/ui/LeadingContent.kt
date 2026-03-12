package com.passbolt.mobile.android.feature.resourceform.main.ui

import PassboltTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.button.SecondaryIconButton
import com.passbolt.mobile.android.core.ui.section.Section
import com.passbolt.mobile.android.core.ui.text.TextInput
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Default
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Error
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note.NoteValidationError
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password.ui.PasswordGenerationInput
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpSecretValidationError
import com.passbolt.mobile.android.feature.resourceform.main.NoteData
import com.passbolt.mobile.android.feature.resourceform.main.PasswordData
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GeneratePassword
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToTotpMoreSettings
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.NoteChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.PasswordMainUriTextChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.PasswordTextChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.PasswordUsernameTextChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.ScanTotp
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.TotpSecretChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.TotpUrlChanged
import com.passbolt.mobile.android.feature.resourceform.main.TotpData
import com.passbolt.mobile.android.feature.resourceform.main.getNoteErrorMessage
import com.passbolt.mobile.android.feature.resourceform.main.getTotpSecretErrorMessage
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.LeadingContentType.CUSTOM_FIELDS
import com.passbolt.mobile.android.ui.LeadingContentType.PASSWORD
import com.passbolt.mobile.android.ui.LeadingContentType.STANDALONE_NOTE
import com.passbolt.mobile.android.ui.LeadingContentType.TOTP
import com.passbolt.mobile.android.ui.PasswordStrength
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun LeadingContent(
    leadingContentType: LeadingContentType?,
    passwordData: PasswordData,
    totpData: TotpData,
    noteData: NoteData,
    onIntent: (ResourceFormIntent) -> Unit,
) {
    when (leadingContentType) {
        PASSWORD ->
            PasswordSection(
                mainUri = passwordData.mainUri,
                username = passwordData.username,
                password = passwordData.password,
                passwordStrength = passwordData.passwordStrength,
                passwordEntropyBits = passwordData.passwordEntropyBits,
                onIntent = onIntent,
            )
        TOTP ->
            TotpSection(
                totpSecret = totpData.totpSecret,
                totpSecretError = totpData.totpSecretError,
                totpIssuer = totpData.totpIssuer,
                onIntent = onIntent,
            )
        STANDALONE_NOTE ->
            StandaloneNoteSection(
                note = noteData.note,
                noteError = noteData.noteError,
                onIntent = onIntent,
            )
        CUSTOM_FIELDS, null -> {
            // no leading form
        }
    }
}

@Composable
private fun PasswordSection(
    mainUri: String,
    username: String,
    password: String,
    passwordStrength: PasswordStrength,
    passwordEntropyBits: Double,
    onIntent: (ResourceFormIntent) -> Unit,
) {
    Section(title = stringResource(LocalizationR.string.resource_form_password)) {
        Column {
            TextInput(
                title = stringResource(LocalizationR.string.resource_form_main_uri),
                text = mainUri,
                onTextChange = { onIntent(PasswordMainUriTextChanged(it)) },
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextInput(
                title = stringResource(LocalizationR.string.resource_form_username),
                text = username,
                onTextChange = { onIntent(PasswordUsernameTextChanged(it)) },
            )
            Spacer(modifier = Modifier.height(16.dp))
            PasswordGenerationInput(
                password = password,
                passwordStrength = passwordStrength,
                entropy = passwordEntropyBits,
                onPasswordChange = { onIntent(PasswordTextChanged(it)) },
                onGenerateClick = { onIntent(GeneratePassword) },
            )
        }
    }
}

@Composable
private fun TotpSection(
    totpSecret: String,
    totpSecretError: TotpSecretValidationError?,
    totpIssuer: String,
    onIntent: (ResourceFormIntent) -> Unit,
) {
    Section(title = stringResource(LocalizationR.string.resource_form_totp)) {
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                TextInput(
                    title = stringResource(LocalizationR.string.resource_form_totp_secret),
                    hint = stringResource(LocalizationR.string.resource_form_totp_secret),
                    text = totpSecret,
                    onTextChange = { onIntent(TotpSecretChanged(it)) },
                    state =
                        if (totpSecretError == null) {
                            Default
                        } else {
                            Error(getTotpSecretErrorMessage(LocalContext.current, totpSecretError))
                        },
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                SecondaryIconButton(
                    modifier = Modifier.size(56.dp),
                    onClick = { onIntent(ScanTotp) },
                    icon = painterResource(CoreUiR.drawable.ic_camera),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextInput(
                title = stringResource(LocalizationR.string.resource_form_totp_url_issuer),
                hint = stringResource(LocalizationR.string.resource_form_totp_url),
                text = totpIssuer,
                onTextChange = { onIntent(TotpUrlChanged(it)) },
            )
            Spacer(modifier = Modifier.height(16.dp))
            SettingRow(
                leadingIconResId = CoreUiR.drawable.ic_cog,
                text = stringResource(LocalizationR.string.resource_form_totp_more_settings),
                onClick = { onIntent(GoToTotpMoreSettings) },
            )
        }
    }
}

@Composable
private fun StandaloneNoteSection(
    note: String,
    noteError: NoteValidationError?,
    onIntent: (ResourceFormIntent) -> Unit,
) {
    Section(title = stringResource(LocalizationR.string.resource_form_note)) {
        TextInput(
            title = stringResource(LocalizationR.string.resource_form_note),
            text = note,
            onTextChange = { onIntent(NoteChanged(it)) },
            minLines = 3,
            state =
                if (noteError == null) {
                    Default
                } else {
                    Error(getNoteErrorMessage(LocalContext.current, noteError))
                },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LeadingContentPasswordPreview() {
    PassboltTheme {
        LeadingContent(
            leadingContentType = PASSWORD,
            passwordData =
                PasswordData(
                    mainUri = "https://passbolt.com",
                    username = "ada@passbolt.com",
                    password = "p@ssb0lt!",
                    passwordStrength = PasswordStrength.Strong,
                    passwordEntropyBits = 60.0,
                ),
            totpData = TotpData(),
            noteData = NoteData(),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LeadingContentTotpPreview() {
    PassboltTheme {
        LeadingContent(
            leadingContentType = TOTP,
            passwordData = PasswordData(),
            totpData =
                TotpData(
                    totpSecret = "JBSWY3DPEHPK3PXP",
                    totpIssuer = "passbolt.com",
                ),
            noteData = NoteData(),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LeadingContentNotePreview() {
    PassboltTheme {
        LeadingContent(
            leadingContentType = STANDALONE_NOTE,
            passwordData = PasswordData(),
            totpData = TotpData(),
            noteData =
                NoteData(
                    note = "This is a secret note",
                ),
            onIntent = {},
        )
    }
}
