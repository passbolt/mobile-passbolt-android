package com.passbolt.mobile.android.feature.resourcedetails

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.findNavHostFragment
import com.passbolt.mobile.android.core.mvp.viewbinding.BindingActivity
import com.passbolt.mobile.android.core.navigation.deeplinks.NavDeepLinkProvider
import com.passbolt.mobile.android.core.security.flagsecure.FlagSecureSetter
import com.passbolt.mobile.android.core.security.runtimeauth.RuntimeAuthenticatedFlag
import com.passbolt.mobile.android.feature.resources.R
import com.passbolt.mobile.android.feature.resources.databinding.ActivityResourcesBinding
import com.passbolt.mobile.android.permissions.permissions.NavigationOrigin
import com.passbolt.mobile.android.permissions.permissions.PermissionsItem
import com.passbolt.mobile.android.permissions.permissions.PermissionsMode
import com.passbolt.mobile.android.ui.ResourceModel
import org.koin.android.ext.android.inject

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
class ResourceActivity : BindingActivity<ActivityResourcesBinding>(ActivityResourcesBinding::inflate) {

    private val flagSecureSetter: FlagSecureSetter by inject()
    private val runtimeAuthenticatedFlag: RuntimeAuthenticatedFlag by inject()

    private val mode by lifecycleAwareLazy {
        intent.getSerializableExtra(EXTRA_RESOURCE_MODE) as ResourceMode
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        runtimeAuthenticatedFlag.require(this)
        flagSecureSetter.set(this)

        val navHostFragment = findNavHostFragment(R.id.fragmentContainer)
        val inflater = navHostFragment.navController.navInflater

        val graph = when (mode) {
            ResourceMode.NEW -> inflater.inflate(R.navigation.resources_new)
            ResourceMode.EDIT -> inflater.inflate(R.navigation.resources_new)
            ResourceMode.DETAILS -> inflater.inflate(R.navigation.resources_details)
            ResourceMode.SHARE -> inflater.inflate(R.navigation.resources_details)
        }

        navHostFragment.navController.setGraph(graph, intent.extras)

        if (mode == ResourceMode.SHARE) {
            navigateToResourcePermissions(navHostFragment)
        }
    }

    private fun navigateToResourcePermissions(navHostFragment: NavHostFragment) {
        val resourceId =
            requireNotNull(intent.getParcelableExtra<ResourceModel>(EXTRA_RESOURCE_MODEL)).resourceId

        val request = NavDeepLinkProvider.permissionsDeepLinkRequest(
            permissionItemName = PermissionsItem.RESOURCE.name,
            permissionItemId = resourceId,
            permissionsModeName = PermissionsMode.EDIT.name,
            navigationOriginName = NavigationOrigin.HOME_RESOURCE_MORE_MENU.name
        )

        navHostFragment.navController.navigate(request)
    }

    companion object {
        const val EXTRA_RESOURCE_MODEL = "RESOURCE_MODEL"
        const val EXTRA_RESOURCE_MODE = "RESOURCE_MODE"

        const val EXTRA_RESOURCE_NAME = "RESOURCE_NAME"
        const val EXTRA_RESOURCE_ID = "RESOURCE_ID"
        const val EXTRA_RESOURCE_PARENT_FOLDER_ID = "RESOURCE_PARENT_FOLDER_ID"

        fun newInstance(
            context: Context,
            mode: ResourceMode,
            resourceParentFolderId: String? = null,
            existingResource: ResourceModel? = null
        ) =
            Intent(context, ResourceActivity::class.java).apply {
                putExtra(EXTRA_RESOURCE_MODE, mode)
                putExtra(EXTRA_RESOURCE_MODEL, existingResource)
                putExtra(EXTRA_RESOURCE_PARENT_FOLDER_ID, resourceParentFolderId)
            }

        fun resourceNameResultIntent(resourceName: String) =
            Intent().apply {
                putExtra(EXTRA_RESOURCE_NAME, resourceName)
            }

        fun resourceNameAndIdIntent(resourceName: String, resourceId: String) =
            Intent().apply {
                putExtra(EXTRA_RESOURCE_NAME, resourceName)
                putExtra(EXTRA_RESOURCE_ID, resourceId)
            }
    }
}
