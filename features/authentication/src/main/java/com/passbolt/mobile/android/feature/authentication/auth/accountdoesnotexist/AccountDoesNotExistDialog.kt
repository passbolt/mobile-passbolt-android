package com.passbolt.mobile.android.feature.authentication.auth.accountdoesnotexist

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.feature.authentication.R
import com.passbolt.mobile.android.feature.authentication.databinding.DialogAccountDoesNotExistBinding

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
class AccountDoesNotExistDialog : DialogFragment() {

    private var listener: Listener? = null
    private val bundledName by lifecycleAwareLazy {
        requireArguments().getString(EXTRA_NAME).orEmpty()
    }
    private val bundledUrl by lifecycleAwareLazy {
        requireArguments().getString(EXTRA_URL).orEmpty()
    }
    private val bundledEmail by lifecycleAwareLazy {
        requireArguments().getString(EXTRA_EMAIL).orEmpty()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.FullscreenDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DialogAccountDoesNotExistBinding.inflate(inflater)
        setupListeners(binding)
        showAccountData(binding)
        isCancelable = false
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
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

    private fun showAccountData(binding: DialogAccountDoesNotExistBinding) {
        with(binding) {
            nameLabel.text = bundledName
            emailLabel.text = bundledEmail
            urlsLabel.text = bundledUrl
        }
    }

    private fun setupListeners(binding: DialogAccountDoesNotExistBinding) {
        val exitAction = {
            listener?.connectToExistingAccountClick()
            dismiss()
        }
        with(binding) {
            closeButton.setDebouncingOnClick { exitAction.invoke() }
            connectToAccountButton.setDebouncingOnClick { exitAction.invoke() }
        }
    }

    interface Listener {
        fun connectToExistingAccountClick()
    }

    companion object {

        private const val EXTRA_NAME = "NAME"
        private const val EXTRA_EMAIL = "EMAIL"
        private const val EXTRA_URL = "URL"

        fun newInstance(name: String?, email: String?, url: String?) =
            AccountDoesNotExistDialog().apply {
                arguments = bundleOf(
                    EXTRA_NAME to name.orEmpty(),
                    EXTRA_EMAIL to email.orEmpty(),
                    EXTRA_URL to url.orEmpty()
                )
            }
    }
}
