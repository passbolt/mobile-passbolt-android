package com.passbolt.mobile.android.feature.autofill.encourage.accessibility

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.feature.autofill.R
import com.passbolt.mobile.android.feature.autofill.databinding.DialogAccessibilityEncourageAutofillBinding
import com.passbolt.mobile.android.feature.autofill.encourage.tutorial.AutofillTutorialDialog
import com.passbolt.mobile.android.feature.autofill.encourage.tutorial.TutorialMode
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.fragmentScope

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
class EncourageAccessibilityAutofillDialog : DialogFragment(), EncourageAccessibilityAutofillContract.View,
    AndroidScopeComponent, AutofillTutorialDialog.Listener {

    override val scope by fragmentScope()
    private var listener: Listener? = null
    private val presenter: EncourageAccessibilityAutofillContract.Presenter by scope.inject()
    private lateinit var binding: DialogAccessibilityEncourageAutofillBinding
    private val dialogMode: DialogMode by lifecycleAwareLazy {
        requireArguments().getSerializable(DIALOG_MODE_KEY) as DialogMode
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.FullscreenDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DialogAccessibilityEncourageAutofillBinding.inflate(inflater)
        setupListeners()
        setupView()
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        presenter.dialogCreate()
        return object : Dialog(requireContext(), theme) {
            override fun onBackPressed() {
                dismiss()
                presenter.backPressed()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.resume()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = when {
            activity is Listener -> activity as Listener
            parentFragment is Listener -> parentFragment as Listener
            else -> error("Parent must implement ${Listener::class.java.name}")
        }
        presenter.attach(this)
    }

    private fun setupView() {
        binding.headerLabel.text = context?.getString(dialogMode.title)
        binding.descriptionLabel.text = context?.getString(dialogMode.description)
        binding.maybeLaterButton.isVisible = dialogMode.buttonVisible
    }

    override fun setOverlayEnabled(overlayEnabled: Boolean) {
        binding.overlaySwitch.isChecked = overlayEnabled
    }

    override fun setAccessibilityServiceEnabled(accessibilityServiceEnabled: Boolean) {
        binding.autofillSwitch.isChecked = accessibilityServiceEnabled
    }

    override fun onDetach() {
        listener = null
        presenter.detach()
        super.onDetach()
    }

    private fun setupListeners() {
        with(binding) {
            maybeLaterButton.setDebouncingOnClick { presenter.maybeLaterClick() }
            closeButton.setDebouncingOnClick { presenter.closeClick() }
            overlayContainer.setDebouncingOnClick { presenter.overlayClick() }
            serviceContainer.setDebouncingOnClick { presenter.serviceClick() }
        }
    }

    override fun navigateToOverlayTutorial() {
        AutofillTutorialDialog().apply {
            arguments = bundleOf(
                AutofillTutorialDialog.TUTORIAL_MODE_KEY to TutorialMode.Overlay
            )
        }.show(
            childFragmentManager, EncourageAccessibilityAutofillDialog::class.java.name
        )
    }

    override fun navigateToServiceTutorial() {
        AutofillTutorialDialog().apply {
            arguments = bundleOf(
                AutofillTutorialDialog.TUTORIAL_MODE_KEY to TutorialMode.Service
            )
        }.show(
            childFragmentManager, EncourageAccessibilityAutofillDialog::class.java.name
        )
    }

    override fun dismissWithNoAction() {
        listener?.setupAutofillLaterClick()
        dismiss()
    }

    override fun closeWithSuccess() {
        listener?.setupAutofillSuccess()
        dismiss()
    }

    override fun closeTutorial() {
        listener?.setupAutofillLaterClick()
        dismiss()
    }

    override fun showAutofillNotSupported() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_encourage_autofill_autofill_not_supported_title)
            .setMessage(R.string.dialog_encourage_autofill_autofill_not_supported_message)
            .setPositiveButton(R.string.ok) { _, _ -> }
            .show()
    }

    override fun dismiss() {
        listener?.autofillDialogDismissed()
        super.dismiss()
    }

    companion object {
        const val DIALOG_MODE_KEY = "DIALOG_MODE_KEY"
    }

    interface Listener {
        fun setupAutofillLaterClick()
        fun setupAutofillSuccess()
        fun autofillDialogDismissed() {}
    }
}
