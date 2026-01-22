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
package com.passbolt.mobile.android.feature.setup.fingerprint

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.passbolt.mobile.android.feature.autofill.encourage.autofill.EncourageAutofillServiceDialog

/*
 * Compose implementation of FingerprintSetup navigation that works with FragmentManager
 * This allows displaying dialogs (not yet refactored) from a Composable context
 */
class ComposeFingerprintNavigationBridge(
    private val activity: FragmentActivity,
    private val onAutofillSetupClosed: () -> Unit,
    private val onAutofillSetupSuccessfully: () -> Unit,
) : FingerprintSetupNavigation {
    override fun showEncourageAutofillDialog() {
        val dialog = EncourageAutofillServiceDialog()

        dialog.show(
            activity.supportFragmentManager,
            AUTOFILL_DIALOG_TAG,
        )

        activity.supportFragmentManager.executePendingTransactions()

        dialog.listener =
            object : EncourageAutofillServiceDialog.Listener {
                override fun autofillSetupClosed() {
                    onAutofillSetupClosed()
                }

                override fun autofillSetupSuccessfully() {
                    onAutofillSetupSuccessfully()
                }
            }
    }

    companion object {
        private const val AUTOFILL_DIALOG_TAG = "EncourageAutofillServiceDialog"

        @Composable
        fun rememberFingerprintNavigation(
            onAutofillSetupClose: () -> Unit,
            onAutofillSetupSuccessfully: () -> Unit,
        ): ComposeFingerprintNavigationBridge {
            val context = LocalContext.current
            val activity =
                context as? FragmentActivity
                    ?: error("Currently FragmentActivity is needed as parent to show fingerprint setup dialogs.")

            return ComposeFingerprintNavigationBridge(
                activity = activity,
                onAutofillSetupClosed = onAutofillSetupClose,
                onAutofillSetupSuccessfully = onAutofillSetupSuccessfully,
            )
        }
    }
}

interface FingerprintSetupNavigation {
    fun showEncourageAutofillDialog()
}
