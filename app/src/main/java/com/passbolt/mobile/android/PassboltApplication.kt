package com.passbolt.mobile.android

import android.app.Application
import coil.Coil
import coil.ImageLoader
import com.passbolt.mobile.android.core.mvp.di.mvpModule
import com.passbolt.mobile.android.core.networking.di.networkingModule
import com.passbolt.mobile.android.core.qrscan.di.barcodeScanModule
import com.passbolt.mobile.android.core.qrscan.di.cameraScanModule
import com.passbolt.mobile.android.di.appModule
import com.passbolt.mobile.android.feature.autofill.autofillModule
import com.passbolt.mobile.android.feature.setup.setupModule
import com.passbolt.mobile.android.gopenpgp.di.openPgpModule
import com.passbolt.mobile.android.service.registration.di.passboltApiModule
import com.passbolt.mobile.android.storage.di.storageModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import timber.log.Timber

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

class PassboltApplication : Application(), KoinComponent {

    private val imageLoader: ImageLoader by inject()

    override fun onCreate() {
        super.onCreate()
        initTimber()
        initKoin()
        setupCoil()
    }

    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun setupCoil() {
        // TODO remove in production version - PAS-105
        Coil.setImageLoader(imageLoader)
    }

    private fun initKoin() {
        startKoin {
            androidContext(this@PassboltApplication)
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
                autofillModule
            )
        }
    }
}
