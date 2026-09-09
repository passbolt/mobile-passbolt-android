package com.passbolt.mobile.android.data.secrets

import com.passbolt.mobile.android.core.networking.RestService
import com.passbolt.mobile.android.data.secrets.datasource.remote.SecretsRemoteDataSourceImpl
import com.passbolt.mobile.android.data.secrets.datasource.remote.api.OfflineSecretsApi
import com.passbolt.mobile.android.data.secrets.datasource.remote.api.SecretsApi
import com.passbolt.mobile.android.data.secrets.offline.OfflineCacheLocalDataSource
import com.passbolt.mobile.android.data.secrets.offline.OfflineCacheRemoteDataSource
import com.passbolt.mobile.android.data.secrets.offline.OfflineCacheRepositoryImpl
import com.passbolt.mobile.android.domain.secrets.SecretsRepository
import com.passbolt.mobile.android.domain.secrets.datasource.SecretsRemoteDataSource
import com.passbolt.mobile.android.domain.secrets.offline.OfflineCacheRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val secretsDataModule =
    module {
        single { get<RestService>().service(SecretsApi::class.java) }
        singleOf(::SecretsRemoteDataSourceImpl) bind SecretsRemoteDataSource::class
        singleOf(::SecretsRepositoryImpl) bind SecretsRepository::class
        // offline mode
        single { get<RestService>().service(OfflineSecretsApi::class.java) }
        singleOf(::OfflineCacheLocalDataSource)
        singleOf(::OfflineCacheRemoteDataSource)
        singleOf(::OfflineCacheRepositoryImpl) bind OfflineCacheRepository::class
    }
