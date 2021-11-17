package com.passbolt.mobile.android.feature.resources

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.findNavHostFragment
import com.passbolt.mobile.android.core.mvp.viewbinding.BindingActivity
import com.passbolt.mobile.android.core.security.FlagSecureSetter
import com.passbolt.mobile.android.feature.resources.databinding.ActivityResourcesBinding
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
class ResourcesActivity : BindingActivity<ActivityResourcesBinding>(ActivityResourcesBinding::inflate) {

    private val flagSecureSetter: FlagSecureSetter by inject()
    private val mode by lifecycleAwareLazy {
        intent.getSerializableExtra(RESOURCE_MODE_EXTRA) as ResourceMode
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        flagSecureSetter.set(this)

        val navHostFragment = findNavHostFragment(R.id.fragmentContainer)
        val inflater = navHostFragment.navController.navInflater

        val graph = when (mode) {
            ResourceMode.NEW -> inflater.inflate(R.navigation.resources_new)
            ResourceMode.DETAILS -> inflater.inflate(R.navigation.resources_details)
        }

        navHostFragment.navController.setGraph(graph, intent.extras)
    }

    companion object {
        const val RESULT_RESOURCE_DELETED = 8000
        const val EXTRA_RESOURCE_NAME = "EXTRA_RESOURCE_NAME"
        const val RESOURCE_MODEL_KEY = "resourceModel"
        private const val RESOURCE_MODE_EXTRA = "RESOURCE_MODE_EXTRA"

        fun newInstance(mode: ResourceMode, context: Context) = Intent(context, ResourcesActivity::class.java).apply {
            putExtra(RESOURCE_MODE_EXTRA, mode)
        }

        fun resourceDeletedResultIntent(resourceName: String) = Intent()
            .putExtra(EXTRA_RESOURCE_NAME, resourceName)
    }

    enum class ResourceMode {
        NEW,
        DETAILS
    }
}
