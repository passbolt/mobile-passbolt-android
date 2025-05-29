package com.passbolt.mobile.android.feature.autofill.encourage.accessibility

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.passbolt.mobile.android.common.dialogs.accessibilityServiceConsentDialog
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.feature.autofill.databinding.DialogAccessibilityEncourageAutofillBinding
import com.passbolt.mobile.android.feature.autofill.enabled.AutofillEnabledDialog
import com.passbolt.mobile.android.feature.autofill.enabled.DialogMode
import com.passbolt.mobile.android.feature.autofill.encourage.tutorial.AutofillTutorialDialog
import com.passbolt.mobile.android.feature.autofill.encourage.tutorial.SettingsNavigator
import com.passbolt.mobile.android.feature.autofill.encourage.tutorial.TutorialMode
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
class EncourageAccessibilityAutofillDialog :
    DialogFragment(),
    EncourageAccessibilityAutofillContract.View,
    AndroidScopeComponent,
    AutofillTutorialDialog.Listener {
    override val scope by fragmentScope(useParentActivityScope = false)
    private var listener: Listener? = null
    private val presenter: EncourageAccessibilityAutofillContract.Presenter by scope.inject()
    private lateinit var binding: DialogAccessibilityEncourageAutofillBinding
    private val settingsNavigator: SettingsNavigator by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, CoreUiR.style.FullscreenDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DialogAccessibilityEncourageAutofillBinding.inflate(inflater)
        setupListeners()
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        object : Dialog(requireContext(), theme) {
            override fun onBackPressed() {
                dismiss()
                presenter.backPressed()
            }
        }

    override fun onResume() {
        super.onResume()
        presenter.resume()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener =
            when {
                activity is Listener -> activity as Listener
                parentFragment is Listener -> parentFragment as Listener
                else -> error("Parent must implement ${Listener::class.java.name}")
            }
        presenter.attach(this)
    }

    override fun setOverlayEnabled(overlayEnabled: Boolean) {
        binding.overlaySwitch.isChecked = overlayEnabled
    }

    override fun setAccessibilityServiceEnabled(accessibilityServiceEnabled: Boolean) {
        binding.autofillSwitch.isChecked = accessibilityServiceEnabled
    }

    override fun onDismiss(dialog: DialogInterface) {
        listener = null
        presenter.detach()
        super.onDismiss(dialog)
    }

    private fun setupListeners() {
        with(binding) {
            closeButton.setDebouncingOnClick { presenter.closeClick() }
            overlayContainer.setDebouncingOnClick { presenter.overlayClick() }
            serviceContainer.setDebouncingOnClick { presenter.serviceClick() }
        }
    }

    override fun navigateToOverlayTutorial() {
        AutofillTutorialDialog()
            .apply {
                arguments =
                    bundleOf(
                        AutofillTutorialDialog.TUTORIAL_MODE_KEY to TutorialMode.Overlay,
                    )
            }.show(
                childFragmentManager,
                EncourageAccessibilityAutofillDialog::class.java.name,
            )
    }

    override fun navigateToServiceTutorial() {
        AutofillTutorialDialog()
            .apply {
                arguments =
                    bundleOf(
                        AutofillTutorialDialog.TUTORIAL_MODE_KEY to TutorialMode.Service,
                    )
            }.show(
                childFragmentManager,
                EncourageAccessibilityAutofillDialog::class.java.name,
            )
    }

    override fun navigateToOverlaySettings() {
        settingsNavigator.navigateToAppSettings(requireActivity())
    }

    override fun navigateToServiceSettings() {
        settingsNavigator.navigateToAccessibilitySettings(requireActivity())
    }

    override fun dismissWithNotify() {
        listener?.setupAccessibilityLaterClick()
        dismiss()
    }

    override fun dismissWithNoAction() {
        dismiss()
    }

    override fun showAutofillNotSupported() {
        AlertDialog
            .Builder(requireContext())
            .setTitle(LocalizationR.string.dialog_encourage_autofill_autofill_not_supported_title)
            .setMessage(LocalizationR.string.dialog_encourage_autofill_autofill_not_supported_message)
            .setPositiveButton(LocalizationR.string.ok) { _, _ -> }
            .show()
    }

    override fun autofillSettingsPossibleChange() {
        presenter.possibleAutofillChange()
    }

    override fun notifyPossibleAutofillChange() {
        listener?.accessibilitySettingsPossibleChange()
    }

    override fun showAutofillEnabledDialog() {
        AutofillEnabledDialog
            .newInstance(DialogMode.Settings)
            .show(childFragmentManager, AutofillEnabledDialog::class.java.name)
    }

    override fun showAccessibilityConsentDialog() {
        accessibilityServiceConsentDialog(requireContext()) {
            presenter.accessibilityServiceConsentGiven()
        }.show()
    }

    interface Listener {
        fun setupAccessibilityLaterClick()

        fun accessibilitySettingsPossibleChange()
    }
}
