package com.passbolt.mobile.android.core.autofill.accessibility

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
class TrustedBrowserPackages {
    val browsersWithUrlBar =
        listOf(
            Browser("com.android.browser", "url"),
            Browser("com.android.chrome", "url_bar"),
            Browser("org.mozilla.firefox", "mozac_browser_toolbar_url_view"),
            Browser("org.mozilla.firefox_beta", "mozac_browser_toolbar_url_view"),
            Browser("com.opera.browser", "url_field"),
            Browser("com.opera.browser.beta", "url_field"),
            Browser("com.opera.mini.native", "url_field"),
            Browser("com.opera.mini.native.beta", "url_field"),
            Browser("com.brave.browser", "url_bar"),
            Browser("com.brave.browser_beta", "url_bar"),
            Browser("com.brave.browser_nightly", "url_bar"),
            Browser("com.chrome.beta", "url_bar"),
            Browser("com.chrome.canary", "url_bar"),
            Browser("com.chrome.dev", "url_bar"),
        )

    // Release signing certificate SHA-256 fingerprints of trusted browsers.
    // Fetched 2026-11-05 from the Google Password Manager privileged-apps allowlist and
    // vendored at core/autofill/reference/gpm-passkeys-privileged-apps.json
    // Allowlist file: https://www.gstatic.com/gpm-passkeys-privileged-apps/apps.json
    // Privileged apps reference: https://developer.android.com/identity/sign-in/privileged-apps
    private val pinnedCertificates =
        mapOf(
            "com.android.browser" to
                setOf(
                    "C9:00:9D:01:EB:F9:F5:D0:30:2B:C7:1B:2F:E9:AA:9A:47:A4:32:BB:A1:73:08:A3:11:1B:75:D7:B2:14:90:25",
                ),
            "com.android.chrome" to
                setOf(
                    "F0:FD:6C:5B:41:0F:25:CB:25:C3:B5:33:46:C8:97:2F:AE:30:F8:EE:74:11:DF:91:04:80:AD:6B:2D:60:DB:83",
                ),
            "com.chrome.beta" to
                setOf(
                    "DA:63:3D:34:B6:9E:63:AE:21:03:B4:9D:53:CE:05:2F:C5:F7:F3:C5:3A:AB:94:FD:C2:A2:08:BD:FD:14:24:9C",
                    "3D:7A:12:23:01:9A:A3:9D:9E:A0:E3:43:6A:B7:C0:89:6B:FB:4F:B6:79:F4:DE:5F:E7:C2:3F:32:6C:8F:99:4A",
                ),
            "com.chrome.canary" to
                setOf(
                    "20:19:DF:A1:FB:23:EF:BF:70:C5:BC:D1:44:3C:5B:EA:B0:4F:3F:2F:F4:36:6E:9A:C1:E3:45:76:39:A2:4C:FC",
                ),
            "com.chrome.dev" to
                setOf(
                    "90:44:EE:5F:EE:4B:BC:5E:21:DD:44:66:54:31:C4:EB:1F:1F:71:A3:27:16:A0:BC:92:7B:CB:B3:92:33:CA:BF",
                    "3D:7A:12:23:01:9A:A3:9D:9E:A0:E3:43:6A:B7:C0:89:6B:FB:4F:B6:79:F4:DE:5F:E7:C2:3F:32:6C:8F:99:4A",
                ),
            "org.chromium.chrome" to
                setOf(
                    "C6:AD:B8:B8:3C:6D:4C:17:D2:92:AF:DE:56:FD:48:8A:51:D3:16:FF:8F:2C:11:C5:41:02:23:BF:F8:A7:DB:B3",
                ),
            "org.mozilla.firefox" to
                setOf(
                    "A7:8B:62:A5:16:5B:44:94:B2:FE:AD:9E:76:A2:80:D2:2D:93:7F:EE:62:51:AE:CE:59:94:46:B2:EA:31:9B:04",
                ),
            "org.mozilla.firefox_beta" to
                setOf(
                    "A7:8B:62:A5:16:5B:44:94:B2:FE:AD:9E:76:A2:80:D2:2D:93:7F:EE:62:51:AE:CE:59:94:46:B2:EA:31:9B:04",
                ),
            "org.mozilla.fennec_aurora" to
                setOf(
                    "BC:04:88:83:8D:06:F4:CA:6B:F3:23:86:DA:AB:0D:D8:EB:CF:3E:77:30:78:74:59:F6:2F:B3:CD:14:A1:BA:AA",
                ),
            "org.mozilla.fenix" to
                setOf(
                    "50:04:77:90:88:E7:F9:88:D5:BC:5C:C5:F8:79:8F:EB:F4:F8:CD:08:4A:1B:2A:46:EF:D4:C8:EE:4A:EA:F2:11",
                ),
            "org.mozilla.focus" to
                setOf(
                    "62:03:A4:73:BE:36:D6:4E:E3:7F:87:FA:50:0E:DB:C7:9E:AB:93:06:10:AB:9B:9F:A4:CA:7D:5C:1F:1B:4F:FC",
                ),
            "com.brave.browser" to
                setOf(
                    "9C:2D:B7:05:13:51:5F:DB:FB:BC:58:5B:3E:DF:3D:71:23:D4:DC:67:C9:4F:FD:30:63:61:C1:D7:9B:BF:18:AC",
                ),
            "com.brave.browser_beta" to
                setOf(
                    "9C:2D:B7:05:13:51:5F:DB:FB:BC:58:5B:3E:DF:3D:71:23:D4:DC:67:C9:4F:FD:30:63:61:C1:D7:9B:BF:18:AC",
                ),
            "com.brave.browser_nightly" to
                setOf(
                    "9C:2D:B7:05:13:51:5F:DB:FB:BC:58:5B:3E:DF:3D:71:23:D4:DC:67:C9:4F:FD:30:63:61:C1:D7:9B:BF:18:AC",
                ),
            "com.opera.browser" to
                setOf(
                    "5D:6A:FB:F8:7F:65:2A:F0:46:47:AD:A0:DF:63:4C:F2:23:70:90:0B:16:4B:09:D5:0B:D2:3A:A2:CB:52:85:B8",
                ),
            "com.opera.browser.beta" to
                setOf(
                    "5D:6A:FB:F8:7F:65:2A:F0:46:47:AD:A0:DF:63:4C:F2:23:70:90:0B:16:4B:09:D5:0B:D2:3A:A2:CB:52:85:B8",
                ),
            "com.opera.mini.native" to
                setOf(
                    "57:AC:BC:52:5F:1B:2E:BD:19:19:6C:D6:F0:14:39:7C:C9:10:FD:18:84:1E:0A:E8:50:FE:BC:3E:1E:59:3F:F2",
                ),
            "com.opera.mini.native.beta" to
                setOf(
                    "57:AC:BC:52:5F:1B:2E:BD:19:19:6C:D6:F0:14:39:7C:C9:10:FD:18:84:1E:0A:E8:50:FE:BC:3E:1E:59:3F:F2",
                ),
            "com.microsoft.emmx" to
                setOf(
                    "01:E1:99:97:10:A8:2C:27:49:B4:D5:0C:44:5D:C8:5D:67:0B:61:36:08:9D:0A:76:6A:73:82:7C:82:A1:EA:C9",
                ),
            "com.microsoft.emmx.beta" to
                setOf(
                    "01:E1:99:97:10:A8:2C:27:49:B4:D5:0C:44:5D:C8:5D:67:0B:61:36:08:9D:0A:76:6A:73:82:7C:82:A1:EA:C9",
                ),
            "com.microsoft.emmx.canary" to
                setOf(
                    "01:E1:99:97:10:A8:2C:27:49:B4:D5:0C:44:5D:C8:5D:67:0B:61:36:08:9D:0A:76:6A:73:82:7C:82:A1:EA:C9",
                ),
            "com.microsoft.emmx.dev" to
                setOf(
                    "01:E1:99:97:10:A8:2C:27:49:B4:D5:0C:44:5D:C8:5D:67:0B:61:36:08:9D:0A:76:6A:73:82:7C:82:A1:EA:C9",
                ),
            "com.sec.android.app.sbrowser" to
                setOf(
                    "34:DF:0E:7A:9F:1C:F1:89:2E:45:C0:56:B4:97:3C:D8:1C:CF:14:8A:40:50:D1:1A:EA:4A:C5:A6:5F:90:0A:42",
                ),
            "com.sec.android.app.sbrowser.beta" to
                setOf(
                    "34:DF:0E:7A:9F:1C:F1:89:2E:45:C0:56:B4:97:3C:D8:1C:CF:14:8A:40:50:D1:1A:EA:4A:C5:A6:5F:90:0A:42",
                ),
            // Tor Browser is not in Google's list. Obtained manually by pulling the Google Play
            // builds and reading the signing certificate via adb (apksigner verify --print-certs).
            "org.torproject.torbrowser" to
                setOf(
                    "20:06:1F:04:5E:73:7C:67:37:5C:17:79:4C:FE:DB:43:6A:03:CE:C6:BA:CB:7C:B9:F9:66:42:20:5C:A2:CE:C8",
                ),
            "org.torproject.torbrowser_alpha" to
                setOf(
                    "15:F7:60:B4:1A:CB:E4:78:3E:66:71:02:C9:F6:71:19:BE:2A:F6:2F:AB:07:76:3F:9D:57:F0:1E:5E:10:74:E1",
                ),
        )

    fun pinnedFingerprints(packageName: String?): Set<String> = packageName?.let { pinnedCertificates[it] }.orEmpty()
}
