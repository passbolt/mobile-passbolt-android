package com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.data

import com.passbolt.mobile.android.dto.response.qrcode.AssembledKeyDto
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.data.QrGenerationConstants.MAX_QR_DATA_BYTES
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.data.QrGenerationConstants.RESERVED_BYTES_COUNT
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.storage.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.privatekey.GetSelectedUserPrivateKeyUseCase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.Buffer
import timber.log.Timber
import kotlin.math.ceil

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */
class CreateTransferInputParametersGenerator(
    private val getSelectedAccountPrivateKeyUseCase: GetSelectedUserPrivateKeyUseCase,
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val openPgp: OpenPgp
) {

    suspend fun calculateCreateTransferParameters(): Output =
        try {
            val armoredPrivateKey = requireNotNull(getSelectedAccountPrivateKeyUseCase.execute(Unit).privateKey)
            val accountData = getSelectedAccountDataUseCase.execute(Unit)
            val userServerId = requireNotNull(accountData.serverId)
            val privateKeyFingerprint = openPgp.getPrivateKeyFingerprint(armoredPrivateKey)
                    as OpenPgpResult.Result<String>
            val keyJson = Json.encodeToString(
                AssembledKeyDto(
                    armoredPrivateKey,
                    userServerId,
                    privateKeyFingerprint.result
                )
            )
            Output.Parameters(keyJson, calculateTotalPageCount(keyJson), calculateHash(keyJson))
        } catch (exception: Exception) {
            Timber.e("Could not initialize transfer parameters", exception)
            Output.Error
        }

    // max bytes in qr code of required parameters is 1465
    // subtract 3 reserved bytes per page
    // add 1 additionally for the the first initial page
    private fun calculateTotalPageCount(keyJson: String) =
        ceil(keyJson.length.toDouble() / (MAX_QR_DATA_BYTES - RESERVED_BYTES_COUNT)).toInt() + 1

    private fun calculateHash(keyJson: String) =
        Buffer()
            .apply {
                write(keyJson.toByteArray())
            }
            .sha512()
            .hex()

    sealed class Output {

        data class Parameters(
            val keyJson: String,
            val totalPagesCount: Int,
            val pagesDataHash: String
        ) : Output()

        object Error : Output()
    }
}
