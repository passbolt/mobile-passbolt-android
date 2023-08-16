package com.passbolt.mobile.android.feature.settings.screen.debuglogssettings

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.passbolt.mobile.android.common.WebsiteOpener
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.feature.settings.databinding.FragmentDebugLogsSettingsBinding
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

class DebugLogsSettingsFragment :
    BindingScopedFragment<FragmentDebugLogsSettingsBinding>(FragmentDebugLogsSettingsBinding::inflate),
    DebugLogsSettingsContract.View {

    private val presenter: DebugLogsSettingsContract.Presenter by inject()
    private val websiteOpener: WebsiteOpener by inject()

    private var logSettingChanged: ((Boolean) -> Unit)? = {
        presenter.enableDebugLogsChanged(it)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(binding.toolbar)
        setListeners()
        presenter.attach(this)
    }

    override fun onDestroyView() {
        binding.enableLogsSetting.onChanged = null
        presenter.detach()
        super.onDestroyView()
    }

    private fun setListeners() {
        with(binding) {
            accessLogsSetting.setDebouncingOnClick {
                presenter.logsClick()
            }
            enableLogsSetting.onChanged = logSettingChanged
            visitHelpWebsite.setDebouncingOnClick {
                presenter.visitHelpClick()
            }
        }
    }

    override fun navigateToLogs() {
        findNavController().navigate(
            DebugLogsSettingsFragmentDirections.actionDebugLogsSettingsFragmentToLogs()
        )
    }

    override fun setEnableLogsSwitchOn() {
        binding.enableLogsSetting.turnOn(silently = true)
    }

    override fun setEnableLogsSwitchOff() {
        binding.enableLogsSetting.turnOff(silently = true)
    }

    override fun enableAccessLogs() {
        with(binding.accessLogsSetting) {
            isEnabled = true
            setDebouncingOnClick { presenter.logsClick() }
        }
    }

    override fun disableAccessLogs() {
        with(binding.accessLogsSetting) {
            isEnabled = false
            setDebouncingOnClick { /* ignore */ }
        }
    }

    override fun openHelpWebsite() {
        websiteOpener.open(requireContext(), getString(LocalizationR.string.help_website))
    }
}
