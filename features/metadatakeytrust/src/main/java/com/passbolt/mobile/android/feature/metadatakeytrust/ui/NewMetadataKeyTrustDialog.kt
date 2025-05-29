package com.passbolt.mobile.android.feature.metadatakeytrust.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.ui.formatter.FingerprintFormatter
import com.passbolt.mobile.android.feature.metadatakeytrust.databinding.DialogNewMetadataKeyTrustBinding
import com.passbolt.mobile.android.ui.MetadataKeyModification
import com.passbolt.mobile.android.ui.NewMetadataKeyToTrustModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

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
class NewMetadataKeyTrustDialog :
    DialogFragment(),
    KoinComponent {
    private var listener: Listener? = null
    private val fingerprintFormatter: FingerprintFormatter by inject()
    private val bundledNewKeyToTrust by lifecycleAwareLazy {
        requireNotNull(
            BundleCompat.getParcelable(requireArguments(), NEW_KEY_TO_TRUST, NewMetadataKeyToTrustModel::class.java),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, CoreUiR.style.FullscreenDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = DialogNewMetadataKeyTrustBinding.inflate(inflater)
        setupListeners(binding)
        showValues(binding)
        isCancelable = false
        return binding.root
    }

    private fun showValues(binding: DialogNewMetadataKeyTrustBinding) {
        fingerprintFormatter.format(bundledNewKeyToTrust.signatureKeyFingerprint, appendMiddleSpacing = true)?.let {
            binding.fingerprint.text = it.uppercase()
        }
        binding.message1.text =
            getString(
                LocalizationR.string.dialog_new_metadata_key_trust_modified_by_user,
                bundledNewKeyToTrust.signedName,
            )
        when (bundledNewKeyToTrust.modificationKind) {
            MetadataKeyModification.ROTATION -> {
                binding.message2.text = getString(LocalizationR.string.dialog_new_metadata_key_trust_key_rotation_info)
                binding.trustButton.setBackgroundColor(requireContext().getColor(CoreUiR.color.primary))
            }
            MetadataKeyModification.ROLLBACK -> {
                binding.message2.text = getString(LocalizationR.string.dialog_new_metadata_key_trust_key_rollback_info)
                binding.trustButton.setBackgroundColor(requireContext().getColor(CoreUiR.color.red))
            }
            else -> {
                Timber.e("Dialog should not be shown for key modification: ${bundledNewKeyToTrust.modificationKind}")
            }
        }
    }

    private fun setupListeners(binding: DialogNewMetadataKeyTrustBinding) {
        with(binding) {
            trustButton.setDebouncingOnClick {
                listener?.trustNewMetadataKeyClick(bundledNewKeyToTrust)
                dismiss()
            }
            backArrow.setDebouncingOnClick { dismiss() }
            cancelButton.setDebouncingOnClick { dismiss() }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener =
            when {
                activity is Listener -> activity as Listener
                parentFragment is Listener -> parentFragment as Listener
                else -> error("Parent must implement ${Listener::class.java.name}")
            }
    }

    override fun onDetach() {
        listener = null
        super.onDetach()
    }

    interface Listener {
        fun trustNewMetadataKeyClick(newKeyToTrust: NewMetadataKeyToTrustModel)
    }

    companion object {
        private const val NEW_KEY_TO_TRUST = "NEW_KEY_TO_TRUST"

        fun newInstance(newMetadataKeyToTrust: NewMetadataKeyToTrustModel) =
            NewMetadataKeyTrustDialog().apply {
                arguments =
                    bundleOf(
                        NEW_KEY_TO_TRUST to newMetadataKeyToTrust,
                    )
            }
    }
}
