package com.passbolt.mobile.android.feature.setup.enterpassphrase

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.mvp.viewbinding.BindingFragment
import com.passbolt.mobile.android.feature.setup.R
import com.passbolt.mobile.android.feature.setup.databinding.FragmentEnterPassphraseBinding
import org.koin.android.ext.android.inject
import coil.load
import coil.transform.CircleCropTransformation
import com.passbolt.mobile.android.common.extension.toByteArray
import com.passbolt.mobile.android.core.extension.initDefaultToolbar

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

class EnterPassphraseFragment :
    BindingFragment<FragmentEnterPassphraseBinding>(FragmentEnterPassphraseBinding::inflate),
    EnterPassphraseContract.View {

    private val presenter: EnterPassphraseContract.Presenter by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.attach(this)
        initDefaultToolbar(binding.toolbar)
        setListeners()
    }

    private fun setListeners() {
        binding.forgotPasswordButton.setDebouncingOnClick { presenter.forgotPasswordCLick() }
        binding.password.setIsEmptyListener { presenter.passwordChanged(it) }
    }

    override fun showForgotPasswordDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.enter_passphrase_forgot_password_title)
            .setMessage(R.string.enter_passphrase_forgot_password_message)
            .setPositiveButton(R.string.got_it) { _, _ -> }
            .show()
    }

    override fun setButtonEnabled(enabled: Boolean) {
        binding.signInButton.isEnabled = enabled
    }

    override fun displayAvatar(url: String) {
        binding.avatar.load(url) {
            transformations(CircleCropTransformation())
            target {
                binding.avatar.setImageDrawable(it)
                presenter.onImageLoaded(it.toByteArray())
            }
        }
    }

    override fun displayName(name: String) {
        binding.name.text = name
    }

    override fun displayUrl(url: String) {
        binding.url.text = url
    }

    override fun displayEmail(email: String?) {
        binding.email.text = email
    }
}
