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

package com.passbolt.mobile.android.feature.setup.scanqr

import com.passbolt.mobile.android.core.navigation.AccountSetupDataModel
import com.passbolt.mobile.android.core.qrscan.analyzer.BarcodeScanResult
import kotlinx.coroutines.flow.StateFlow

sealed interface ScanQrIntent {
    data class Initialize(
        val barcodeScanFlow: StateFlow<BarcodeScanResult>,
        val accountSetupDataModel: AccountSetupDataModel? = null,
    ) : ScanQrIntent

    data object GoBack : ScanQrIntent

    data object OpenHelpMenu : ScanQrIntent

    data object DismissHelpMenu : ScanQrIntent

    data object ConfirmSetupLeave : ScanQrIntent

    data object DismissSetupLeave : ScanQrIntent

    data object ImportProfileManually : ScanQrIntent

    data object AccessLogs : ScanQrIntent

    data class StartCameraError(
        val exception: Exception,
    ) : ScanQrIntent

    data class SelectedAccountKit(
        val accountKit: String,
    ) : ScanQrIntent

    data object DismissServerNotReachable : ScanQrIntent
}
