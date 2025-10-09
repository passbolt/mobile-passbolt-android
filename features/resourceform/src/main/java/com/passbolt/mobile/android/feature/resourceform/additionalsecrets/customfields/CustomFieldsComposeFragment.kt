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

package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.customfields

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
import com.passbolt.mobile.android.ui.CustomFieldsModel
import org.koin.androidx.compose.getViewModel

class CustomFieldsComposeFragment :
    Fragment(),
    CustomFieldsNavigation {
    private val navArgs: CustomFieldsComposeFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                getViewModel<CustomFieldsFormViewModel>().onIntent(CustomFieldsFormIntent.Initialize(navArgs.customFieldsModel))

                PassboltTheme {
                    CustomFieldsFormScreen(
                        navigation = this@CustomFieldsComposeFragment,
                    )
                }
            }
        }

    override fun navigateUp() {
        findNavController().popBackStack()
    }

    override fun navigateBackWithResult(model: CustomFieldsModel) {
        setFragmentResult(
            REQUEST_CUSTOM_FIELDS,
            bundleOf(EXTRA_CUSTOM_FIELDS to model),
        )
        findNavController().popBackStack()
    }

    companion object Companion {
        const val REQUEST_CUSTOM_FIELDS = "CUSTOM_FIELDS"

        const val EXTRA_CUSTOM_FIELDS = "custom_fields"
    }
}
