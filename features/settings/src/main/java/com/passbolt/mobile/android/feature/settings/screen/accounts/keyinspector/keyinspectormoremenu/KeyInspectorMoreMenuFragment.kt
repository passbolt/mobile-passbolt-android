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

package com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedBottomSheetFragment
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.feature.settings.databinding.BottomsheetKeyInspectorMoreMenuBinding
import org.koin.android.ext.android.inject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

class KeyInspectorMoreMenuFragment :
    BindingScopedBottomSheetFragment<BottomsheetKeyInspectorMoreMenuBinding>(
        BottomsheetKeyInspectorMoreMenuBinding::inflate,
    ),
    KeyInspectorMoreMenuContract.View {
    private val presenter: KeyInspectorMoreMenuContract.Presenter by inject()
    private val authenticationResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                presenter.authenticationSucceeded()
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        presenter.attach(this)
    }

    override fun onDetach() {
        presenter.detach()
        super.onDetach()
    }

    private fun setListeners() {
        with(requiredBinding) {
            exportPrivateKey.setDebouncingOnClick {
                presenter.exportPrivateKeyClick()
            }
            exportPublicKey.setDebouncingOnClick {
                presenter.exportPublicKeyClick()
            }
            close.setDebouncingOnClick {
                dismiss()
            }
        }
    }

    override fun navigateToRefreshPassphrase() {
        authenticationResult.launch(
            ActivityIntents.authentication(
                requireContext(),
                ActivityIntents.AuthConfig.RefreshPassphrase,
            ),
        )
    }

    override fun showShareSheet(keyText: String) {
        val sendIntent: Intent =
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, keyText)
                type = TYPE_TEXT_PLAIN
            }

        val shareIntent =
            Intent.createChooser(
                sendIntent,
                getString(LocalizationR.string.key_inspector_menu_export_key),
            )
        startActivity(shareIntent)
    }

    override fun close() {
        dismiss()
    }

    override fun showFailedToGeneratePublicKey(message: String) {
        showSnackbar(
            messageResId = LocalizationR.string.key_inspector_menu_failed_to_generate_public_key,
            backgroundColor = CoreUiR.color.red,
            messageArgs = arrayOf(message),
        )
    }

    private companion object {
        private const val TYPE_TEXT_PLAIN = "text/plain"
    }
}
