package com.passbolt.mobile.android.feature.autofill.encourage.autofill

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.passbolt.mobile.android.common.extension.fromHtml
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.ui.circlestepsview.CircleStepItemModel
import com.passbolt.mobile.android.feature.autofill.databinding.DialogEncourageAutofillBinding
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
class EncourageAutofillServiceDialog :
    DialogFragment(),
    EncourageAutofillContract.View,
    AndroidScopeComponent {
    override val scope by fragmentScope(useParentActivityScope = false)
    private var listener: Listener? = null
    private val presenter: EncourageAutofillContract.Presenter by scope.inject()
    private var autofillSystemSettingsLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                presenter.autofillSettingsClosedWithResult()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, CoreUiR.style.FullscreenDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = DialogEncourageAutofillBinding.inflate(inflater)
        setupListeners(binding)
        setupSteps(binding)
        return binding.root
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

    override fun onDetach() {
        listener = null
        presenter.detach()
        super.onDetach()
    }

    private fun setupSteps(binding: DialogEncourageAutofillBinding) {
        binding.stepsView.addList(
            requireContext()
                .resources
                .getStringArray(LocalizationR.array.dialog_encourage_autofill_setup_steps)
                .mapIndexed { index, text -> CircleStepItemModel(text.fromHtml(), getStepDrawable(index)) },
        )
    }

    private fun getStepDrawable(index: Int) =
        try {
            AUTOFILL_SETUP_STEPS_ICONS[index]
        } catch (ignored: Exception) {
            null
        }

    private fun setupListeners(binding: DialogEncourageAutofillBinding) {
        with(binding) {
            goToSettingsButton.setDebouncingOnClick { presenter.goToSettingsClick() }
            maybeLaterButton.setDebouncingOnClick { presenter.maybeLaterClick() }
            closeButton.setDebouncingOnClick { presenter.closeClick() }
        }
    }

    override fun openAutofillSettings() {
        autofillSystemSettingsLauncher.launch(
            Intent(
                Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE,
                Uri.parse(PACKAGE_URI_FORMAT.format(requireContext().packageName)),
            ),
        )
    }

    override fun showAutofillNotSupported() {
        AlertDialog
            .Builder(requireContext())
            .setTitle(LocalizationR.string.dialog_encourage_autofill_autofill_not_supported_title)
            .setMessage(LocalizationR.string.dialog_encourage_autofill_autofill_not_supported_message)
            .setPositiveButton(LocalizationR.string.ok) { _, _ -> }
            .show()
    }

    override fun closeWithSuccess() {
        listener?.autofillSetupSuccessfully()
        super.dismiss()
    }

    override fun close() {
        listener?.autofillSetupClosed()
        dismiss()
    }

    private companion object {
        private const val PACKAGE_URI_FORMAT = "package:%s"
        private val AUTOFILL_SETUP_STEPS_ICONS =
            listOf(
                CoreUiR.drawable.autofill_with_bg,
                CoreUiR.drawable.passbolt_with_bg,
            )
    }

    interface Listener {
        fun autofillSetupClosed() {}

        fun autofillSetupSuccessfully()
    }
}
