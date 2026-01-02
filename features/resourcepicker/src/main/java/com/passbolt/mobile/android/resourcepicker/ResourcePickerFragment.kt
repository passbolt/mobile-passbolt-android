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

package com.passbolt.mobile.android.resourcepicker

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
import com.passbolt.mobile.android.resourcepicker.model.PickResourceAction
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerScreen
import com.passbolt.mobile.android.ui.ResourceModel

class ResourcePickerFragment :
    Fragment(),
    ResourcePickerNavigation {
    private val args: ResourcePickerFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                PassboltTheme {
                    ResourcePickerScreen(
                        suggestionUri = args.suggestionUri,
                        navigation = this@ResourcePickerFragment,
                    )
                }
            }
        }

    override fun navigateUp() {
        findNavController().popBackStack()
    }

    override fun navigateBackWithResult(
        pickAction: PickResourceAction,
        resourceModel: ResourceModel,
    ) {
        setFragmentResult(
            REQUEST_PICK_RESOURCE_FOR_RESULT,
            bundleOf(
                RESULT_PICKED_ACTION to pickAction,
                RESULT_PICKED_RESOURCE to resourceModel,
            ),
        )
        findNavController().popBackStack()
    }

    companion object {
        const val REQUEST_PICK_RESOURCE_FOR_RESULT = "PICK_RESOURCE_FOR_RESULT"
        const val RESULT_PICKED_RESOURCE = "PICKED_RESOURCE"
        const val RESULT_PICKED_ACTION = "PICK_ACTION"
    }
}
