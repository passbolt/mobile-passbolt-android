package com.passbolt.mobile.android.mappers

import com.passbolt.mobile.android.dto.response.SettingsResponseDto
import com.passbolt.mobile.android.entity.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.storage.usecase.featureflags.Defaults

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
class FeatureFlagsMapper {

    fun map(settingsResponseDto: SettingsResponseDto): FeatureFlagsModel =
        settingsResponseDto.passboltSettings.let {
            return FeatureFlagsModel(
                it.legalSettings.privacyPolicyUrl.url,
                it.legalSettings.termsAndConditionsUrl.url,
                isPreviewPasswordAvailable = it.plugins.previewPassword?.enabled
                    ?: Defaults.IS_PREVIEW_PASSWORD_AVAILABLE,
                areFoldersAvailable = it.plugins.folders?.enabled
                    ?: Defaults.ARE_FOLDERS_AVAILABLE,
                areTagsAvailable = it.plugins.tags?.enabled
                    ?: Defaults.ARE_TAGS_AVAILABLE,
                isTotpAvailable = it.plugins.totpResourceTypes?.enabled
                    ?: Defaults.IS_TOTP_AVAILABLE,
                isRbacAvailable = it.plugins.rbacs?.enabled
                    ?: Defaults.IS_RBAC_AVAILABLE,
                isPasswordExpiryAvailable = it.plugins.passwordExpiry?.enabled
                    ?: Defaults.IS_PASSWORD_EXPIRY_AVAILABLE,
                arePasswordPoliciesAvailable = it.plugins.passwordPolicies?.enabled
                    ?: Defaults.ARE_PASSWORD_POLICIES_AVAILABLE,
                canUpdatePasswordPolicies = it.plugins.passwordPoliciesUpdate?.enabled
                    ?: Defaults.CAN_UPDATE_PASSWORD_POLICIES,
                isV5MetadataAvailable = it.plugins.metadata?.enabled
                    ?: Defaults.IS_V5_METADATA_AVAILABLE
            )
        }
}
