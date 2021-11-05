package com.passbolt.mobile.android.feature.resources.new

import android.os.Bundle
import android.view.View
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.ui.textinputfield.PasswordGenerateInputView
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Error
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.resources.R
import com.passbolt.mobile.android.feature.resources.databinding.FragmentNewResourceBinding
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
class NewResourceFragment : BindingScopedAuthenticatedFragment<FragmentNewResourceBinding, NewResourceContract.View>(
    FragmentNewResourceBinding::inflate
), NewResourceContract.View {

    override val presenter: NewResourceContract.Presenter by inject()
    private val viewProvider: ViewProvider by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setListeners()
        presenter.attach(this)
        presenter.viewCreated()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun setListeners() {
        binding.createButton.setDebouncingOnClick {
            presenter.createClick()
        }
    }

    override fun addTextInput(name: String) {
        val (view, params) = viewProvider.getTextInput(name, requireContext())
        binding.container.addView(view, params)
    }

    override fun addPasswordInput(name: String) {
        val (view, params) = viewProvider.getPasswordWithGeneratorInput(name, requireContext())
        view.setGenerateClickListener { presenter.passwordGenerateClick(name) }
        view.setPasswordChangeListener { presenter.passwordTextChanged(name, it) }
        binding.container.addView(view, params)
    }

    override fun showPasswordStrength(tag: String, strength: PasswordGenerateInputView.PasswordStrength) {
        (binding.container.findViewWithTag<View>(tag) as PasswordGenerateInputView).setPasswordStrength(strength)
    }

    override fun addDescriptionInput(name: String) {
        val (view, params) = viewProvider.getDescriptionInput(name, requireContext())
        binding.container.addView(view, params)
    }

    override fun addSecretInput(name: String) {
        val (view, params) = viewProvider.getSecretInput(name, requireContext())
        binding.container.addView(view, params)
    }

    override fun showEmptyValueError(tag: String) {
        (binding.container.findViewWithTag<View>(tag) as StatefulInput)
            .setState(Error(String.format(resources.getString(R.string.resource_new_empty_error), tag)))
    }

    override fun showTooLongError(tag: String) {
        (binding.container.findViewWithTag<View>(tag) as StatefulInput)
            .setState(Error(resources.getString(R.string.resource_new_too_long_error)))
    }

    override fun showPassword(tag: String, generatedPassword: String) {
        (binding.container.findViewWithTag<View>(tag) as PasswordGenerateInputView).showPassword(
            generatedPassword,
            PasswordGenerateInputView.PasswordStrength.VeryStrong
        )
    }
}
