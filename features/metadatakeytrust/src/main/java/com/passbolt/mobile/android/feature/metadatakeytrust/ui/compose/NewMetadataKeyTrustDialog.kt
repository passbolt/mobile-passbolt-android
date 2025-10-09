package com.passbolt.mobile.android.feature.metadatakeytrust.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.formatter.FingerprintFormatter
import com.passbolt.mobile.android.ui.MetadataKeyModification
import com.passbolt.mobile.android.ui.MetadataKeyModification.ROLLBACK
import com.passbolt.mobile.android.ui.MetadataKeyModification.ROTATION
import com.passbolt.mobile.android.ui.NewMetadataKeyToTrustModel
import com.passbolt.mobile.android.ui.ParsedMetadataPrivateKeyModel
import org.koin.compose.koinInject
import java.time.ZonedDateTime
import java.util.UUID
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

@Composable
fun NewMetadataKeyTrustDialog(
    newKeyToTrustModel: NewMetadataKeyToTrustModel,
    onTrustClick: (NewMetadataKeyToTrustModel) -> Unit,
    onDismiss: () -> Unit,
    fingerprintFormatter: FingerprintFormatter = koinInject(),
) {
    Dialog(
        onDismissRequest = {},
        properties =
            DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colorResource(CoreUiR.color.background),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                BackNavigationIcon(onBackClick = onDismiss)

                Column(
                    modifier =
                        Modifier
                            .padding(horizontal = 40.dp)
                            .padding(top = 64.dp)
                            .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(40.dp))

                    Text(
                        text = stringResource(LocalizationR.string.dialog_new_metadata_key_trust_changed),
                        style = MaterialTheme.typography.titleLarge,
                        color = colorResource(CoreUiR.color.text_primary),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text =
                            stringResource(
                                LocalizationR.string.dialog_new_metadata_key_trust_modified_by_user,
                                newKeyToTrustModel.signedName,
                            ),
                        style = MaterialTheme.typography.displayMedium,
                        color = colorResource(CoreUiR.color.text_secondary),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(getMessageResId(newKeyToTrustModel.modificationKind)),
                        style = MaterialTheme.typography.displayMedium,
                        color = colorResource(CoreUiR.color.text_secondary),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(LocalizationR.string.dialog_new_metadata_key_trust_key_fingerprint),
                        style = MaterialTheme.typography.displayMedium,
                        color = colorResource(CoreUiR.color.text_secondary),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(60.dp))

                    Text(
                        text =
                            fingerprintFormatter
                                .format(
                                    newKeyToTrustModel.metadataPrivateKey.fingerprint,
                                    appendMiddleSpacing = true,
                                )?.uppercase()
                                .orEmpty(),
                        style = MaterialTheme.typography.displayMedium,
                        color = colorResource(CoreUiR.color.text_primary),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(colorResource(CoreUiR.color.background)),
                ) {
                    Button(
                        shape = RoundedCornerShape(4.dp),
                        onClick = {
                            onTrustClick(newKeyToTrustModel)
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = colorResource(getButtonColor(newKeyToTrustModel.modificationKind)),
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 16.dp)
                                .padding(top = 16.dp),
                    ) {
                        Text(
                            text = stringResource(LocalizationR.string.dialog_new_metadata_key_trust_key_trust_the_key),
                        )
                    }

                    Text(
                        text = stringResource(LocalizationR.string.cancel),
                        color = colorResource(CoreUiR.color.text_primary),
                        modifier =
                            Modifier
                                .padding(16.dp)
                                .clickable { onDismiss() },
                    )
                }
            }
        }
    }
}

private fun getMessageResId(modificationKind: MetadataKeyModification): Int =
    when (modificationKind) {
        ROTATION ->
            LocalizationR.string.dialog_new_metadata_key_trust_key_rotation_info
        ROLLBACK ->
            LocalizationR.string.dialog_new_metadata_key_trust_key_rollback_info
        else -> error("Dialog should not be shown for key modification: $modificationKind")
    }

private fun getButtonColor(modificationKind: MetadataKeyModification): Int =
    when (modificationKind) {
        ROTATION -> CoreUiR.color.primary
        ROLLBACK -> CoreUiR.color.red
        else -> error("Dialog should not be shown for key modification: $modificationKind")
    }

@Preview(showBackground = true)
@Composable
private fun NewMetadataKeyTrustDialogPreview() {
    val sampleModel =
        NewMetadataKeyToTrustModel(
            id = UUID.randomUUID(),
            metadataPrivateKey =
                ParsedMetadataPrivateKeyModel(
                    id = UUID.randomUUID(),
                    userId = UUID.randomUUID(),
                    keyData = "",
                    passphrase = "",
                    created = ZonedDateTime.now(),
                    createdBy = UUID.randomUUID(),
                    modified = ZonedDateTime.now(),
                    modifiedBy = UUID.randomUUID(),
                    pgpMessage = "--- PGP MESSAGE ---",
                    fingerprint = "AAABBBCCCDDD",
                    domain = "",
                ),
            signedUsername = "john.doe@passbolt.com",
            signedName = "John Doe",
            modificationKind = ROLLBACK,
            signatureCreationTimestampSeconds = ZonedDateTime.now().toEpochSecond(),
            signatureKeyFingerprint = "AAABBBCCCDDD",
        )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorResource(CoreUiR.color.background),
    ) {
        NewMetadataKeyTrustDialog(
            newKeyToTrustModel = sampleModel,
            onTrustClick = { },
            onDismiss = { },
        )
    }
}
