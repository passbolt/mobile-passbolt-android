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

package com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector

import com.passbolt.mobile.android.common.types.ClipboardLabel
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedContract

interface KeyInspectorContract {
    interface View : BaseAuthenticatedContract.View {
        fun showProgress()

        fun hideProgress()

        fun showError(message: String?)

        fun showUid(uid: String)

        fun showFingerprint(fingerprint: String)

        fun showCreationDate(keyCreationDate: String)

        fun showExpirationDate(keyExpirationDate: String)

        fun showLength(bits: String)

        fun showAlgorithm(algorithm: String)

        fun showAvatar(avatarUrl: String?)

        fun showLabel(label: String)

        fun addToClipboard(
            clipboardLabel: ClipboardLabel,
            value: String,
        )
    }

    interface Presenter : BaseAuthenticatedContract.Presenter<View> {
        fun uidCopyClick()

        fun fingerprintCopyClick()
    }
}
