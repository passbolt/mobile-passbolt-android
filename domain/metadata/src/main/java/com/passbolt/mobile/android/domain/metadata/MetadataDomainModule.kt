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

package com.passbolt.mobile.android.domain.metadata

import com.passbolt.mobile.android.domain.metadata.interactor.MetadataKeysInteractor
import com.passbolt.mobile.android.domain.metadata.interactor.MetadataKeysSettingsInteractor
import com.passbolt.mobile.android.domain.metadata.interactor.MetadataPrivateKeysHelperInteractor
import com.passbolt.mobile.android.domain.metadata.interactor.MetadataPrivateKeysInteractor
import com.passbolt.mobile.android.domain.metadata.interactor.MetadataSessionKeysInteractor
import com.passbolt.mobile.android.domain.metadata.interactor.MetadataTypesSettingsInteractor
import com.passbolt.mobile.android.domain.metadata.interactor.ResourceAccessInteractor
import com.passbolt.mobile.android.domain.metadata.keys.MetadataKeysValidator
import com.passbolt.mobile.android.domain.metadata.sessionkeys.SessionKeysBundleMerger
import com.passbolt.mobile.android.domain.metadata.sessionkeys.SessionKeysBundleProcessor
import com.passbolt.mobile.android.domain.metadata.sessionkeys.SessionKeysBundleValidator
import com.passbolt.mobile.android.domain.metadata.sessionkeys.SessionKeysMemoryCache
import com.passbolt.mobile.android.domain.metadata.usecase.CanCreateResourceUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.CanShareResourceUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.DeleteTrustedMetadataKeyUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.FetchMetadataKeysSettingsUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.FetchMetadataKeysUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.FetchMetadataSessionKeysUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.FetchMetadataTypesSettingsUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.GetMetadataKeysSettingsUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.GetMetadataTypesSettingsUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.GetTrustedMetadataKeyUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.PostMetadataSessionKeysUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.SaveMetadataKeysSettingsUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.SaveMetadataTypesSettingsUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.SaveTrustedMetadataKeyUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.UpdateMetadataPrivateKeyUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.UpdateMetadataSessionKeysUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.db.GetLocalMetadataKeyUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.db.GetLocalMetadataKeysUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.db.RebuildMetadataKeysTablesUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val metadataDomainModule =
    module {
        singleOf(::GetMetadataTypesSettingsUseCase)
        singleOf(::SaveMetadataTypesSettingsUseCase)
        singleOf(::GetMetadataKeysSettingsUseCase)
        singleOf(::SaveMetadataKeysSettingsUseCase)

        singleOf(::FetchMetadataKeysUseCase)
        singleOf(::GetLocalMetadataKeysUseCase)
        singleOf(::GetLocalMetadataKeyUseCase)
        singleOf(::RebuildMetadataKeysTablesUseCase)
        singleOf(::MetadataKeysInteractor)

        singleOf(::FetchMetadataTypesSettingsUseCase)
        singleOf(::MetadataTypesSettingsInteractor)
        singleOf(::FetchMetadataKeysSettingsUseCase)
        singleOf(::MetadataKeysSettingsInteractor)

        singleOf(::FetchMetadataSessionKeysUseCase)
        singleOf(::PostMetadataSessionKeysUseCase)
        singleOf(::UpdateMetadataSessionKeysUseCase)
        singleOf(::SessionKeysBundleMerger)
        singleOf(::SessionKeysMemoryCache)
        singleOf(::SessionKeysBundleValidator)
        singleOf(::SessionKeysBundleProcessor)
        singleOf(::MetadataSessionKeysInteractor)

        singleOf(::MetadataKeysValidator)
        singleOf(::UpdateMetadataPrivateKeyUseCase)
        singleOf(::GetTrustedMetadataKeyUseCase)
        singleOf(::SaveTrustedMetadataKeyUseCase)
        singleOf(::DeleteTrustedMetadataKeyUseCase)
        singleOf(::MetadataPrivateKeysInteractor)
        singleOf(::MetadataPrivateKeysHelperInteractor)

        singleOf(::CanCreateResourceUseCase)
        singleOf(::CanShareResourceUseCase)
        singleOf(::ResourceAccessInteractor)
    }
