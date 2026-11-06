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

package com.passbolt.mobile.android.core.autofill.accessibility

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TrustedBrowserPackagesTest {
    private val trustedBrowserPackages = TrustedBrowserPackages()

    @Test
    fun `pinnedFingerprints returns the release fingerprint for an allow-listed browser`() {
        assertThat(trustedBrowserPackages.pinnedFingerprints("com.android.chrome"))
            .containsExactly("F0:FD:6C:5B:41:0F:25:CB:25:C3:B5:33:46:C8:97:2F:AE:30:F8:EE:74:11:DF:91:04:80:AD:6B:2D:60:DB:83")
    }

    @Test
    fun `pinnedFingerprints returns every pinned fingerprint when a browser has more than one`() {
        assertThat(trustedBrowserPackages.pinnedFingerprints("com.chrome.beta")).hasSize(2)
    }

    @Test
    fun `pinnedFingerprints is empty for a package that is not allow-listed`() {
        assertThat(trustedBrowserPackages.pinnedFingerprints("com.example.spoofer")).isEmpty()
    }

    @Test
    fun `pinnedFingerprints is empty for a null package`() {
        assertThat(trustedBrowserPackages.pinnedFingerprints(null)).isEmpty()
    }
}
