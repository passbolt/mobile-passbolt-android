package com.passbolt.mobile.android

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import coil.ImageLoader
import com.google.gson.GsonBuilder
import com.passbolt.mobile.android.common.HttpsVerifier
import com.passbolt.mobile.android.common.InitialsProvider
import com.passbolt.mobile.android.common.TimeProvider
import com.passbolt.mobile.android.common.UuidProvider
import com.passbolt.mobile.android.common.WebsiteOpener
import com.passbolt.mobile.android.core.networking.COIL_HTTP_CLIENT
import com.passbolt.mobile.android.core.networking.DEFAULT_HTTP_CLIENT
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
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

internal val appModule = module {
    single {
        ImageLoader.Builder(androidContext())
            .okHttpClient(okHttpClient = get(named(DEFAULT_HTTP_CLIENT)))
            .build()
    }
    factory {
        ContextCompat.getMainExecutor(androidContext())
    }
    factory(named<ProcessLifecycleOwner>()) {
        ProcessLifecycleOwner.get()
    }
    single {
        GsonBuilder().create()
    }
    single { TimeProvider() }
    single { UuidProvider() }
    single {
        provideImageLoader(
            okHttpClient = get(named(COIL_HTTP_CLIENT)),
            androidContext()
        )
    }
    single { InitialsProvider() }
    single { WebsiteOpener() }
    single { HttpsVerifier() }
}

private fun provideImageLoader(okHttpClient: OkHttpClient, context: Context) {
    ImageLoader.Builder(context)
        .okHttpClient(okHttpClient)
        .build()
}
