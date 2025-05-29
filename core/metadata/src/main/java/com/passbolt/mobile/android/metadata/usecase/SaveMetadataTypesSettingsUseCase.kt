package com.passbolt.mobile.android.metadata.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.encryptedstorage.EncryptedSharedPreferencesFactory
import com.passbolt.mobile.android.metadata.usecase.MetadataTypesStorageConstants.ALLOW_CREATION_OF_V4_FOLDERS
import com.passbolt.mobile.android.metadata.usecase.MetadataTypesStorageConstants.ALLOW_CREATION_OF_V4_RESOURCES
import com.passbolt.mobile.android.metadata.usecase.MetadataTypesStorageConstants.ALLOW_CREATION_OF_V4_TAGS
import com.passbolt.mobile.android.metadata.usecase.MetadataTypesStorageConstants.ALLOW_CREATION_OF_V5_FOLDERS
import com.passbolt.mobile.android.metadata.usecase.MetadataTypesStorageConstants.ALLOW_CREATION_OF_V5_RESOURCES
import com.passbolt.mobile.android.metadata.usecase.MetadataTypesStorageConstants.ALLOW_CREATION_OF_V5_TAGS
import com.passbolt.mobile.android.metadata.usecase.MetadataTypesStorageConstants.ALLOW_V4_V5_UPGRADE
import com.passbolt.mobile.android.metadata.usecase.MetadataTypesStorageConstants.ALLOW_V5_V4_DOWNGRADE
import com.passbolt.mobile.android.metadata.usecase.MetadataTypesStorageConstants.DEFAULT_FOLDER_TYPE
import com.passbolt.mobile.android.metadata.usecase.MetadataTypesStorageConstants.DEFAULT_METADATA_TYPE
import com.passbolt.mobile.android.metadata.usecase.MetadataTypesStorageConstants.DEFAULT_TAG_TYPE
import com.passbolt.mobile.android.ui.MetadataTypesSettingsModel

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
class SaveMetadataTypesSettingsUseCase(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
) : AsyncUseCase<SaveMetadataTypesSettingsUseCase.Input, Unit>,
    com.passbolt.mobile.android.core.accounts.usecase.SelectedAccountUseCase {
    override suspend fun execute(input: Input) {
        val fileName = MetadataSettingsFileName(selectedAccountId).name
        val sharedPreferences = encryptedSharedPreferencesFactory.get("$fileName.xml")
        with(sharedPreferences.edit()) {
            putString(
                DEFAULT_METADATA_TYPE,
                input.metadataTypesSettingsModel.defaultMetadataType.name,
            )
            putString(
                DEFAULT_FOLDER_TYPE,
                input.metadataTypesSettingsModel.defaultFolderType.name,
            )
            putString(
                DEFAULT_TAG_TYPE,
                input.metadataTypesSettingsModel.defaultTagType.name,
            )
            putBoolean(
                ALLOW_CREATION_OF_V5_RESOURCES,
                input.metadataTypesSettingsModel.allowCreationOfV5Resources,
            )
            putBoolean(
                ALLOW_CREATION_OF_V5_FOLDERS,
                input.metadataTypesSettingsModel.allowCreationOfV5Folders,
            )
            putBoolean(
                ALLOW_CREATION_OF_V5_TAGS,
                input.metadataTypesSettingsModel.allowCreationOfV5Tags,
            )
            putBoolean(
                ALLOW_CREATION_OF_V4_RESOURCES,
                input.metadataTypesSettingsModel.allowCreationOfV4Resources,
            )
            putBoolean(
                ALLOW_CREATION_OF_V4_FOLDERS,
                input.metadataTypesSettingsModel.allowCreationOfV4Folders,
            )
            putBoolean(
                ALLOW_CREATION_OF_V4_TAGS,
                input.metadataTypesSettingsModel.allowCreationOfV4Tags,
            )
            putBoolean(
                ALLOW_V4_V5_UPGRADE,
                input.metadataTypesSettingsModel.allowV4V5Upgrade,
            )
            putBoolean(
                ALLOW_V5_V4_DOWNGRADE,
                input.metadataTypesSettingsModel.allowV5V4Downgrade,
            )
            apply()
        }
    }

    data class Input(
        val metadataTypesSettingsModel: MetadataTypesSettingsModel,
    )
}
