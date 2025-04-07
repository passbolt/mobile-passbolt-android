package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.passbolt.mobile.android.common.dialogs.unableToGeneratePasswordAlertDialog
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.core.passwordgenerator.codepoints.Codepoint
import com.passbolt.mobile.android.feature.resourceform.databinding.FragmentPasswordFormBinding
import com.passbolt.mobile.android.ui.PasswordStrength
import com.passbolt.mobile.android.ui.PasswordUiModel
import org.koin.android.ext.android.inject
import com.passbolt.mobile.android.core.localization.R as LocalizationR

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
class PasswordFormFragment :
    BindingScopedFragment<FragmentPasswordFormBinding>(
        FragmentPasswordFormBinding::inflate
    ), PasswordFormContract.View {

    private val presenter: PasswordFormContract.Presenter by inject()
    private val navArgs: PasswordFormFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(binding.toolbar)
        setListeners()
        presenter.attach(this)
        presenter.argsRetrieved(navArgs.mode, navArgs.passwordModel)
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    override fun showCreateTitle() {
        binding.toolbar.toolbarTitle = getString(LocalizationR.string.resource_form_create_password)
    }

    override fun showEditTitle(resourceName: String) {
        binding.toolbar.toolbarTitle = getString(LocalizationR.string.resource_form_edit_resource, resourceName)
    }

    private fun setListeners() {
        with(binding) {
            passwordSubformView.mainUriInput.apply {
                setTextChangeListener { presenter.passwordMainUriTextChanged(it) }
            }
            passwordSubformView.usernameInput.apply {
                setTextChangeListener { presenter.passwordUsernameTextChanged(it) }
            }
            passwordSubformView.passwordGenerateInput.apply {
                setGenerateClickListener { presenter.passwordGenerateClick() }
                setPasswordChangeListener { presenter.passwordTextChanged(it) }
            }
            apply.setDebouncingOnClick {
                presenter.applyClick()
            }
        }
    }

    override fun showPasswordUsername(username: String) {
        binding.passwordSubformView.usernameInput.text = username
    }

    override fun showPasswordMainUri(mainUri: String) {
        binding.passwordSubformView.mainUriInput.text = mainUri
    }

    override fun showPassword(password: List<Codepoint>, entropy: Double, passwordStrength: PasswordStrength) {
        val passwordStringBuilder = StringBuilder()
        password.forEach {
            passwordStringBuilder.append(Character.toChars(it.value))
        }

        binding.passwordSubformView.passwordGenerateInput.showPassword(
            passwordStringBuilder.toString(),
            passwordStrength,
            entropy
        )
    }

    override fun showPasswordStrength(passwordStrength: PasswordStrength, entropy: Double) {
        binding.passwordSubformView.passwordGenerateInput.setPasswordStrength(passwordStrength, entropy)
    }

    override fun showUnableToGeneratePassword(minimumEntropyBits: Int) {
        unableToGeneratePasswordAlertDialog(requireContext(), minimumEntropyBits).show()
    }

    override fun goBackWithResult(password: PasswordUiModel) {
        setFragmentResult(
            REQUEST_PASSWORD,
            bundleOf(EXTRA_PASSWORD to password)
        )
        findNavController().popBackStack()
    }

    companion object {
        const val REQUEST_PASSWORD = "PASSWORD"

        const val EXTRA_PASSWORD = "password"
    }
}
