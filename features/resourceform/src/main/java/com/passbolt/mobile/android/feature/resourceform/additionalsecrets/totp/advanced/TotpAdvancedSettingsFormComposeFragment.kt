/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
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

package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced

import PassboltTheme
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.passbolt.mobile.android.ui.TotpUiModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

// TODO: replace this with just compose once result navigation gets merged
class TotpAdvancedSettingsFormComposeFragment :
    Fragment(),
    TotpAdvancedSettingsFormNavigation {
    private val navArgs: TotpAdvancedSettingsFormComposeFragmentArgs by navArgs()
    private val viewModel: TotpAdvancedSettingsFormViewModel by viewModel {
        parametersOf(navArgs.mode, navArgs.totpUiModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                PassboltTheme {
                    TotpAdvancedSettingsFormScreen(
                        navigation = this@TotpAdvancedSettingsFormComposeFragment,
                        viewModel = viewModel,
                    )
                }
            }
        }

    override fun navigateBack() {
        findNavController().popBackStack()
    }

    // TODO: replace this result navigation with Nav3/EventBus once it's merged
    override fun navigateBackWithResult(totpModel: TotpUiModel) {
        setFragmentResult(
            REQUEST_TOTP_ADVANCED,
            bundleOf(EXTRA_TOTP_ADVANCED to totpModel),
        )
        findNavController().popBackStack()
    }

    companion object {
        const val REQUEST_TOTP_ADVANCED = "TOTP_ADVANCED"

        const val EXTRA_TOTP_ADVANCED = "totp_advanced"
    }
}
