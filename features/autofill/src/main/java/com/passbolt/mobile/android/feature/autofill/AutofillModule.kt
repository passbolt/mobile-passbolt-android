package com.passbolt.mobile.android.feature.autofill

import android.app.NotificationManager
import android.content.Context
import android.os.PowerManager
import android.view.WindowManager
import android.view.autofill.AutofillManager
import com.passbolt.mobile.android.common.autofill.AutofillInformationProvider
import com.passbolt.mobile.android.core.networking.DEFAULT_HTTP_CLIENT
import com.passbolt.mobile.android.feature.autofill.accessibility.AccessibilityOperationsProvider
import com.passbolt.mobile.android.feature.autofill.accessibility.notification.AccessibilityServiceNotificationFactory
import com.passbolt.mobile.android.feature.autofill.accessibility.notification.NotificationChannelManager
import com.passbolt.mobile.android.feature.autofill.encourage.encourageAutofillModule
import com.passbolt.mobile.android.feature.autofill.resources.DomainProvider
import com.passbolt.mobile.android.feature.autofill.resources.FetchAndUpdateDatabaseUseCase
import com.passbolt.mobile.android.feature.autofill.resources.autofillResourcesModule
import com.passbolt.mobile.android.feature.autofill.service.RestServiceProvider
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.converter.gson.GsonConverterFactory

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
    autofillResourcesModule()
    factory { androidContext().getSystemService(AutofillManager::class.java) }
    factory { androidContext().getSystemService(NotificationManager::class.java) }
    factory {
        AutofillInformationProvider(
            autofillManager = get()
        )
    }
    single {
        RestServiceProvider(
            client = get(named(DEFAULT_HTTP_CLIENT)),
            converterFactory = GsonConverterFactory.create()
        )
    }
    single {
        AccessibilityOperationsProvider()
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
}
