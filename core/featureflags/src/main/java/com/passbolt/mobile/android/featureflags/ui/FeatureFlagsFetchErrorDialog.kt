package com.passbolt.mobile.android.featureflags.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.featureflags.R
import com.passbolt.mobile.android.featureflags.databinding.DialogFeatureFlagsFetchErrorBinding

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
class FeatureFlagsFetchErrorDialog : DialogFragment() {

    private var listener: Listener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.FullscreenDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = DialogFeatureFlagsFetchErrorBinding.inflate(inflater)
        setupListeners(binding)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        isCancelable = false
        listener = when {
            activity is Listener -> activity as Listener
            parentFragment is Listener -> parentFragment as Listener
            else -> error("Parent must implement ${Listener::class.java.name}")
        }
    }

    override fun onDetach() {
        listener = null
        super.onDetach()
    }

    private fun setupListeners(binding: DialogFeatureFlagsFetchErrorBinding) {
        with(binding) {
            refreshButton.setDebouncingOnClick {
                listener?.fetchFeatureFlagsErrorDialogRefreshClick()
            }
            signOutButton.setDebouncingOnClick {
                listener?.fetchFeatureFlagsErrorDialogSignOutClick()
            }
        }
    }

    interface Listener {
        fun fetchFeatureFlagsErrorDialogRefreshClick()
        fun fetchFeatureFlagsErrorDialogSignOutClick()
    }
}
