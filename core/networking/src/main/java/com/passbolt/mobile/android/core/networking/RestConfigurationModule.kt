package com.passbolt.mobile.android.core.networking

import coil.util.CoilUtils
import com.passbolt.mobile.android.common.userid.UserIdProvider
import com.passbolt.mobile.android.core.networking.interceptor.ChangeableBaseUrlInterceptor
import com.passbolt.mobile.android.core.networking.usecase.GetBaseUrlUseCase
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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
val networkingModule = module {
    single { provideLoggingInterceptor() }
    single(named(DEFAULT_HTTP_CLIENT)) {
        provideHttpClient(
            loggingInterceptor = get(),
            changeableBaseUrlInterceptor = get()
        )
    }
    single(named(COIL_HTTP_CLIENT)) {
        provideCoilHttpClient(
            loggingInterceptor = get(),
            changeableBaseUrlInterceptor = get(),
            cache = get()
        )
    }
    single { ChangeableBaseUrlInterceptor(getBaseUrlUseCase = get()) }
    single {
        GetBaseUrlUseCase(
            getAccountDataUseCase = get(),
            getSelectedAccountUseCase = get()
        )
    }
    single {
        RetrofitRestService(
            client = get(),
            converterFactory = get()
        )
    }
    single { UserIdProvider() }
    single {
        ResponseHandler(
            errorHeaderMapper = get()
        )
    }
    single {
        ErrorHeaderMapper(
            gson = get(),
            context = get()
        )
    }
    single { CoilUtils.createDefaultCache(androidContext()) }
}

private fun provideLoggingInterceptor(): HttpLoggingInterceptor {
    return HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
}

private fun provideHttpClient(
    loggingInterceptor: HttpLoggingInterceptor,
    changeableBaseUrlInterceptor: ChangeableBaseUrlInterceptor
): OkHttpClient {
    val builder = OkHttpClient.Builder()
    builder.addNetworkInterceptor(loggingInterceptor)
    builder.addInterceptor(changeableBaseUrlInterceptor)
    // TODO remove in production version - PAS-105
    builder.hostnameVerifier { _, _ -> true }
    return builder.build()
}

private fun provideCoilHttpClient(
    loggingInterceptor: HttpLoggingInterceptor,
    changeableBaseUrlInterceptor: ChangeableBaseUrlInterceptor,
    cache: Cache
): OkHttpClient {
    val builder = OkHttpClient.Builder()
    builder.addNetworkInterceptor(loggingInterceptor)
    builder.addInterceptor(changeableBaseUrlInterceptor)
    // TODO remove in production version - PAS-105
    builder.hostnameVerifier { _, _ -> true }
    builder.cache(cache)
    return builder.build()
}

const val DEFAULT_HTTP_CLIENT = "DEFAULT_HTTP_CLIENT"
const val COIL_HTTP_CLIENT = "COIL_HTTP_CLIENT"
