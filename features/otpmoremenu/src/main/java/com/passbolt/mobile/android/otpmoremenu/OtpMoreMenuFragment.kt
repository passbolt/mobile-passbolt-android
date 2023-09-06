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

package com.passbolt.mobile.android.otpmoremenu

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import com.passbolt.mobile.android.common.extension.setDebouncingOnClickAndDismiss
import com.passbolt.mobile.android.common.extension.visible
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedBottomSheetFragment
import com.passbolt.mobile.android.otpmoremenu.databinding.BottomsheetOtpMoreMenuBinding
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent

class OtpMoreMenuFragment :
    BindingScopedAuthenticatedBottomSheetFragment<BottomsheetOtpMoreMenuBinding, OtpMoreMenuContract.View>(
        BottomsheetOtpMoreMenuBinding::inflate
    ), OtpMoreMenuContract.View, AndroidScopeComponent {

    override val presenter: OtpMoreMenuContract.Presenter by inject()
    private val resourceId: String by lifecycleAwareLazy {
        requireNotNull(requireArguments().getString(EXTRA_RESOURCE_ID))
    }
    private val initialResourceName: String by lifecycleAwareLazy {
        requireNotNull(requireArguments().getString(EXTRA_RESOURCE_NAME))
    }
    private val canShowTotp: Boolean by lifecycleAwareLazy {
        requireNotNull(requireArguments().getBoolean(EXTRA_CAN_SHOW_TOTP))
    }
    private var listener: Listener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showTitle(initialResourceName)
        setListeners()
        presenter.attach(this)
        presenter.argsRetrieved(resourceId, canShowTotp = canShowTotp)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = when {
            activity is Listener -> activity as Listener
            parentFragment is Listener -> parentFragment as Listener
            else -> error("Parent must implement ${Listener::class.java.name}")
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.resume(this)
    }

    override fun onPause() {
        presenter.pause()
        super.onPause()
    }

    override fun onDetach() {
        presenter.detach()
        listener?.otpMenuDismissed()
        listener = null
        super.onDetach()
    }

    private fun setListeners() {
        with(binding) {
            setDebouncingOnClickAndDismiss(showOtp) { listener?.menuShowOtpClick() }
            setDebouncingOnClickAndDismiss(copyOtp) { listener?.menuCopyOtpClick() }
            setDebouncingOnClickAndDismiss(deleteOtp) { listener?.menuDeleteOtpClick() }
            setDebouncingOnClickAndDismiss(editOtp) { listener?.menuEditOtpClick() }
            setDebouncingOnClickAndDismiss(close)
        }
    }

    override fun showTitle(title: String) {
        binding.title.text = title
    }

    override fun showSeparator() {
        binding.separator.visible()
    }

    override fun showDeleteButton() {
        binding.deleteOtp.visible()
    }

    override fun showEditButton() {
        binding.editOtp.visible()
    }

    override fun showShowOtpButton() {
        binding.showOtp.visible()
    }

    override fun showRefreshFailure() {
        showSnackbar(
            messageResId = R.string.common_data_refresh_error,
            backgroundColor = R.color.red
        )
    }

    override fun hideRefreshProgress() {
        // ignored - progress indicator should not be shown on the menu fragment
    }

    override fun showRefreshProgress() {
        // ignored - progress indicator should not be shown on the menu fragment
    }

    companion object {
        private const val EXTRA_RESOURCE_ID = "RESOURCE_ID"
        private const val EXTRA_RESOURCE_NAME = "RESOURCE_NAME"
        private const val EXTRA_CAN_SHOW_TOTP = "CAN_SHOW_TOTP"

        fun newInstance(resourceId: String, resourceName: String, canShowTotp: Boolean) =
            OtpMoreMenuFragment().apply {
                arguments = bundleOf(
                    EXTRA_RESOURCE_ID to resourceId,
                    EXTRA_RESOURCE_NAME to resourceName,
                    EXTRA_CAN_SHOW_TOTP to canShowTotp
                )
            }
    }

    interface Listener {
        fun menuCopyOtpClick()
        fun menuShowOtpClick() {} // on some flows show OTP is hidden
        fun menuEditOtpClick()
        fun menuDeleteOtpClick()
        fun otpMenuDismissed()
    }
}
