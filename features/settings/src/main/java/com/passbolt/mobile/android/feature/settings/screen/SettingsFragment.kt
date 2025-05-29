package com.passbolt.mobile.android.feature.settings.screen

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.passbolt.mobile.android.common.dialogs.signOutAlertDialog
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.feature.settings.databinding.FragmentSettingsBinding
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

class SettingsFragment :
    BindingScopedFragment<FragmentSettingsBinding>(FragmentSettingsBinding::inflate),
    SettingsContract.View {
    private val presenter: SettingsContract.Presenter by inject()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        presenter.attach(this)
    }

    private fun setListeners() {
        with(requiredBinding) {
            appSettings.setDebouncingOnClick {
                presenter.appSettingsClick()
            }
            accountsSettings.setDebouncingOnClick {
                presenter.accountsSettingsClick()
            }
            termsAndLicensesSettings.setDebouncingOnClick {
                presenter.termsAndLicensesClick()
            }
            debugLogsSettings.setDebouncingOnClick {
                presenter.debugLogsSettingsClick()
            }
            signOut.setDebouncingOnClick {
                presenter.signOutClick()
            }
        }
    }

    override fun showLogoutDialog() {
        signOutAlertDialog(requireContext()) { presenter.logoutConfirmed() }
            .show()
    }

    override fun navigateToSignInWithLogout() {
        startActivity(
            ActivityIntents.authentication(
                requireContext(),
                ActivityIntents.AuthConfig.Startup,
            ),
        )
    }

    override fun showProgress() {
        showProgressDialog(childFragmentManager)
    }

    override fun hideProgress() {
        hideProgressDialog(childFragmentManager)
    }

    override fun navigateAccountsSettings() {
        findNavController().navigate(
            SettingsFragmentDirections.actionSettingsToAccountsSettingsFragment(),
        )
    }

    override fun navigateToDebugLogsSettings() {
        findNavController().navigate(
            SettingsFragmentDirections.actionSettingsToDebugLogsSettingsComposeFragment(),
        )
    }

    override fun navigateToTermsAndLicensesSettings() {
        findNavController().navigate(
            SettingsFragmentDirections.actionSettingsToTermsAndLicensesSettingsFragment(),
        )
    }

    override fun navigateToAppSettings() {
        findNavController().navigate(
            SettingsFragmentDirections.actionSettingsToAppSettingsFragment(),
        )
    }
}
