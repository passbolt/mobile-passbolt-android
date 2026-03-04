package com.passbolt.mobile.android.feature.resourcedetails.details

import PassboltTheme
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.passbolt.mobile.android.core.navigation.deeplinks.NavDeepLinkProvider
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.ResourceEdited
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.ResourceShared
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormFragment
import com.passbolt.mobile.android.feature.resources.ResourcesDetailsDirections
import com.passbolt.mobile.android.locationdetails.ui.LocationItem
import com.passbolt.mobile.android.permissions.permissions.PermissionsFragment
import com.passbolt.mobile.android.ui.PermissionsItem
import com.passbolt.mobile.android.ui.PermissionsMode
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.ResourceModel
import org.koin.androidx.compose.koinViewModel

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

class ResourceDetailsFragment :
    Fragment(),
    ResourceDetailsNavigation {
    private val navArgs: ResourceDetailsFragmentArgs by navArgs()
    private lateinit var viewModel: ResourceDetailsViewModel

    private val resourceEditResult = { _: String, result: Bundle ->
        if (result.containsKey(ResourceFormFragment.EXTRA_RESOURCE_EDITED)) {
            viewModel.onIntent(
                ResourceEdited(
                    resourceName = result.getString(ResourceFormFragment.EXTRA_RESOURCE_NAME),
                ),
            )
        }
    }

    private val resourceShareResult = { _: String, _: Bundle ->
        viewModel.onIntent(ResourceShared)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                viewModel = koinViewModel()
                PassboltTheme {
                    ResourceDetailsScreen(
                        resourceModel = navArgs.resourceModel,
                        navigation = this@ResourceDetailsFragment,
                        viewModel = viewModel,
                    )
                }
            }
        }

    override fun navigateBack() {
        findNavController().popBackStack()
    }

    // TODO MOB-3690: Replace XML navigation to ResourceForm with Compose navigation
    override fun navigateToEditResource(resourceModel: ResourceModel) {
        setFragmentResultListener(ResourceFormFragment.REQUEST_RESOURCE_FORM, resourceEditResult)
        findNavController().navigate(
            ResourceDetailsFragmentDirections.actionResourceDetailsToResourceForm(
                ResourceFormMode.Edit(
                    resourceModel.resourceId,
                    resourceModel.metadataJsonModel.name,
                ),
            ),
        )
    }

    // TODO MOB-3696: Replace XML navigation to Permissions with Compose navigation
    override fun navigateToResourcePermissions(
        resourceId: String,
        mode: PermissionsMode,
    ) {
        setFragmentResultListener(
            PermissionsFragment.REQUEST_UPDATE_PERMISSIONS,
            resourceShareResult,
        )

        findNavController().navigate(
            ResourcesDetailsDirections.actionResourceDetailsToResourcePermissions(
                resourceId,
                mode,
                PermissionsItem.RESOURCE,
            ),
        )
    }

    override fun navigateToResourceTags(resourceId: String) {
        findNavController().navigate(
            NavDeepLinkProvider.resourceTagsDeepLinkRequest(resourceId),
        )
    }

    override fun navigateToResourceLocation(resourceId: String) {
        val request =
            NavDeepLinkProvider.locationDetailsDeepLinkRequest(
                locationDetailsItemName = LocationItem.RESOURCE.name,
                locationDetailsItemId = resourceId,
            )
        findNavController().navigate(request)
    }

    override fun closeWithDeleteSuccessResult(resourceName: String) {
        setFragmentResult(
            REQUEST_RESOURCE_DETAILS,
            bundleOf(
                EXTRA_RESOURCE_DELETED to true,
                EXTRA_RESOURCE_NAME to resourceName,
            ),
        )
        findNavController().popBackStack()
    }

    override fun setResourceEditedResult(resourceName: String) {
        setFragmentResult(
            REQUEST_RESOURCE_DETAILS,
            bundleOf(
                EXTRA_RESOURCE_EDITED to true,
                EXTRA_RESOURCE_NAME to resourceName,
            ),
        )
    }

    companion object {
        const val REQUEST_RESOURCE_DETAILS = "RESOURCE_DETAILS"

        const val EXTRA_RESOURCE_EDITED = "RESOURCE_EDITED"
        const val EXTRA_RESOURCE_DELETED = "RESOURCE_DELETED"
        const val EXTRA_RESOURCE_NAME = "RESOURCE_NAME"
    }
}
