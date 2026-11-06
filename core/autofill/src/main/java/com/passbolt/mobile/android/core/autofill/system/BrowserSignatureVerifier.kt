package com.passbolt.mobile.android.core.autofill.system

import android.content.pm.PackageManager
import com.passbolt.mobile.android.core.autofill.accessibility.TrustedBrowserPackages
import timber.log.Timber

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
class BrowserSignatureVerifier(
    private val packageManager: PackageManager,
    private val trustedBrowserPackages: TrustedBrowserPackages,
) {
    fun isTrustedBrowser(packageName: String?): Boolean {
        if (packageName == null) {
            return false
        }
        val pinnedFingerprints = trustedBrowserPackages.pinnedFingerprints(packageName)
        if (pinnedFingerprints.isEmpty()) {
            return false
        }
        val matches =
            pinnedFingerprints.any { fingerprint ->
                runCatching {
                    packageManager.hasSigningCertificate(
                        packageName,
                        fingerprint.sha256HexToBytes(),
                        PackageManager.CERT_INPUT_SHA256,
                    )
                }.getOrElse {
                    Timber.e(it, "Failed to verify signing certificate for a trusted browser candidate")
                    false
                }
            }
        if (!matches) {
            Timber.d("Requesting package is allow-listed but its signing certificate does not match the pinned fingerprint")
        }
        return matches
    }

    fun trustedUriForCaller(
        uri: String?,
        callingPackage: String?,
    ): String? = uri?.takeIf { isTrustedBrowser(callingPackage) }

    private fun String.sha256HexToBytes(): ByteArray =
        replace(":", "")
            .chunked(2)
            .map { it.toInt(HEX_RADIX).toByte() }
            .toByteArray()

    private companion object {
        private const val HEX_RADIX = 16
    }
}
