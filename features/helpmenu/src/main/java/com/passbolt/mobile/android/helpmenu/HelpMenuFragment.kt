package com.passbolt.mobile.android.helpmenu

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.passbolt.mobile.android.common.WebsiteOpener
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.visible
import com.passbolt.mobile.android.feature.helpmenu.databinding.ViewHelpBottomsheetBinding
import com.passbolt.mobile.android.ui.HelpMenuModel
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.fragmentScope
import org.koin.core.scope.Scope
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

class HelpMenuFragment : BottomSheetDialogFragment(), AndroidScopeComponent, HelpMenuContract.View {

    override val scope: Scope by fragmentScope()
    private lateinit var binding: ViewHelpBottomsheetBinding
    private var listener: Listener? = null
    private val websiteOpener: WebsiteOpener by inject()
    private val bundledShowQrCodesHelp by lifecycleAwareLazy {
        requireNotNull(requireArguments().getParcelable<HelpMenuModel>(EXTRA_HELP_MENU_MODEL))
    }
    private val presenter: HelpMenuContract.Presenter by scope.inject()
    private val enableLogsSwitchChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        presenter.logsSettingChanged(isChecked)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = ViewHelpBottomsheetBinding.inflate(inflater)
        return binding.root
    }

    override fun onDestroyView() {
        binding.enableLogsSwitch.setOnCheckedChangeListener(null)
        scope.close()
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        presenter.attach(this)
        presenter.argsRetrieved(bundledShowQrCodesHelp)
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
            accessLogs.setListenerWithDismiss {
                listener?.menuShowLogsClick()
            }
            visitHelpWebsite.setListenerWithDismiss {
                openHelpWebsite()
            }
            whyScanQrCodes.setListenerWithDismiss {
                listener?.menuWhyScanQrCodesClick()
            }
            importProfileManually.setListenerWithDismiss {
                listener?.menuImportProfileManuallyClick()
            }
            enableLogsSwitch.setOnCheckedChangeListener(enableLogsSwitchChangeListener)
            close.setListenerWithDismiss { }
        }
    }

    private fun View.setListenerWithDismiss(action: () -> Unit) {
        setDebouncingOnClick {
            action()
            dismiss()
        }
    }

    private fun openHelpWebsite() {
        websiteOpener.open(requireContext(), getString(LocalizationR.string.help_website))
    }

    override fun showScanQrCodesHelp() {
        binding.whyScanQrCodes.visible()
    }

    override fun setEnableLogsSwitchOff() {
        changeSwitchStateSilently(isOn = false)
    }

    override fun setEnableLogsSwitchOn() {
        changeSwitchStateSilently(isOn = true)
    }

    private fun changeSwitchStateSilently(isOn: Boolean) {
        with(binding.enableLogsSwitch) {
            setOnCheckedChangeListener(null)
            isChecked = isOn
            setOnCheckedChangeListener(enableLogsSwitchChangeListener)
        }
    }

    override fun showImportProfileHelp() {
        binding.importProfileManually.visible()
    }

    companion object {
        private const val EXTRA_HELP_MENU_MODEL = "HELP_MENU_MODEL"

        fun newInstance(helpMenuModel: HelpMenuModel) = HelpMenuFragment().apply {
            arguments = bundleOf(
                EXTRA_HELP_MENU_MODEL to helpMenuModel
            )
        }
    }

    override fun enableAccessLogs() {
        binding.accessLogs.isEnabled = true
    }

    override fun disableAccessLogs() {
        binding.accessLogs.isEnabled = false
    }

    interface Listener {
        fun menuShowLogsClick()
        fun menuWhyScanQrCodesClick() {}
        fun menuImportProfileManuallyClick() {}
    }
}
