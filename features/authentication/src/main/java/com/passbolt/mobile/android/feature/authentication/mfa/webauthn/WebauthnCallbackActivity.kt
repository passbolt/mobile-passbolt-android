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

package com.passbolt.mobile.android.feature.authentication.mfa.webauthn

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Exported receiver for the `passbolt://mfa/webauthn/callback` redirect fired by the browser at the end
 * of the passkey MFA ceremony (Custom Tab and plain browser paths only; Auth Tab returns its result
 * directly to [WebauthnCeremonyActivity]).
 *
 * It does not process the URI. It restarts [WebauthnCeremonyActivity], which is already in the back
 * stack below the browser tab, with the URI as data and CLEAR_TOP | SINGLE_TOP. That removes the tab and
 * this activity, and hands the URI to the intermediary via onNewIntent.
 */
class WebauthnCallbackActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            Intent(this, WebauthnCeremonyActivity::class.java)
                .setData(intent.data)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }
}
