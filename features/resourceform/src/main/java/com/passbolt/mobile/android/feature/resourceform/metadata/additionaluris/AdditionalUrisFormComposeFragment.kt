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

package com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris

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
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormIntent.Initialize
import com.passbolt.mobile.android.ui.AdditionalUrisUiModel
import org.koin.androidx.compose.koinViewModel

class AdditionalUrisFormComposeFragment :
    Fragment(),
    AdditionalUrisFormNavigation {
    private val navArgs: AdditionalUrisFormComposeFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                koinViewModel<AdditionalUrisFormViewModel>().onIntent(
                    Initialize(navArgs.mode, navArgs.additionalUris),
                )

                PassboltTheme {
                    AdditionalUrisFormScreen(
                        navigation = this@AdditionalUrisFormComposeFragment,
                    )
                }
            }
        }

    override fun navigateUp() {
        findNavController().popBackStack()
    }

    override fun navigateBackWithResult(model: AdditionalUrisUiModel) {
        setFragmentResult(
            REQUEST_ADDITIONAL_URIS,
            bundleOf(EXTRA_ADDITIONAL_URIS to model),
        )
        findNavController().popBackStack()
    }

    companion object {
        const val REQUEST_ADDITIONAL_URIS = "ADDITIONAL_URIS"

        const val EXTRA_ADDITIONAL_URIS = "additional_uris"
    }
}
