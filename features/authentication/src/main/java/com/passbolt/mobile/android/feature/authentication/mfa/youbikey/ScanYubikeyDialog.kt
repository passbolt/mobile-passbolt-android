package com.passbolt.mobile.android.feature.authentication.mfa.youbikey

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.google.android.material.snackbar.Snackbar
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.feature.authentication.R
import com.passbolt.mobile.android.feature.authentication.databinding.DialogScanYubikeyBinding
import com.yubico.yubikit.android.ui.OtpActivity
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.fragmentScope

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

class ScanYubikeyDialog : DialogFragment(), AndroidScopeComponent, ScanYubikeyContract.View {

    override val scope by fragmentScope()
    private var listener: Listener? = null
    private val presenter: ScanYubikeyContract.Presenter by scope.inject()
    private val scanYubikeyResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val otp = it.data?.getStringExtra(OtpActivity.EXTRA_OTP)
            presenter.yubikeyScanned(otp)
        } else {
            presenter.yubikeyScanCancelled()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.FullscreenDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DialogScanYubikeyBinding.inflate(inflater)
        setupListeners(binding)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = when {
            activity is Listener -> activity as Listener
            parentFragment is Listener -> parentFragment as Listener
            else -> error("Parent must implement ${Listener::class.java.name}")
        }
        presenter.attach(this)
    }

    override fun onDetach() {
        listener = null
        presenter.detach()
        super.onDetach()
    }

    private fun setupListeners(binding: DialogScanYubikeyBinding) {
        with(binding) {
            scanYubikeyButton.setDebouncingOnClick { presenter.scanYubikeyClick() }
            otherProviderButton.setDebouncingOnClick { presenter.otherProviderClick() }
            closeButton.setDebouncingOnClick { presenter.closeClick() }
            rememberMeCheckBox.setOnCheckedChangeListener { _, isChecked ->
                presenter.rememberMeCheckChanged(isChecked)
            }
        }
    }

    override fun showScanYubikey() {
        scanYubikeyResult.launch(
            Intent(requireContext(), OtpActivity::class.java)
        )
    }

    override fun showScanOtpCancelled() {
        Snackbar.make(requireView(), R.string.dialog_mfa_scan_cancelled, Snackbar.LENGTH_SHORT)
            .show()
    }

    override fun showEmptyScannedOtp() {
        Snackbar.make(requireView(), R.string.dialog_mfa_scan_empty_otp, Snackbar.LENGTH_SHORT)
            .show()
    }

    override fun navigateToTotp() {
        listener?.changeProviderToTotp()
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

    interface Listener {
        fun changeProviderToTotp()

        // TODO notify caller after successful otp backend verification
    }
}
