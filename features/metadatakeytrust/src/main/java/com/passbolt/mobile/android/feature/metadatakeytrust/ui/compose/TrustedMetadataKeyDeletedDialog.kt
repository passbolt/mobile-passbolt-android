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
import com.passbolt.mobile.android.ui.MetadataKeyModification
import com.passbolt.mobile.android.ui.TrustedKeyDeletedModel
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
fun TrustedMetadataKeyDeletedDialog(
    trustedKeyDeletedModel: TrustedKeyDeletedModel,
    onTrustClick: (TrustedKeyDeletedModel) -> Unit,
    onDismiss: () -> Unit,
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
                        text = stringResource(LocalizationR.string.dialog_trusted_metadata_key_deleted),
                        style = MaterialTheme.typography.titleLarge,
                        color = colorResource(CoreUiR.color.text_primary),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(LocalizationR.string.dialog_trusted_metadata_key_deleted_main),
                        style = MaterialTheme.typography.displayMedium,
                        color = colorResource(CoreUiR.color.text_secondary),
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp),
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = stringResource(LocalizationR.string.dialog_trusted_metadata_key_deleted_info),
                        style = MaterialTheme.typography.displayMedium,
                        color = colorResource(CoreUiR.color.text_secondary),
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
                            onTrustClick(trustedKeyDeletedModel)
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = colorResource(CoreUiR.color.red),
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 16.dp)
                                .padding(top = 16.dp),
                    ) {
                        Text(
                            text = stringResource(LocalizationR.string.dialog_trusted_metadata_key_deleted_trust),
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

@Preview(showBackground = true)
@Composable
private fun TrustedMetadataKeyDeletedDialogPreview() {
    val sampleModel =
        TrustedKeyDeletedModel(
            keyFingerprint = "1234 5678 9ABC DEF0",
            signedUsername = "john.doe@passbolt.com",
            signedName = "John Doe",
            modificationKind = MetadataKeyModification.DELETION,
        )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorResource(CoreUiR.color.background),
    ) {
        TrustedMetadataKeyDeletedDialog(
            trustedKeyDeletedModel = sampleModel,
            onTrustClick = { },
            onDismiss = { },
        )
    }
}
