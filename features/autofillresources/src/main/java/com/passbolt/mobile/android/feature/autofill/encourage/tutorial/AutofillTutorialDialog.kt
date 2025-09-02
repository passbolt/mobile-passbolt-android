package com.passbolt.mobile.android.feature.autofill.encourage.tutorial

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import com.passbolt.mobile.android.common.ExternalDeeplinkHandler
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.mvp.EdgeToEdgeDialogFragment
import com.passbolt.mobile.android.feature.authentication.auth.accountdoesnotexist.AccountDoesNotExistDialog
import com.passbolt.mobile.android.feature.autofill.databinding.DialogAutofillTutorialBinding
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.fragmentScope
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
class AutofillTutorialDialog :
    EdgeToEdgeDialogFragment(),
    AutofillTutorialContract.View,
    AndroidScopeComponent {
    override val scope by fragmentScope(useParentActivityScope = false)
    private val presenter: AutofillTutorialContract.Presenter by scope.inject()
    private val tutorialMode by lifecycleAwareLazy {
        requireNotNull(
            BundleCompat.getSerializable(requireArguments(), TUTORIAL_MODE_KEY, TutorialMode::class.java),
        )
    }
    private val externalDeeplinkHandler: ExternalDeeplinkHandler by inject()
    private val settingsNavigator: SettingsNavigator by inject()
    private var listener: Listener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, CoreUiR.style.FullscreenDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = DialogAutofillTutorialBinding.inflate(inflater)
        setupView(binding)
        setupListeners(binding)
        presenter.argsReceived(tutorialMode)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener =
            when {
                parentFragment is Listener -> parentFragment as Listener
                activity is Listener -> activity as Listener
                else -> error("Parent must implement ${AccountDoesNotExistDialog.Listener::class.java.name}")
            }
        presenter.attach(this)
    }

    override fun onResume() {
        super.onResume()
        presenter.resume()
    }

    private fun setupView(binding: DialogAutofillTutorialBinding) =
        with(binding) {
            headerLabel.text = context?.getString(tutorialMode.title)
            descriptionLabel.text = context?.getString(tutorialMode.description)
        }

    override fun openWebsite(url: String) {
        externalDeeplinkHandler.openWebsite(requireContext(), url)
    }

    private fun setupListeners(binding: DialogAutofillTutorialBinding) =
        with(binding) {
            samsungContainer.setDebouncingOnClick { presenter.samsungClick() }
            xiaomiContainer.setDebouncingOnClick { presenter.xiaomiClick() }
            huaweiContainer.setDebouncingOnClick { presenter.huaweiClick() }
            otherContainer.setDebouncingOnClick { presenter.otherClick() }
            closeButton.setDebouncingOnClick { presenter.closeClick() }
            backButton.setDebouncingOnClick { presenter.backClick() }
            goToSettings.setDebouncingOnClick { presenter.goToSettingsClick() }
        }

    override fun navigateToOverlaySettings() {
        settingsNavigator.navigateToAppSettings(requireActivity())
    }

    override fun navigateToServiceSettings() {
        settingsNavigator.navigateToAccessibilitySettings(requireActivity())
    }

    override fun notifyAutofillSettingsPossibleChange() {
        listener?.autofillSettingsPossibleChange()
    }

    override fun closeDialog() {
        dismiss()
    }

    interface Listener {
        fun autofillSettingsPossibleChange()
    }

    companion object {
        const val TUTORIAL_MODE_KEY = "TUTORIAL_MODE_KEY"
    }
}
