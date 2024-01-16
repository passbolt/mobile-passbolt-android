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

package com.passbolt.mobile.android.core.accounts

import android.util.Base64
import com.passbolt.mobile.android.core.navigation.AccountSetupDataModel
import com.passbolt.mobile.android.dto.response.AccountKitDto
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import kotlinx.serialization.json.Json
import timber.log.Timber

class AccountKitParser(
    private val openPgp: OpenPgp,
    private val json: Json
) {

    suspend fun parseAndVerify(
        accountKitFileContent: String,
        onSuccess: (AccountSetupDataModel) -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val pgpMessage = Base64.decode(accountKitFileContent, Base64.DEFAULT)
            parseAccountKit(pgpMessage, onFailure, onSuccess)
        } catch (e: IllegalArgumentException) {
            parseAndVerifyError("File does not seem to be the account kit", onFailure)
        }
    }

    private suspend fun parseAccountKit(
        pgpMessage: ByteArray,
        onFailure: (String) -> Unit,
        onSuccess: (AccountSetupDataModel) -> Unit
    ) {
        when (val unarmored = openPgp.unarmor(pgpMessage)) {
            is OpenPgpResult.Error -> onFailure(unarmored.error.message)
            is OpenPgpResult.Result -> {
                JSON_REG_EX.find(unarmored.result)?.value?.let { accountKitJson ->
                    try {
                        val accountKitDto = json.decodeFromString<AccountKitDto>(accountKitJson)
                        verifyAccountKit(
                            pgpMessage = pgpMessage,
                            hardcodedVerifiedMessage = accountKitJson,
                            armoredPublicKey = accountKitDto.publicKeyArmored,
                            onSuccess = onSuccess,
                            onFailure = onFailure
                        )
                    } catch (e: Exception) {
                        parseAndVerifyError("Could not parse the account kit: error during parsing JSON", onFailure)
                    }
                } ?: run {
                    parseAndVerifyError("Could not parse the account kit: data JSON not found in file", onFailure)
                }
            }
        }
    }

    private suspend fun verifyAccountKit(
        pgpMessage: ByteArray,
        hardcodedVerifiedMessage: String,
        armoredPublicKey: String,
        onSuccess: (AccountSetupDataModel) -> Unit,
        onFailure: (String) -> Unit
    ) {
        when (val verificationResult =
            openPgp.verifyBinarySignature(
                armoredPublicKey = armoredPublicKey,
                pgpMessage = pgpMessage,
                hardcodedVerifiedMessage = hardcodedVerifiedMessage
            )) {
            is OpenPgpResult.Error -> parseAndVerifyError(verificationResult.error.message, onFailure)
            is OpenPgpResult.Result -> {
                if (verificationResult.result.isSignatureVerified) {
                    Timber.d("Signature verification skipped")
                    json.decodeFromString<AccountKitDto>(verificationResult.result.message).apply {
                        onSuccess(
                            AccountSetupDataModel(
                                serverUserId = userId.toString(),
                                domain = domain,
                                userName = username,
                                firstName = firstName,
                                lastName = lastName,
                                avatarUrl = null,
                                keyFingerprint = verificationResult.result.keyFingerprint,
                                armoredKey = privateKeyArmored
                            )
                        )
                    }
                } else {
                    parseAndVerifyError("Signature is invalid", onFailure)
                }
            }
        }
    }

    private fun parseAndVerifyError(message: String, onFailureCallback: (String) -> Unit) {
        Timber.e(message)
        onFailureCallback(message)
    }

    private companion object {
        private val JSON_REG_EX = Regex("\\{.*\\}\\}")
    }
}
