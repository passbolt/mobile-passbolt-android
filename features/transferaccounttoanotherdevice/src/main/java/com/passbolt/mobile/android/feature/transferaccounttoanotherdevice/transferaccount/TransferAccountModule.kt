package com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount

import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.data.CreateTransferInputParametersGenerator
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.data.TransferQrCodesDataGenerator
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named

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

internal const val QR_CODE_GEN_HINTS = "QR_CODE_GEN_HINTS"

private val QR_CODE_GEN_ERROR_CORRECTION = ErrorCorrectionLevel.L
private const val QR_CODE_GEN_QR_VERSION = 27
private const val QR_CODE_GEN_MARGIN_PX = 4

fun Module.transferAccountModule() {
    singleOf(::BarcodeEncoder)
    singleOf(::CreateTransferInputParametersGenerator)
    singleOf(::TransferQrCodesDataGenerator)
    single(named(QR_CODE_GEN_HINTS)) {
        mapOf<EncodeHintType, Any>(
            EncodeHintType.ERROR_CORRECTION to QR_CODE_GEN_ERROR_CORRECTION,
            EncodeHintType.QR_VERSION to QR_CODE_GEN_QR_VERSION,
            EncodeHintType.MARGIN to QR_CODE_GEN_MARGIN_PX,
        )
    }
    viewModelOf(::TransferAccountViewModel)
}
