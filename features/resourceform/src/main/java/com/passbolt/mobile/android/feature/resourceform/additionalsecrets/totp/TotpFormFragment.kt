package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp

import android.os.Bundle
import android.view.View
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormFragment
import com.passbolt.mobile.android.feature.resourceform.databinding.FragmentTotpFormBinding
import com.passbolt.mobile.android.ui.TotpUiModel
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
class TotpFormFragment :
    BindingScopedFragment<FragmentTotpFormBinding>(
        FragmentTotpFormBinding::inflate
    ), TotpFormContract.View {

    private val presenter: TotpFormContract.Presenter by inject()
    private val navArgs: TotpFormFragmentArgs by navArgs()

    private val totpAdvancedResult = { _: String, result: Bundle ->
        if (result.containsKey(TotpAdvancedSettingsFormFragment.EXTRA_TOTP_ADVANCED)) {
            presenter.totpAdvancedSettingsChanged(
                BundleCompat.getParcelable(
                    result,
                    TotpAdvancedSettingsFormFragment.EXTRA_TOTP_ADVANCED,
                    TotpUiModel::class.java
                )
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(binding.toolbar)
        setListeners()
        presenter.attach(this)
        presenter.argsRetrieved(navArgs.mode, navArgs.totpUiModel)
    }

    private fun setListeners() {
        with(binding) {
            totpSubformView.moreSettingsClickListener = {
                presenter.moreSettingsClick()
            }
            totpSubformView.secretInput.setTextChangeListener {
                presenter.totpSecretChanged(it)
            }
            totpSubformView.urlInput.setTextChangeListener {
                presenter.totpUrlChanged(it)
            }
            apply.setDebouncingOnClick {
                presenter.applyClick()
            }
        }
    }

    override fun navigateToTotpAdvancedSettingsForm(uiModel: TotpUiModel) {
        setFragmentResultListener(TotpAdvancedSettingsFormFragment.REQUEST_TOTP_ADVANCED, totpAdvancedResult)
        findNavController().navigate(
            TotpFormFragmentDirections.actionTotpFormFragmentToTotpAdvancedSettingsFormFragment(navArgs.mode, uiModel)
        )
    }

    override fun showCreateTitle() {
        binding.toolbar.toolbarTitle = getString(LocalizationR.string.resource_form_create_totp)
    }

    override fun showSecret(secret: String) {
        binding.totpSubformView.secretInput.text = secret
    }

    override fun showUrl(issuer: String) {
        binding.totpSubformView.urlInput.text = issuer
    }

    override fun goBackWithResult(totpUiModel: TotpUiModel) {
        setFragmentResult(REQUEST_TOTP, bundleOf(EXTRA_TOTP to totpUiModel))
        findNavController().popBackStack()
    }

    companion object {
        const val REQUEST_TOTP = "TOTP"

        const val EXTRA_TOTP = "totp"
    }
}
