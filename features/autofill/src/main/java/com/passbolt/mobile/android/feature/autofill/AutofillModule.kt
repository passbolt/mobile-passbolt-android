package com.passbolt.mobile.android.feature.autofill

import android.app.NotificationManager
import android.content.Context
import android.os.PowerManager
import android.view.WindowManager
import android.view.autofill.AutofillManager
import com.passbolt.mobile.android.common.ResourceDimenProvider
import com.passbolt.mobile.android.feature.autofill.accessibility.AccessibilityOperationsProvider
import com.passbolt.mobile.android.feature.autofill.accessibility.notification.AccessibilityServiceNotificationFactory
import com.passbolt.mobile.android.feature.autofill.accessibility.notification.NotificationChannelManager
import com.passbolt.mobile.android.feature.autofill.autofill.AutofillHintsFactory
import com.passbolt.mobile.android.feature.autofill.autofill.FillableInputsFinder
import com.passbolt.mobile.android.feature.autofill.autofill.RemoteViewsFactory
import com.passbolt.mobile.android.feature.autofill.encourage.accessibility.accessibilityAutofillModule
import com.passbolt.mobile.android.feature.autofill.encourage.autofill.encourageAutofillModule
import com.passbolt.mobile.android.feature.autofill.encourage.tutorial.SettingsNavigator
import com.passbolt.mobile.android.feature.autofill.resources.DomainProvider
import com.passbolt.mobile.android.feature.autofill.resources.FetchAndUpdateDatabaseUseCase
import com.passbolt.mobile.android.feature.autofill.resources.autofillResourcesModule
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

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

val autofillModule = module {
    encourageAutofillModule()
    accessibilityAutofillModule()
    autofillResourcesModule()
    factory { androidContext().getSystemService(AutofillManager::class.java) }
    factory { androidContext().getSystemService(NotificationManager::class.java) }
    factory {
        AutofillInformationProvider(
            autofillManager = get(),
            context = androidContext()
        )
    }

    single {
        AccessibilityOperationsProvider(
            resourceDimenProvider = get()
        )
    }
    single {
        SettingsNavigator()
    }
    single {
        FetchAndUpdateDatabaseUseCase(
            getSelectedAccountUseCase = get(),
            removeLocalResourcesUseCase = get(),
            addLocalResourcesUseCase = get()
        )
    }
    single {
        DomainProvider()
    }
    single {
        androidApplication().getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    single {
        androidApplication().getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    single {
        AccessibilityServiceNotificationFactory(
            notificationChannelManager = get()
        )
    }
    single {
        NotificationChannelManager(
            notificationManager = get()
        )
    }
    single {
        ResourceDimenProvider(
            androidApplication().resources
        )
    }
    factory {
        RemoteViewsFactory(
            appContext = androidContext()
        )
    }
    factory {
        FillableInputsFinder(
            autofillHintsFactory = get()
        )
    }
    factory {
        AutofillHintsFactory(
            resources = get()
        )
    }
}
