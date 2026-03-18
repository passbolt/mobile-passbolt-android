package com.passbolt.mobile.android.feature.home.foldermoremenu

import PassboltTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.passbolt.mobile.android.core.ui.compose.bottomsheet.BottomSheetHeader
import com.passbolt.mobile.android.core.ui.compose.menu.OpenableSettingsItem
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FolderMoreMenuBottomSheet(
    folderName: String?,
    onDismissRequest: () -> Unit,
    onSeeDetails: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = colorResource(CoreUiR.color.elevated_background),
    ) {
        BottomSheetHeader(
            title = folderName ?: stringResource(LocalizationR.string.folder_root),
            onClose = onDismissRequest,
        )

        OpenableSettingsItem(
            title = stringResource(LocalizationR.string.folder_more_see_details),
            iconPainter = painterResource(CoreUiR.drawable.ic_zoom),
            onClick = onSeeDetails,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FolderMoreMenuBottomSheetPreview() {
    PassboltTheme {
        FolderMoreMenuBottomSheet(
            folderName = "Social Media",
            onDismissRequest = {},
            onSeeDetails = {},
        )
    }
}
