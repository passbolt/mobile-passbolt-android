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

package com.passbolt.mobile.android.feature.otp.otpmoremenu

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.passbolt.mobile.android.common.extension.setDebouncingOnClickAndDismiss
import com.passbolt.mobile.android.common.extension.visible
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.feature.otp.databinding.BottomsheetOtpMoreMenuBinding
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.fragmentScope

class OtpMoreMenuFragment : BottomSheetDialogFragment(), OtpMoreMenuContract.View, AndroidScopeComponent {

    override val scope by fragmentScope()
    private val presenter: OtpMoreMenuContract.Presenter by scope.inject()
    private val menuModel: ResourceMoreMenuModel by lifecycleAwareLazy {
        requireNotNull(requireArguments().getParcelable(EXTRA_OTP_MENU_MODEL))
    }
    private lateinit var binding: BottomsheetOtpMoreMenuBinding
    private var listener: Listener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomsheetOtpMoreMenuBinding.inflate(inflater)
        presenter.attach(this)
        presenter.argsRetrieved(menuModel)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = when {
            activity is Listener -> activity as Listener
            parentFragment is Listener -> parentFragment as Listener
            else -> error("Parent must implement ${Listener::class.java.name}")
        }
    }

    override fun onDetach() {
        listener = null
        super.onDetach()
    }

    private fun setListeners() {
        with(binding) {
            setDebouncingOnClickAndDismiss(showOtp) { listener?.menuShowOtpClick() }
            setDebouncingOnClickAndDismiss(copyOtp) { listener?.menuCopyOtpClick() }
            setDebouncingOnClickAndDismiss(deleteOtp) { listener?.menuDeleteOtpClick() }
            setDebouncingOnClickAndDismiss(close)
        }
    }

    override fun showTitle(title: String) {
        binding.title.text = menuModel.title
    }

    override fun showSeparator() {
        binding.separator.visible()
    }

    override fun showDeleteButton() {
        binding.deleteOtp.visible()
    }

    companion object {
        private const val EXTRA_OTP_MENU_MODEL = "OTP_MENU_MODEL"

        fun newInstance(model: ResourceMoreMenuModel) =
            OtpMoreMenuFragment().apply {
                arguments = bundleOf(EXTRA_OTP_MENU_MODEL to model)
            }
    }

    interface Listener {
        fun menuCopyOtpClick()
        fun menuShowOtpClick()
        fun menuEditOtpClick()
        fun menuDeleteOtpClick()
    }
}
