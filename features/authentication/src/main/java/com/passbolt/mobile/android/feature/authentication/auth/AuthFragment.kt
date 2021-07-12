package com.passbolt.mobile.android.feature.authentication.auth

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.navArgs
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.snackbar.Snackbar
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.hideSoftInput
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.core.ui.progressdialog.ProgressDialog
import com.passbolt.mobile.android.feature.authentication.R
import com.passbolt.mobile.android.feature.authentication.auth.uistrategy.AuthStrategy
import com.passbolt.mobile.android.feature.authentication.auth.uistrategy.AuthStrategyFactory
import com.passbolt.mobile.android.feature.authentication.databinding.FragmentAuthBinding
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

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
class AuthFragment : BindingScopedFragment<FragmentAuthBinding>(FragmentAuthBinding::inflate), AuthContract.View {

    private val args: AuthFragmentArgs by navArgs()
    private val strategyFactory: AuthStrategyFactory by inject()

    private lateinit var authStrategy: AuthStrategy
    private lateinit var presenter: AuthContract.Presenter

    private var progressDialog = ProgressDialog()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authStrategy = strategyFactory.get(args.authenticationStrategy, this)
        presenter = get(named(args.authenticationStrategy.javaClass.simpleName))
        presenter.argsRetrieved(args.userId)
        presenter.attach(this)
        setListeners()
    }

    private fun setListeners() {
        with(binding) {
            passphraseInput.setIsEmptyListener {
                presenter.passphraseInputIsEmpty(it)
            }
            authButton.setDebouncingOnClick {
                presenter.signInClick(binding.passphraseInput.getText())
            }
            forgotPasswordButton.setDebouncingOnClick {
                presenter.forgotPasswordClick()
            }
            with(toolbar) {
                setNavigationIcon(com.passbolt.mobile.android.core.ui.R.drawable.ic_back)
                setNavigationOnClickListener { presenter.backClick() }
            }
        }
    }

    override fun onDestroyView() {
        authStrategy.detach()
        presenter.detach()
        super.onDestroyView()
    }

    override fun showTitle() {
        binding.toolbar.toolbarTitle = authStrategy.title()
    }

    override fun navigateBack() {
        authStrategy.navigateBack()
    }

    override fun authSuccess() {
        authStrategy.authSuccess()
    }

    override fun showWrongPassphrase() {
        Snackbar.make(binding.root, R.string.auth_incorrect_passphrase, Snackbar.LENGTH_LONG)
            .show()
    }

    override fun showGenericError() {
        Snackbar.make(binding.root, R.string.unknown_error, Snackbar.LENGTH_LONG)
            .show()
    }

    override fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .show()
    }

    override fun showProgress() {
        progressDialog.show(childFragmentManager, ProgressDialog::class.java.name)
    }

    override fun hideProgress() {
        progressDialog.dismiss()
    }

    override fun showName(name: String) {
        binding.nameLabel.text = name
    }

    override fun showEmail(email: String) {
        binding.emailLabel.text = email
    }

    override fun showAvatar(url: String) {
        binding.avatarImage.load(url) {
            transformations(CircleCropTransformation())
            error(R.drawable.ic_avatar_placeholder)
            placeholder(R.drawable.ic_avatar_placeholder)
        }
    }

    override fun showForgotPasswordDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.auth_forgot_password_title)
            .setMessage(R.string.auth_forgot_password_message)
            .setPositiveButton(R.string.got_it) { _, _ -> }
            .show()
    }

    override fun enableAuthButton() {
        binding.authButton.isEnabled = true
    }

    override fun disableAuthButton() {
        binding.authButton.isEnabled = false
    }

    override fun hideKeyboard() {
        hideSoftInput()
    }
}
