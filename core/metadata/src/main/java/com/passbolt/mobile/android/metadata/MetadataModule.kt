package com.passbolt.mobile.android.metadata

import com.passbolt.mobile.android.metadata.interactor.MetadataKeysInteractor
import com.passbolt.mobile.android.metadata.interactor.MetadataKeysSettingsInteractor
import com.passbolt.mobile.android.metadata.interactor.MetadataPrivateKeysHelperInteractor
import com.passbolt.mobile.android.metadata.interactor.MetadataPrivateKeysInteractor
import com.passbolt.mobile.android.metadata.interactor.MetadataSessionKeysInteractor
import com.passbolt.mobile.android.metadata.interactor.MetadataTypesSettingsInteractor
import com.passbolt.mobile.android.metadata.privatekeys.MetadataPrivateKeysValidator
import com.passbolt.mobile.android.metadata.sessionkeys.SessionKeysBundleMerger
import com.passbolt.mobile.android.metadata.sessionkeys.SessionKeysBundleValidator
import com.passbolt.mobile.android.metadata.sessionkeys.SessionKeysMemoryCache
import com.passbolt.mobile.android.metadata.usecase.DeleteTrustedMetadataKeyUseCase
import com.passbolt.mobile.android.metadata.usecase.FetchMetadataKeysSettingsUseCase
import com.passbolt.mobile.android.metadata.usecase.FetchMetadataKeysUseCase
import com.passbolt.mobile.android.metadata.usecase.FetchMetadataSessionKeysUseCase
import com.passbolt.mobile.android.metadata.usecase.FetchMetadataTypesSettingsUseCase
import com.passbolt.mobile.android.metadata.usecase.GetTrustedMetadataKeyUseCase
import com.passbolt.mobile.android.metadata.usecase.PostMetadataSessionKeysUseCase
import com.passbolt.mobile.android.metadata.usecase.SaveTrustedMetadataKeyUseCase
import com.passbolt.mobile.android.metadata.usecase.UpdateMetadataPrivateKeyUseCase
import com.passbolt.mobile.android.metadata.usecase.UpdateMetadataSessionKeysUseCase
import com.passbolt.mobile.android.metadata.usecase.db.AddLocalMetadataKeysUseCase
import com.passbolt.mobile.android.metadata.usecase.db.GetLocalMetadataKeyUseCase
import com.passbolt.mobile.android.metadata.usecase.db.GetLocalMetadataKeysUseCase
import com.passbolt.mobile.android.metadata.usecase.db.RebuildMetadataKeysTablesUseCase
import com.passbolt.mobile.android.metadata.usecase.db.RemoveLocalMetadataKeysUseCase
import com.passbolt.mobile.android.metadata.usecase.metadataSettingsModule
import org.koin.core.module.dsl.singleOf
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

val metadataModule = module {
    metadataSettingsModule()

    singleOf(::FetchMetadataKeysUseCase)
    singleOf(::MetadataKeysInteractor)
    singleOf(::AddLocalMetadataKeysUseCase)
    singleOf(::GetLocalMetadataKeysUseCase)
    singleOf(::GetLocalMetadataKeyUseCase)
    singleOf(::RebuildMetadataKeysTablesUseCase)
    singleOf(::RemoveLocalMetadataKeysUseCase)
    singleOf(::FetchMetadataTypesSettingsUseCase)
    singleOf(::MetadataTypesSettingsInteractor)
    singleOf(::FetchMetadataKeysUseCase)
    singleOf(::FetchMetadataKeysSettingsUseCase)
    singleOf(::MetadataKeysSettingsInteractor)
    singleOf(::FetchMetadataSessionKeysUseCase)
    singleOf(::SessionKeysBundleMerger)
    singleOf(::SessionKeysMemoryCache)
    singleOf(::SessionKeysBundleValidator)
    singleOf(::MetadataPrivateKeysValidator)
    singleOf(::PostMetadataSessionKeysUseCase)
    singleOf(::UpdateMetadataSessionKeysUseCase)
    single {
        MetadataSessionKeysInteractor(
            fetchMetadataSessionKeysUseCase = get(),
            postMetadataSessionKeysUseCase = get(),
            updateMetadataSessionKeysUseCase = get(),
            passphraseMemoryCache = get(),
            getPrivateKeyUseCase = get(),
            openPgp = get(),
            sessionKeysBundleMerger = get(),
            sessionKeysMemoryCache = get(),
            metadataMapper = get(),
            gson = get(),
            sessionKeysBundleValidator = get()
        )
    }
    singleOf(::UpdateMetadataPrivateKeyUseCase)
    singleOf(::GetTrustedMetadataKeyUseCase)
    singleOf(::SaveTrustedMetadataKeyUseCase)
    singleOf(::DeleteTrustedMetadataKeyUseCase)
    singleOf(::MetadataPrivateKeysInteractor)
    singleOf(::MetadataPrivateKeysHelperInteractor)
}
