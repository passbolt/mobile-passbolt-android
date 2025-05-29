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

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import coil.load
import coil.transform.CircleCropTransformation
import com.passbolt.mobile.android.common.types.ClipboardLabel
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.core.extension.visible
import com.passbolt.mobile.android.core.ui.labelledtext.LabelledTextEndAction
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.settings.databinding.FragmentKeyInspectorBinding
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorMoreMenuFragment
import org.koin.android.ext.android.inject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

class KeyInspectorFragment :
    BindingScopedAuthenticatedFragment<FragmentKeyInspectorBinding, KeyInspectorContract.View>(
        FragmentKeyInspectorBinding::inflate,
    ),
    KeyInspectorContract.View {
    override val presenter: KeyInspectorContract.Presenter by inject()
    private val clipboardManager: ClipboardManager? by inject()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(requiredBinding.toolbar)
        setListeners()
        presenter.attach(this)
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    private fun setListeners() {
        with(requiredBinding) {
            uidLabeledText.endActionButton =
                LabelledTextEndAction(CoreUiR.drawable.ic_copy) {
                    presenter.uidCopyClick()
                }
            fingerprintLabeledText.endActionButton =
                LabelledTextEndAction(CoreUiR.drawable.ic_copy) {
                    presenter.fingerprintCopyClick()
                }
            moreButton.setDebouncingOnClick {
                navigateToKeyInspectorMoreMenu()
            }
        }
    }

    private fun navigateToKeyInspectorMoreMenu() {
        KeyInspectorMoreMenuFragment()
            .show(childFragmentManager, KeyInspectorMoreMenuFragment::class.java.name)
    }

    override fun showProgress() {
        showProgressDialog(childFragmentManager)
    }

    override fun hideProgress() {
        hideProgressDialog(childFragmentManager)
    }

    override fun showError(message: String?) {
        showSnackbar(
            messageResId = LocalizationR.string.key_inspector_error_during_key_fetch,
            backgroundColor = CoreUiR.color.red,
            messageArgs = arrayOf(message.orEmpty()),
        )
    }

    override fun showUid(uid: String) {
        with(requiredBinding.uidLabeledText) {
            visible()
            text = uid
        }
    }

    override fun showFingerprint(fingerprint: String) {
        requiredBinding.fingerprintLabeledText.text = fingerprint
    }

    override fun showCreationDate(keyCreationDate: String) {
        with(requiredBinding.createdLabeledText) {
            visible()
            text = keyCreationDate
        }
    }

    override fun showExpirationDate(keyExpirationDate: String) {
        with(requiredBinding.expiresLabeledText) {
            visible()
            text = keyExpirationDate
        }
    }

    override fun showLength(bits: String) {
        requiredBinding.lengthLabeledText.text = bits
    }

    override fun showAlgorithm(algorithm: String) {
        with(requiredBinding.algorithmLabeledText) {
            visible()
            text = algorithm
        }
    }

    override fun showAvatar(avatarUrl: String?) {
        requiredBinding.avatarImage.load(avatarUrl) {
            error(CoreUiR.drawable.ic_avatar_placeholder)
            transformations(CircleCropTransformation())
            placeholder(CoreUiR.drawable.ic_avatar_placeholder)
        }
    }

    override fun showLabel(label: String) {
        requiredBinding.label.text = label
    }

    override fun addToClipboard(
        clipboardLabel: ClipboardLabel,
        value: String,
    ) {
        clipboardManager?.setPrimaryClip(
            ClipData.newPlainText(clipboardLabel, value),
        )
        Toast
            .makeText(
                requireContext(),
                getString(LocalizationR.string.copied_info, clipboardLabel),
                Toast.LENGTH_SHORT,
            ).show()
    }
}
