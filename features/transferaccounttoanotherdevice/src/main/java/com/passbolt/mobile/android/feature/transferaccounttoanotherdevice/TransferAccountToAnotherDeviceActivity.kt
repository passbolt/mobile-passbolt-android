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

package com.passbolt.mobile.android.feature.transferaccounttoanotherdevice

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.passbolt.mobile.android.core.mvp.viewbinding.BindingActivity
import com.passbolt.mobile.android.core.navigation.compose.base.Feature
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.TransferAccountToAnotherDeviceKey.Onboarding
import com.passbolt.mobile.android.core.security.runtimeauth.RuntimeAuthenticatedFlag
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.databinding.ActivityTransferAccountToAnotherDeviceBinding
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

// NOTE: When changing name or package read core/navigation/README.md
class TransferAccountToAnotherDeviceActivity :
    BindingActivity<ActivityTransferAccountToAnotherDeviceBinding>(
        ActivityTransferAccountToAnotherDeviceBinding::inflate,
    ),
    TransferAccountNavigation {
    private val runtimeAuthenticatedFlag: RuntimeAuthenticatedFlag by inject()
    private val transferAccountNavigation: FeatureModuleNavigation by inject(
        named(Feature.TRANSFER_ACCOUNT_TO_ANOTHER_DEVICE),
    )
    lateinit var backstackList: NavBackStack<NavKey>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runtimeAuthenticatedFlag.require(this)

        setContent {
            val backStack =
                rememberNavBackStack(Onboarding).apply {
                    backstackList = this
                }

            NavDisplay(
                backStack = backStack,
                onBack = {
                    if (backStack.size > 1) {
                        backStack.removeLastOrNull()
                    }
                },
                entryDecorators =
                    listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                entryProvider =
                    entryProvider {
                        transferAccountNavigation.provideEntryProviderInstaller().invoke(this)
                    },
            )
        }
    }

    override fun navigateToKey(key: NavKey) {
        backstackList.add(key)
    }

    override fun navigateBack() {
        if (backstackList.size > 1) {
            backstackList.removeLastOrNull()
        } else {
            finish()
        }
    }

    override fun popToKey(key: NavKey) {
        while (backstackList.size > 1 && backstackList.last() != key) {
            backstackList.removeAt(backstackList.lastIndex)
        }
    }

    override fun close() {
        finish()
    }
}
