package com.passbolt.mobile.android.feature.authentication.mfa.totp

import android.app.Activity
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.google.android.material.snackbar.Snackbar
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.feature.authentication.databinding.DialogEnterTotpBinding
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.fragmentScope
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

class EnterTotpDialog :
    DialogFragment(),
    AndroidScopeComponent,
    EnterTotpContract.View {
    override val scope by fragmentScope(useParentActivityScope = false)
    private var listener: EnterTotpListener? = null
    val presenter: EnterTotpContract.Presenter by scope.inject()
    private val clipboardManager: ClipboardManager? by inject()
    private lateinit var binding: DialogEnterTotpBinding
    private val bundledAuthToken by lifecycleAwareLazy {
        requireArguments().getString(EXTRA_AUTH_KEY).orEmpty()
    }
    private val authenticationResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                presenter.authenticationSucceeded()
            }
        }
    private val bundledHasYubikeyProvider by lifecycleAwareLazy {
        requireArguments().getBoolean(EXTRA_OTHER_PROVIDER)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, CoreUiR.style.FullscreenDialogTheme)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        binding.otpInput.isFocusableInTouchMode = true
        binding.otpInput.requestFocus()
        presenter.viewCreated(bundledHasYubikeyProvider)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DialogEnterTotpBinding.inflate(inflater)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        isCancelable = false
        listener =
            when {
                activity is EnterTotpListener -> activity as EnterTotpListener
                parentFragment is EnterTotpListener -> parentFragment as EnterTotpListener
                else -> error("Parent must implement ${EnterTotpListener::class.java.name}")
            }
        presenter.attach(this)
    }

    override fun onDetach() {
        listener = null
        presenter.detach()
        super.onDetach()
    }

    override fun showChangeProviderButton(hasYubikeyProvider: Boolean) {
        binding.otherProviderButton.isVisible = hasYubikeyProvider
    }

    private fun setupListeners() {
        with(binding) {
            otherProviderButton.setDebouncingOnClick {
                listener?.totpOtherProviderClick(bundledAuthToken)
            }
            closeButton.setDebouncingOnClick {
                presenter.closeClick()
            }
            pasteCodeButton.setDebouncingOnClick {
                presenter.pasteButtonClick(getPasteData())
            }
            otpInput.setOnPinEnteredListener {
                presenter.otpEntered(it.toString(), bundledAuthToken, rememberMeCheckBox.isChecked)
            }
        }
    }

    private fun getPasteData() =
        clipboardManager?.let {
            if (it.hasPrimaryClip() && it.primaryClipDescription?.hasMimeType(MIMETYPE_TEXT_PLAIN) == true) {
                it.primaryClip?.getItemAt(0)?.text
            } else {
                return null
            }
        }

    override fun showError() {
        Snackbar
            .make(binding.root, LocalizationR.string.unknown_error, Snackbar.LENGTH_SHORT)
            .apply {
                view.setBackgroundColor(context.getColor(CoreUiR.color.red))
                show()
            }
    }

    override fun showNetworkError() {
        Snackbar
            .make(binding.root, LocalizationR.string.common_network_failure, Snackbar.LENGTH_SHORT)
            .apply {
                view.setBackgroundColor(context.getColor(CoreUiR.color.red))
                show()
            }
    }

    override fun notifyLoginSucceeded() {
        dismiss()
        listener?.totpVerificationSucceeded()
    }

    override fun close() {
        dismiss()
    }

    override fun clearInput() {
        binding.otpInput.setText("")
    }

    override fun setTotpInputRed() {
        binding.otpInput.setCustomTextColor(ContextCompat.getColor(binding.root.context, CoreUiR.color.red))
    }

    override fun setTotpInputBlack() {
        binding.otpInput.setCustomTextColor(ContextCompat.getColor(binding.root.context, CoreUiR.color.text_primary))
    }

    override fun showWrongCodeError() {
        Snackbar
            .make(binding.root, LocalizationR.string.dialog_mfa_wrong_code, Snackbar.LENGTH_LONG)
            .apply {
                view.setBackgroundColor(context.getColor(CoreUiR.color.red))
                show()
            }
    }

    override fun pasteOtp(otp: String) {
        binding.otpInput.setText(otp)
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
        listener?.totpVerificationSucceeded(mfaHeader)
    }

    override fun navigateToLogin() {
        dismiss()
        authenticationResult.launch(
            ActivityIntents.authentication(
                requireContext(),
                ActivityIntents.AuthConfig.SignIn,
            ),
        )
    }

    override fun showSessionExpired() {
        Toast.makeText(requireContext(), LocalizationR.string.session_expired, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val EXTRA_AUTH_KEY = "EXTRA_AUTH_KEY"
        private const val EXTRA_OTHER_PROVIDER = "EXTRA_OTHER_PROVIDER"

        fun newInstance(
            token: String? = null,
            hasOtherProvider: Boolean,
        ) = EnterTotpDialog().apply {
            arguments =
                bundleOf(
                    EXTRA_AUTH_KEY to token,
                    EXTRA_OTHER_PROVIDER to hasOtherProvider,
                )
        }
    }
}

interface EnterTotpListener {
    fun totpOtherProviderClick(bearer: String)

    fun totpVerificationSucceeded(mfaHeader: String? = null)
}
