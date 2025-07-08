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
import com.passbolt.mobile.android.feature.metadatakeytrust.databinding.DialogTrustedMetadataKeyDeletedBinding
import com.passbolt.mobile.android.ui.TrustedKeyDeletedModel
import org.koin.core.component.KoinComponent
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
class NewTrustedMetadataKeyDeletedDialog :
    DialogFragment(),
    KoinComponent {
    private var listener: Listener? = null
    private val bundledDeletedKey by lifecycleAwareLazy {
        requireNotNull(
            BundleCompat.getParcelable(requireArguments(), DELETED_KEY, TrustedKeyDeletedModel::class.java),
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
        val binding = DialogTrustedMetadataKeyDeletedBinding.inflate(inflater)
        setupListeners(binding)
        isCancelable = false
        return binding.root
    }

    private fun setupListeners(binding: DialogTrustedMetadataKeyDeletedBinding) {
        with(binding) {
            trustButton.setDebouncingOnClick {
                listener?.trustMetadataKeyDeletionClick(bundledDeletedKey)
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
                parentFragment is Listener -> parentFragment as Listener
                activity is Listener -> activity as Listener
                else -> error("Parent must implement ${Listener::class.java.name}")
            }
    }

    override fun onDetach() {
        listener = null
        super.onDetach()
    }

    interface Listener {
        fun trustMetadataKeyDeletionClick(model: TrustedKeyDeletedModel)
    }

    companion object {
        private const val DELETED_KEY = "DELETED_KEY"

        fun newInstance(trustedKeyDeletedModel: TrustedKeyDeletedModel) =
            NewTrustedMetadataKeyDeletedDialog().apply {
                arguments =
                    bundleOf(
                        DELETED_KEY to trustedKeyDeletedModel,
                    )
            }
    }
}
