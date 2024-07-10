package com.passbolt.mobile.android.storage.usecase.featureflags

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.entity.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.storage.encrypted.EncryptedSharedPreferencesFactory
import com.passbolt.mobile.android.storage.paths.FeatureFlagsFileName
import com.passbolt.mobile.android.storage.usecase.SelectedAccountUseCase
import com.passbolt.mobile.android.storage.usecase.featureflags.Constants.FOLDERS_KEY
import com.passbolt.mobile.android.storage.usecase.featureflags.Constants.PASSWORD_EXPIRY_KEY
import com.passbolt.mobile.android.storage.usecase.featureflags.Constants.PREVIEW_PASSWORD_KEY
import com.passbolt.mobile.android.storage.usecase.featureflags.Constants.PRIVACY_POLICY_KEY
import com.passbolt.mobile.android.storage.usecase.featureflags.Constants.RBAC_KEY
import com.passbolt.mobile.android.storage.usecase.featureflags.Constants.TAGS_KEY
import com.passbolt.mobile.android.storage.usecase.featureflags.Constants.TERMS_AND_CONDITIONS_KEY
import com.passbolt.mobile.android.storage.usecase.featureflags.Constants.TOTP_KEY

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
class GetFeatureFlagsUseCase(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory
) : AsyncUseCase<Unit, GetFeatureFlagsUseCase.Output>, SelectedAccountUseCase {

    override suspend fun execute(input: Unit): Output {
        val fileName = FeatureFlagsFileName(selectedAccountId).name
        encryptedSharedPreferencesFactory.get("$fileName.xml").let {
            val privacyPolicyUrl = it.getString(PRIVACY_POLICY_KEY, null)
            val termsUrl = it.getString(TERMS_AND_CONDITIONS_KEY, null)
            val previewPasswordAvailable = it.getBoolean(PREVIEW_PASSWORD_KEY, Defaults.IS_PREVIEW_PASSWORD_AVAILABLE)
            val areFoldersAvailable = it.getBoolean(FOLDERS_KEY, Defaults.ARE_FOLDERS_AVAILABLE)
            val areTagsAvailable = it.getBoolean(TAGS_KEY, Defaults.ARE_TAGS_AVAILABLE)
            val isTotpAvailable = it.getBoolean(TOTP_KEY, Defaults.IS_TOTP_AVAILABLE)
            val isRbacAvailable = it.getBoolean(RBAC_KEY, Defaults.IS_RBAC_AVAILABLE)
            val isPasswordExpiryAvailable = it.getBoolean(PASSWORD_EXPIRY_KEY, Defaults.IS_PASSWORD_EXPIRY_AVAILABLE)
            val arePasswordPoliciesAvailable = it.getBoolean(
                Constants.PASSWORD_POLICIES_KEY,
                Defaults.ARE_PASSWORD_POLICIES_AVAILABLE
            )
            val canUpdatePasswordPolicies = it.getBoolean(
                Constants.PASSWORD_POLICIES_UPDATE_KEY,
                Defaults.CAN_UPDATE_PASSWORD_POLICIES
            )
            return Output(
                FeatureFlagsModel(
                    privacyPolicyUrl = privacyPolicyUrl,
                    termsAndConditionsUrl = termsUrl,
                    isPreviewPasswordAvailable = previewPasswordAvailable,
                    areFoldersAvailable = areFoldersAvailable,
                    areTagsAvailable = areTagsAvailable,
                    isTotpAvailable = isTotpAvailable,
                    isRbacAvailable = isRbacAvailable,
                    isPasswordExpiryAvailable = isPasswordExpiryAvailable,
                    arePasswordPoliciesAvailable = arePasswordPoliciesAvailable,
                    canUpdatePasswordPolicies = canUpdatePasswordPolicies
                )
            )
        }
    }

    data class Output(val featureFlags: FeatureFlagsModel)
}
