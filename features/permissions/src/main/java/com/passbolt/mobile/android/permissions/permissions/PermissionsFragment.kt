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

package com.passbolt.mobile.android.permissions.permissions

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
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.compose.PermissionsHostNavigation
import com.passbolt.mobile.android.core.navigation.compose.PermissionsNavigation
import com.passbolt.mobile.android.ui.PermissionsMode

// TODO MOB-3696: Remove fragment wrapper - use pure Compose navigation, eliminate findNavController/setFragmentResult/startActivity
class PermissionsFragment :
    Fragment(),
    PermissionsHostNavigation {
    private val args: PermissionsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                PermissionsNavigation(
                    id = args.id,
                    mode = args.mode,
                    permissionsItem = args.permissionsItem,
                    hostNavigation = this@PermissionsFragment,
                )
            }
        }

    override fun navigateBack() {
        findNavController().popBackStack()
    }

    override fun navigateToSelfWithMode(
        id: String,
        mode: PermissionsMode,
    ) {
        findNavController().navigate(
            PermissionsFragmentDirections.actionResourcePermissionsFragmentSelf(
                id,
                mode,
                args.permissionsItem,
            ),
        )
    }

    override fun closeWithShareSuccessResult() {
        setFragmentResult(
            REQUEST_UPDATE_PERMISSIONS,
            bundleOf(EXTRA_RESOURCE_SHARED to true),
        )
        findNavController().popBackStack()
    }

    override fun navigateToHome() {
        requireActivity().startActivity(ActivityIntents.bringHome(requireContext()))
    }

    companion object {
        const val REQUEST_UPDATE_PERMISSIONS = "REQUEST_UPDATE_PERMISSIONS"
        const val EXTRA_RESOURCE_SHARED = "RESOURCE_SHARED"
    }
}
