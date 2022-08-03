package com.passbolt.mobile.android.initializers

import android.content.Context
import androidx.startup.Initializer
import com.passbolt.mobile.android.appModule
import com.passbolt.mobile.android.common.commonModule
import com.passbolt.mobile.android.core.commonfolders.commonFoldersModule
import com.passbolt.mobile.android.core.commongroups.commonGroupsModule
import com.passbolt.mobile.android.core.commonresource.commonResourceModule
import com.passbolt.mobile.android.core.logger.loggerModule
import com.passbolt.mobile.android.core.mvp.mvpModule
import com.passbolt.mobile.android.core.networking.networkingModule
import com.passbolt.mobile.android.core.qrscan.barcodeScanModule
import com.passbolt.mobile.android.core.qrscan.di.cameraScanModule
import com.passbolt.mobile.android.core.security.securityModule
import com.passbolt.mobile.android.core.users.usersModule
import com.passbolt.mobile.android.data.dataModule
import com.passbolt.mobile.android.database.databaseModule
import com.passbolt.mobile.android.feature.accountdetails.accountDetailsModule
import com.passbolt.mobile.android.feature.authenticationModule
import com.passbolt.mobile.android.feature.autofill.autofillModule
import com.passbolt.mobile.android.feature.home.homeModule
import com.passbolt.mobile.android.feature.main.mainModule
import com.passbolt.mobile.android.feature.resources.resourcesModule
import com.passbolt.mobile.android.feature.secrets.secretsModule
import com.passbolt.mobile.android.feature.settings.settingsModule
import com.passbolt.mobile.android.feature.setup.setupModule
import com.passbolt.mobile.android.feature.startup.di.startUpModule
import com.passbolt.mobile.android.featureflags.featureFlagsModule
import com.passbolt.mobile.android.gopenpgp.di.openPgpModule
import com.passbolt.mobile.android.mappersModule
import com.passbolt.mobile.android.passboltapi.passboltApiModule
import com.passbolt.mobile.android.service.linksApiModule
import com.passbolt.mobile.android.storage.storageModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

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
 * Initializes the dependency injection framework.
 */
@Suppress("unused")
class KoinInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        startKoin {
            androidContext(context)
            modules(
                appModule,
                openPgpModule,
                setupModule,
                mappersModule,
                mvpModule,
                networkingModule,
                barcodeScanModule,
                cameraScanModule,
                storageModule,
                passboltApiModule,
                autofillModule,
                authenticationModule,
                homeModule,
                settingsModule,
                startUpModule,
                resourcesModule,
                featureFlagsModule,
                databaseModule,
                secretsModule,
                commonResourceModule,
                securityModule,
                linksApiModule,
                usersModule,
                loggerModule,
                accountDetailsModule,
                commonFoldersModule,
                mainModule,
                commonGroupsModule,
                commonModule,
                dataModule
            )
        }
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> =
        mutableListOf()
}
