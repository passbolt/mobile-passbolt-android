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

package com.passbolt.mobile.android.core.autofill.system

import android.content.pm.PackageManager
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.autofill.accessibility.TrustedBrowserPackages
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class BrowserSignatureVerifierTest {
    private val packageManager = mock<PackageManager>()
    private val verifier = BrowserSignatureVerifier(packageManager, TrustedBrowserPackages())

    @Test
    fun `isTrustedBrowser is true when an allow-listed package is signed with the pinned certificate`() {
        whenever(
            packageManager.hasSigningCertificate(eq("com.android.chrome"), any(), eq(PackageManager.CERT_INPUT_SHA256)),
        ).thenReturn(true)

        assertThat(verifier.isTrustedBrowser("com.android.chrome")).isTrue()
    }

    @Test
    fun `isTrustedBrowser is false when an allow-listed package is signed with a different certificate`() {
        whenever(
            packageManager.hasSigningCertificate(eq("com.android.chrome"), any(), eq(PackageManager.CERT_INPUT_SHA256)),
        ).thenReturn(false)

        assertThat(verifier.isTrustedBrowser("com.android.chrome")).isFalse()
    }

    @Test
    fun `isTrustedBrowser is false for a package that is not allow-listed`() {
        assertThat(verifier.isTrustedBrowser("com.example.spoofer")).isFalse()
    }

    @Test
    fun `isTrustedBrowser is false for a null package`() {
        assertThat(verifier.isTrustedBrowser(null)).isFalse()
    }

    @Test
    fun `trustedUriForCaller keeps the uri when the caller is a verified trusted browser`() {
        whenever(
            packageManager.hasSigningCertificate(eq("com.android.chrome"), any(), eq(PackageManager.CERT_INPUT_SHA256)),
        ).thenReturn(true)

        assertThat(verifier.trustedUriForCaller("https://victim.example.com", "com.android.chrome"))
            .isEqualTo("https://victim.example.com")
    }

    @Test
    fun `trustedUriForCaller drops the uri when the caller is allow-listed but signed with a different certificate`() {
        whenever(
            packageManager.hasSigningCertificate(eq("com.android.chrome"), any(), eq(PackageManager.CERT_INPUT_SHA256)),
        ).thenReturn(false)

        assertThat(verifier.trustedUriForCaller("https://victim.example.com", "com.android.chrome")).isNull()
    }

    @Test
    fun `trustedUriForCaller drops the uri for a caller that is not allow-listed`() {
        assertThat(verifier.trustedUriForCaller("https://victim.example.com", "com.example.spoofer")).isNull()
    }

    @Test
    fun `trustedUriForCaller drops the uri for a null caller`() {
        assertThat(verifier.trustedUriForCaller("https://victim.example.com", null)).isNull()
    }

    @Test
    fun `trustedUriForCaller returns null when there is no uri even for a trusted browser`() {
        whenever(
            packageManager.hasSigningCertificate(eq("com.android.chrome"), any(), eq(PackageManager.CERT_INPUT_SHA256)),
        ).thenReturn(true)

        assertThat(verifier.trustedUriForCaller(null, "com.android.chrome")).isNull()
    }
}
