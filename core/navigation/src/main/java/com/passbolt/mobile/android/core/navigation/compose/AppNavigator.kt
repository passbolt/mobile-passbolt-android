package com.passbolt.mobile.android.core.navigation.compose

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider.getUriForFile
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.passbolt.mobile.android.common.ExternalDeeplinkHandler
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.ActivityIntents.AuthConfig.ManageAccount
import com.passbolt.mobile.android.core.navigation.ActivityIntents.AuthConfig.Startup
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.AccountDetails
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.AuthenticationManageAccounts
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.AuthenticationStartUp
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.Home
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.Start
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.TransferAccount
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import java.io.File

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
class AppNavigator(
    private val externalDeeplinkHandler: ExternalDeeplinkHandler,
) : KoinComponent {
    private var _backStack: NavBackStack<NavKey>? = null
    var backStack: NavBackStack<NavKey>
        get() = requireNotNull(_backStack) { "backStack has not been initialized" }
        set(value) {
            _backStack = value
            _currentBackStackItem.value = value.lastOrNull()
        }

    private val _currentBackStackItem = MutableStateFlow<NavKey?>(null)
    val currentBackStackItem: StateFlow<NavKey?> = _currentBackStackItem.asStateFlow()

    private val pendingNavigationKey = MutableStateFlow<NavKey?>(null)

    private val _tabSwitchRequest = MutableSharedFlow<BottomTab>(extraBufferCapacity = 1)
    val tabSwitchRequest: SharedFlow<BottomTab> = _tabSwitchRequest.asSharedFlow()

    fun setPendingNavigation(key: NavKey) {
        pendingNavigationKey.update { key }
    }

    fun consumePendingNavigation() =
        pendingNavigationKey.value.also {
            pendingNavigationKey.update { null }
        }

    fun requestTabSwitch(tab: BottomTab) {
        _tabSwitchRequest.tryEmit(tab)
    }

    fun navigateToKey(key: NavKey) {
        backStack.add(key)
        _currentBackStackItem.value = key
    }

    fun popToKey(key: NavKey) {
        while (backStack.size > 1 && backStack.last() != key) {
            backStack.removeAt(backStack.lastIndex)
        }
        _currentBackStackItem.value = key
    }

    fun navigateBack(): Any? {
        if (backStack.size <= 1) return null

        val result = backStack.removeLastOrNull()
        _currentBackStackItem.value = backStack.lastOrNull()
        return result
    }

    fun navigateUp(activity: Activity?) {
        activity?.finish()
    }

    fun startNavigationActivity(
        context: Context,
        activity: NavigationActivity,
        vararg flags: Int,
    ) {
        val intent =
            when (activity) {
                is AuthenticationStartUp -> ActivityIntents.authentication(context, Startup, appContext = activity.appContext)
                AuthenticationManageAccounts -> ActivityIntents.authentication(context, ManageAccount)
                TransferAccount -> ActivityIntents.transferAccountToAnotherDevice(context)
                AccountDetails -> ActivityIntents.accountDetails(context)
                Home -> ActivityIntents.home(context)
                Start -> ActivityIntents.start(context)
            }

        flags.forEach { intent.addFlags(it) }

        context.startActivity(intent)
    }

    fun startFileShareSheet(
        context: Context,
        authority: String,
        filePath: String,
        fileMimeType: String,
        shareSheetTitle: String,
    ) {
        val contentUri = getUriForFile(context, authority, File(filePath))

        val sendIntent =
            Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                type = fileMimeType

                putExtra(Intent.EXTRA_STREAM, contentUri)
            }

        context.startActivity(Intent.createChooser(sendIntent, shareSheetTitle))
    }

    fun openExternalWebsite(
        context: Context,
        url: String,
    ) {
        externalDeeplinkHandler.openWebsite(
            context = context,
            url = url,
        )
    }

    fun openChromeNativeAutofillSettings(context: Context) {
        externalDeeplinkHandler.openChromeNativeAutofillSettings(context)
    }

    fun startTextShareSheet(
        context: Context,
        text: String,
        shareSheetTitle: String,
    ) {
        val sendIntent: Intent =
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }

        val shareIntent =
            Intent.createChooser(
                sendIntent,
                shareSheetTitle,
            )
        context.startActivity(shareIntent)
    }

    fun openAppOsSettings(context: Context) {
        externalDeeplinkHandler.openAppOsSettings(context)
    }
}
