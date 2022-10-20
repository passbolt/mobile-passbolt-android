package com.passbolt.mobile.android.feature.setup.importprofile

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput
import com.passbolt.mobile.android.feature.setup.R
import com.passbolt.mobile.android.feature.setup.databinding.FragmentImportProfileBinding
import com.passbolt.mobile.android.feature.setup.summary.ResultStatus
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
class ImportProfileFragment :
    BindingScopedFragment<FragmentImportProfileBinding>(FragmentImportProfileBinding::inflate),
    ImportProfileContract.View {

    private val presenter: ImportProfileContract.Presenter by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(binding.toolbar)
        setUpListeners()
        presenter.attach(this)
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    private fun setUpListeners() {
        with(binding) {
            userIdInput.setTextChangeListener {
                presenter.userIdChanged(it)
            }
            accountUrlInput.setTextChangeListener {
                presenter.accountUrlChanged(it)
            }
            privateKeyInput.setTextChangeListener {
                presenter.privateKeyChanged(it)
            }
            importButton.setDebouncingOnClick {
                presenter.importClick()
            }
        }
    }

    override fun clearValidationErrors() {
        setOf(
            binding.userIdInput,
            binding.accountUrlInput,
            binding.privateKeyInput
        ).forEach {
            it.setState(StatefulInput.State.Default)
        }
    }

    override fun showIncorrectUuid() {
        binding.userIdInput.setState(
            StatefulInput.State.Error(
                getString(R.string.import_profile_invalid_uuid)
            )
        )
    }

    override fun showIncorrectAccountUrl() {
        binding.accountUrlInput.setState(
            StatefulInput.State.Error(
                getString(R.string.import_profile_invalid_account_url)
            )
        )
    }

    override fun showIncorrectPrivateKey() {
        binding.privateKeyInput.setState(
            StatefulInput.State.Error(
                getString(R.string.import_profile_invalid_private_key)
            )
        )
    }

    override fun navigateToSummary(status: ResultStatus) {
        findNavController().navigate(
            ImportProfileFragmentDirections.actionImportProfileFragmentToSummaryFragment(status)
        )
    }
}
