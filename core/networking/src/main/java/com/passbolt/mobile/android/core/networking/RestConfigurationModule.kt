package com.passbolt.mobile.android.core.networking

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.passbolt.mobile.android.common.CookieExtractor
import com.passbolt.mobile.android.core.networking.interceptor.AuthInterceptor
import com.passbolt.mobile.android.core.networking.interceptor.ChangeableBaseUrlInterceptor
import com.passbolt.mobile.android.core.networking.interceptor.CookiesInterceptor
import com.passbolt.mobile.android.dto.response.ResourceResponseDto
import com.passbolt.mobile.android.dto.response.ResourceTypeDto
import com.passbolt.mobile.android.serializers.gson.ResourceListDeserializer
import com.passbolt.mobile.android.serializers.gson.ResourceTypesListDeserializer
import com.passbolt.mobile.android.serializers.gson.strictTypeAdapters
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.time.Duration

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
            interceptors = listOf(
                get<ChangeableBaseUrlInterceptor>(),
                get<AuthInterceptor>(),
                get<CookiesInterceptor.ReceivedCookiesInterceptor>(),
                get<CookiesInterceptor.AddCookiesInterceptor>()
            )
        )
    }
    single(named(COIL_HTTP_CLIENT)) {
        provideHttpClient(
            loggingInterceptor = get(),
            interceptors = listOf(
                get<ChangeableBaseUrlInterceptor>()
            )
        )
    }
    single { ChangeableBaseUrlInterceptor(getCurrentApiUrlUseCase = get()) }
    single {
        AuthInterceptor(
            getSessionUseCase = get()
        )
    }
    single {
        CookiesInterceptor.ReceivedCookiesInterceptor(
            cookieExtractor = get()
        )
    }
    single {
        CookiesInterceptor.AddCookiesInterceptor()
    }
    single { CookieExtractor() }

    single<RestService> {
        RetrofitRestService(
            client = get(named(DEFAULT_HTTP_CLIENT)),
            converterFactory = GsonConverterFactory.create(
                GsonBuilder()
                    .apply {
                        strictTypeAdapters.forEach {
                            registerTypeAdapter(it.key, it.value)
                        }
                        registerTypeAdapter(
                            object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type,
                            get<ResourceListDeserializer>()
                        )
                        registerTypeAdapter(
                            object : TypeToken<List<@JvmSuppressWildcards ResourceTypeDto>>() {}.type,
                            get<ResourceTypesListDeserializer>()
                        )
                    }
                    .create()
            )
        )
    }
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
}

private fun provideLoggingInterceptor(): HttpLoggingInterceptor {
    return HttpLoggingInterceptor(logger = provideHttpLogger())
        .apply { level = HttpLoggingInterceptor.Level.BASIC }
}

private fun provideHttpLogger(): HttpLoggingInterceptor.Logger = object : HttpLoggingInterceptor.Logger {
    override fun log(message: String) {
        Timber.d(message)
    }
}

private fun provideHttpClient(
    loggingInterceptor: HttpLoggingInterceptor,
    interceptors: List<Interceptor> = emptyList()
) =
    OkHttpClient.Builder()
        .addNetworkInterceptor(loggingInterceptor)
        .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
        .writeTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
        .readTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
        .apply {
            interceptors.forEach { addInterceptor(it) }
        }
        .build()

const val DEFAULT_HTTP_CLIENT = "DEFAULT_HTTP_CLIENT"
const val COIL_HTTP_CLIENT = "COIL_HTTP_CLIENT"
