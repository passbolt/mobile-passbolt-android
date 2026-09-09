package com.passbolt.mobile.android.domain.secrets

import com.passbolt.mobile.android.domain.secrets.offline.OfflineSessionState
import com.passbolt.mobile.android.domain.secrets.offline.OfflineSyncTracker
import com.passbolt.mobile.android.domain.secrets.parser.SecretParser
import com.passbolt.mobile.android.domain.secrets.usecase.decrypt.DecryptSecretUseCase
import com.passbolt.mobile.android.domain.secrets.usecase.decrypt.FetchSecretUseCase
import com.passbolt.mobile.android.domain.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.domain.secrets.usecase.offline.ClearOfflineCacheUseCase
import com.passbolt.mobile.android.domain.secrets.usecase.offline.ClearOfflineSecretsUseCase
import com.passbolt.mobile.android.domain.secrets.usecase.offline.GetOfflineCacheStatusUseCase
import com.passbolt.mobile.android.domain.secrets.usecase.offline.IsResourceMarkedOfflineUseCase
import com.passbolt.mobile.android.domain.secrets.usecase.offline.MarkResourceOfflineUseCase
import com.passbolt.mobile.android.domain.secrets.usecase.offline.OfflineSecretsSyncInteractor
import com.passbolt.mobile.android.domain.secrets.usecase.offline.OfflineSignInGate
import com.passbolt.mobile.android.domain.secrets.usecase.offline.SetOfflineModeUseCase
import com.passbolt.mobile.android.domain.secrets.usecase.offline.UnmarkResourceOfflineUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val secretsDomainModule =
    module {
        singleOf(::FetchSecretUseCase)
        singleOf(::DecryptSecretUseCase)
        singleOf(::SecretInteractor)
        singleOf(::SecretParser)
        // offline mode
        singleOf(::OfflineSessionState)
        singleOf(::OfflineSyncTracker)
        singleOf(::OfflineSecretsSyncInteractor)
        singleOf(::OfflineSignInGate)
        singleOf(::MarkResourceOfflineUseCase)
        singleOf(::UnmarkResourceOfflineUseCase)
        singleOf(::IsResourceMarkedOfflineUseCase)
        singleOf(::SetOfflineModeUseCase)
        singleOf(::ClearOfflineCacheUseCase)
        singleOf(::ClearOfflineSecretsUseCase)
        singleOf(::GetOfflineCacheStatusUseCase)
    }
