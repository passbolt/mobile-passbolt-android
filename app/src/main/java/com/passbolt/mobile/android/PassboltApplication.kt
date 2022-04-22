package com.passbolt.mobile.android

import android.app.Application
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppForegroundListener
import com.passbolt.mobile.android.core.navigation.isAuthenticated
import com.passbolt.mobile.android.core.security.runtimeauth.RuntimeAuthenticatedFlag
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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
/**
 * The main entry point for the Passbolt Android application.
 * Contains code for initialization of the main components.
 *
 * @property appForegroundListener listener detecting when the app goes foreground
 * @property applicationScope coroutine scope for the application class
 */
class PassboltApplication : Application(), KoinComponent {

    private val appForegroundListener: AppForegroundListener by inject()
    private val applicationScope = MainScope()
    private val runtimeAuthenticatedFlag: RuntimeAuthenticatedFlag by inject()

    override fun onCreate() {
        super.onCreate()
        registerAppForegroundListener()
    }

    /**
     * Registers a listener detecting when application goes foreground.
     * Listening is done using the application coroutine scope.
     */
    private fun registerAppForegroundListener() {
        registerActivityLifecycleCallbacks(appForegroundListener)
        applicationScope.launch {
            runtimeAuthenticatedFlag.isAuthenticated = false
            appForegroundListener.appWentForegroundFlow.collect {
                if (it.isAuthenticated()) {
                    it.startActivity(
                        ActivityIntents.authentication(
                            it,
                            ActivityIntents.AuthConfig.RefreshSession
                        )
                    )
                }
            }
        }
    }
}
