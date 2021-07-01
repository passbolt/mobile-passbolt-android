package com.passbolt.mobile.android.feature.home.screen.more

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.passbolt.mobile.android.common.WebsiteOpener
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.feature.home.R
import com.passbolt.mobile.android.feature.home.databinding.ViewPasswordBottomsheetBinding
import org.koin.android.ext.android.inject

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
class PasswordMenuFragment : BottomSheetDialogFragment() {

    private val passwordArgs: PasswordMenuFragmentArgs by navArgs()
    private val clipboardManager: ClipboardManager? by inject()
    private val websiteOpener: WebsiteOpener by inject()
    private lateinit var binding: ViewPasswordBottomsheetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ViewPasswordBottomsheetBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        binding.title.text = passwordArgs.passwordModel.title
    }

    private fun setListeners() {
        with(binding) {
            copyPassword.setDebouncingOnClick {
                addToClipboard(PASSWORD_LABEL, passwordArgs.passwordModel.password)
            }
            copyUrl.setDebouncingOnClick {
                addToClipboard(URL_LABEL, passwordArgs.passwordModel.url)
            }
            copyUsername.setDebouncingOnClick {
                addToClipboard(USERNAME_LABEL, passwordArgs.passwordModel.username)
            }
            launchWebsite.setDebouncingOnClick {
                websiteOpener.open(requireContext(), passwordArgs.passwordModel.url)
            }
            close.setDebouncingOnClick {
                dismiss()
            }
        }
    }

    private fun addToClipboard(label: String, value: String) {
        clipboardManager?.setPrimaryClip(
            ClipData.newPlainText(label, value)
        )
        Toast.makeText(requireContext(), getString(R.string.copied_info, label), Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val PASSWORD_LABEL = "Password"
        private const val USERNAME_LABEL = "Username"
        private const val URL_LABEL = "Url"
    }
}
