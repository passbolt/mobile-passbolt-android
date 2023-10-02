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

package com.passbolt.mobile.android.feature.authentication.mfa.duo

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.google.android.material.snackbar.Snackbar
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.feature.authentication.databinding.DialogAuthWithDuoBinding
import com.passbolt.mobile.android.feature.authentication.mfa.duo.duowebviewsheet.DuoState
import com.passbolt.mobile.android.feature.authentication.mfa.duo.duowebviewsheet.DuoWebViewBottomSheetFragment
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.fragmentScope
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

class AuthWithDuoDialog : DialogFragment(), AndroidScopeComponent, AuthWithDuoContract.View,
    DuoWebViewBottomSheetFragment.Listener {

    override val scope by fragmentScope(useParentActivityScope = false)
    private var listener: AuthWithDuoListener? = null
    private val presenter: AuthWithDuoContract.Presenter by scope.inject()
    private lateinit var binding: DialogAuthWithDuoBinding
    private val authenticationResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            presenter.authenticationSucceeded()
        }
    }

    private val bundledAuthToken by lifecycleAwareLazy {
        requireArguments().getString(EXTRA_AUTH_KEY)
    }
    private val bundledHasOtherProvider by lifecycleAwareLazy {
        requireArguments().getBoolean(EXTRA_OTHER_PROVIDER)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, CoreUiR.style.FullscreenDialogTheme)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        presenter.onViewCreated(bundledHasOtherProvider, bundledAuthToken)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogAuthWithDuoBinding.inflate(inflater)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        isCancelable = false
        listener = when {
            activity is AuthWithDuoListener -> activity as AuthWithDuoListener
            parentFragment is AuthWithDuoListener -> parentFragment as AuthWithDuoListener
            else -> error("Parent must implement ${AuthWithDuoListener::class.java.name}")
        }
        presenter.attach(this)
    }

    override fun onDetach() {
        listener = null
        presenter.detach()
        super.onDetach()
    }

    override fun navigateToLogin() {
        dismiss()
        authenticationResult.launch(
            ActivityIntents.authentication(
                requireContext(),
                ActivityIntents.AuthConfig.SignIn
            )
        )
    }

    override fun showSessionExpired() {
        Toast.makeText(requireContext(), LocalizationR.string.session_expired, Toast.LENGTH_SHORT).show()
    }

    override fun navigateToDuoPrompt(duoPromptUrl: String) {
        DuoWebViewBottomSheetFragment.newInstance(duoPromptUrl)
            .show(childFragmentManager, DuoWebViewBottomSheetFragment::class.java.name)
    }

    override fun showChangeProviderButton(bundledHasTotpProvider: Boolean) {
        binding.otherProviderButton.isVisible = bundledHasTotpProvider
    }

    private fun setupListeners() {
        with(binding) {
            authWithDuoButton.setDebouncingOnClick {
                presenter.authWithDuoClick()
            }
            otherProviderButton.setDebouncingOnClick {
                listener?.duoOtherProviderClick(bundledAuthToken)
            }
            closeButton.setDebouncingOnClick {
                presenter.closeClick()
            }
        }
    }

    override fun duoAuthFinished(state: DuoState) {
        presenter.verifyDuoAuth(state)
    }

    override fun closeAndNavigateToStartup() {
        dismiss()
        startActivity(ActivityIntents.start(requireContext()))
    }

    override fun showProgress() {
        showProgressDialog(childFragmentManager)
    }

    override fun hideProgress() {
        hideProgressDialog(childFragmentManager)
    }

    override fun notifyVerificationSucceeded(mfaHeader: String) {
        dismiss()
        listener?.duoAuthSucceeded(mfaHeader)
    }

    override fun notifyLoginSucceeded() {
        dismiss()
        listener?.duoAuthSucceeded()
    }

    override fun close() {
        dismiss()
    }

    override fun showError() {
        Snackbar.make(binding.root, LocalizationR.string.unknown_error, Snackbar.LENGTH_LONG)
            .apply {
                view.setBackgroundColor(context.getColor(CoreUiR.color.red))
                show()
            }
    }

    companion object {
        private const val EXTRA_AUTH_KEY = "EXTRA_AUTH_KEY"
        private const val EXTRA_OTHER_PROVIDER = "EXTRA_OTHER_PROVIDER"

        fun newInstance(
            token: String? = null,
            hasOtherProvider: Boolean
        ) =
            AuthWithDuoDialog().apply {
                arguments = bundleOf(
                    EXTRA_AUTH_KEY to token,
                    EXTRA_OTHER_PROVIDER to hasOtherProvider
                )
            }
    }
}

interface AuthWithDuoListener {
    fun duoOtherProviderClick(jwtToken: String?)
    fun duoAuthSucceeded(mfaHeader: String? = null)
}
