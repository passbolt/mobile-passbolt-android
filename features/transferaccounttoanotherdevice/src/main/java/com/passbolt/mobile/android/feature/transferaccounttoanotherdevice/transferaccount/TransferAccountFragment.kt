package com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount

import android.os.Bundle
import android.view.View
import com.passbolt.mobile.android.common.dialogs.cancelTransferAccountAlertDialog
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.databinding.FragmentTransferAccountBinding
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

class TransferAccountFragment :
    BindingScopedAuthenticatedFragment<FragmentTransferAccountBinding, TransferAccountContract.View>(
        FragmentTransferAccountBinding::inflate
    ), TransferAccountContract.View {

    override val presenter: TransferAccountContract.Presenter by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.attach(this)
        initDefaultToolbar(binding.toolbar)
        setListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detach()
    }

    private fun setListeners() {
        binding.cancelTransferButton.setDebouncingOnClick {
            presenter.cancelTransferButtonClick()
        }
    }

    override fun showCancelTransferDialog() {
        cancelTransferAccountAlertDialog(requireContext()) {
            presenter.stopTransferClick()
        }.show()
    }

    override fun finish() {
        requireActivity().finish()
    }
}
