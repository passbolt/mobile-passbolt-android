package com.passbolt.mobile.android.feature.main.mainscreen.encouragements.chromenativeautofill

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.passbolt.mobile.android.common.ExternalDeeplinkHandler
import com.passbolt.mobile.android.common.extension.fromHtml
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.mvp.EdgeToEdgeDialogFragment
import com.passbolt.mobile.android.core.ui.circlestepsview.CircleStepItemModel
import com.passbolt.mobile.android.feature.main.databinding.DialogEncourageChromeNativeAutofillBinding
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
class EncourageChromeNativeAutofillServiceDialog :
    EdgeToEdgeDialogFragment(),
    EncourageChromeNativeAutofillContract.View,
    AndroidScopeComponent {
    private var listener: Listener? = null
    override val scope by fragmentScope(useParentActivityScope = false)
    private val presenter: EncourageChromeNativeAutofillContract.Presenter by scope.inject()
    private val externalDeeplinkHandler: ExternalDeeplinkHandler by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, CoreUiR.style.FullscreenDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = DialogEncourageChromeNativeAutofillBinding.inflate(inflater)
        setupListeners(binding)
        setupSteps(binding)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener =
            when {
                parentFragment is Listener -> parentFragment as Listener
                activity is Listener -> activity as Listener
                else -> error("Parent must implement ${Listener::class.java.name}")
            }
        presenter.attach(this)
    }

    override fun onResume() {
        super.onResume()
        presenter.resume()
    }

    override fun onDetach() {
        presenter.detach()
        super.onDetach()
    }

    private fun setupSteps(binding: DialogEncourageChromeNativeAutofillBinding) {
        binding.stepsView.addList(
            requireContext()
                .resources
                .getStringArray(LocalizationR.array.dialog_encourage_chrome_native_autofill_setup_steps)
                .mapIndexed { index, text -> CircleStepItemModel(text.fromHtml(), getStepDrawable(index)) },
        )
    }

    private fun getStepDrawable(index: Int) =
        try {
            CHROME_NATIVE_AUTOFILL_SETUP_STEPS_ICONS[index]
        } catch (ignored: Exception) {
            null
        }

    private fun setupListeners(binding: DialogEncourageChromeNativeAutofillBinding) {
        with(binding) {
            goToChromeSettingsButton.setDebouncingOnClick { presenter.goToChromeNativeAutofillSettingsClick() }
            maybeLaterButton.setDebouncingOnClick { presenter.maybeLaterClick() }
            closeButton.setDebouncingOnClick { presenter.closeClick() }
        }
    }

    override fun launchChromeNativeAutofillDeeplink() {
        externalDeeplinkHandler.openChromeNativeAutofillSettings(requireContext())
    }

    override fun notifyChromeNativeAutofillSetUp() {
        listener?.chromeNativeAutofillSetupSuccessfully()
        dismiss()
    }

    override fun close() {
        listener?.chromeNativeAutofillSetupClosed()
        dismiss()
    }

    private companion object {
        private val CHROME_NATIVE_AUTOFILL_SETUP_STEPS_ICONS =
            listOf(
                CoreUiR.drawable.passbolt_with_bg,
                CoreUiR.drawable.ic_chrome,
                CoreUiR.drawable.passbolt_with_bg,
            )
    }

    interface Listener {
        fun chromeNativeAutofillSetupClosed() {}

        fun chromeNativeAutofillSetupSuccessfully() {}
    }
}
