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

package com.passbolt.mobile.android.data.preferences.datasource.local

import com.passbolt.mobile.android.data.preferences.GLOBAL_PREFERENCES_FILE_NAME
import com.passbolt.mobile.android.data.preferences.KEY_ACCESSIBILITY_POLICIES_CONSENT_GIVEN
import com.passbolt.mobile.android.data.preferences.KEY_API_FETCH_PAGE_SIZE
import com.passbolt.mobile.android.data.preferences.KEY_API_FETCH_PAGE_SIZE_MANUAL
import com.passbolt.mobile.android.data.preferences.KEY_COPY_TOTP_ON_AUTOFILL_ENABLED
import com.passbolt.mobile.android.data.preferences.KEY_DEBUG_LOGS_ENABLED
import com.passbolt.mobile.android.data.preferences.KEY_DEBUG_LOGS_FILE_CREATION_DATE_TIME
import com.passbolt.mobile.android.data.preferences.KEY_DEBUG_LOGS_LAST_APP_VERSION
import com.passbolt.mobile.android.data.preferences.KEY_IS_AUTH_REQUIRED_ON_EVERY_ENTRY
import com.passbolt.mobile.android.data.preferences.KEY_IS_HIDE_ROOT_DIALOG_ENABLED
import com.passbolt.mobile.android.domain.preferences.GlobalPreferencesLocalDataSource
import com.passbolt.mobile.android.domain.preferences.GlobalPreferencesUpdate
import com.passbolt.mobile.android.domain.preferences.PreferencesDefaults
import com.passbolt.mobile.android.encryptedstorage.EncryptedSharedPreferencesFactory
import com.passbolt.mobile.android.ui.GlobalPreferencesUiModel
import java.time.LocalDateTime
import java.time.ZoneOffset

internal class GlobalPreferencesLocalDataSourceImpl(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
) : GlobalPreferencesLocalDataSource {
    override fun getGlobalPreferences(): GlobalPreferencesUiModel {
        val sharedPreferences = encryptedSharedPreferencesFactory.get("$GLOBAL_PREFERENCES_FILE_NAME.xml")
        val areDebugLogsEnabled = sharedPreferences.getBoolean(KEY_DEBUG_LOGS_ENABLED, false)
        val debugLogsCreationDateTime =
            sharedPreferences.getLong(KEY_DEBUG_LOGS_FILE_CREATION_DATE_TIME, -1L).let {
                if (it == -1L) null else LocalDateTime.ofEpochSecond(it, 0, ZoneOffset.UTC)
            }
        val debugLogLastAppVersion = sharedPreferences.getString(KEY_DEBUG_LOGS_LAST_APP_VERSION, null)
        val isHideRootDialogEnabled = sharedPreferences.getBoolean(KEY_IS_HIDE_ROOT_DIALOG_ENABLED, false)
        val isAuthRequiredOnEveryEntry = sharedPreferences.getBoolean(KEY_IS_AUTH_REQUIRED_ON_EVERY_ENTRY, false)
        val apiFetchPageSize = sharedPreferences.getInt(KEY_API_FETCH_PAGE_SIZE, PreferencesDefaults.API_FETCH_PAGE_SIZE)
        val isApiFetchPageSizeManuallySet = sharedPreferences.getBoolean(KEY_API_FETCH_PAGE_SIZE_MANUAL, false)
        val accessibilityPoliciesConsentGiven =
            sharedPreferences.getBoolean(KEY_ACCESSIBILITY_POLICIES_CONSENT_GIVEN, false)
        val isCopyTotpOnAutofillEnabled = sharedPreferences.getBoolean(KEY_COPY_TOTP_ON_AUTOFILL_ENABLED, false)
        return GlobalPreferencesUiModel(
            areDebugLogsEnabled = areDebugLogsEnabled,
            debugLogFileCreationDateTime = debugLogsCreationDateTime,
            debugLogLastAppVersion = debugLogLastAppVersion,
            isHideRootDialogEnabled = isHideRootDialogEnabled,
            isAuthRequiredOnEveryEntry = isAuthRequiredOnEveryEntry,
            apiFetchPageSize = apiFetchPageSize,
            isApiFetchPageSizeManuallySet = isApiFetchPageSizeManuallySet,
            accessibilityPoliciesConsentGiven = accessibilityPoliciesConsentGiven,
            isCopyTotpOnAutofillEnabled = isCopyTotpOnAutofillEnabled,
        )
    }

    override fun updateGlobalPreferences(update: GlobalPreferencesUpdate) {
        val sharedPreferences = encryptedSharedPreferencesFactory.get("$GLOBAL_PREFERENCES_FILE_NAME.xml")
        with(sharedPreferences.edit()) {
            update.areDebugLogsEnabled?.let {
                putBoolean(KEY_DEBUG_LOGS_ENABLED, it)
            }
            update.debugLogFileCreationDateTime?.let {
                putLong(KEY_DEBUG_LOGS_FILE_CREATION_DATE_TIME, it.toEpochSecond(ZoneOffset.UTC))
            }
            update.debugLogLastAppVersion?.let {
                putString(KEY_DEBUG_LOGS_LAST_APP_VERSION, it)
            }
            update.isHideRootDialogEnabled?.let {
                putBoolean(KEY_IS_HIDE_ROOT_DIALOG_ENABLED, it)
            }
            update.isAuthRequiredOnEveryEntry?.let {
                putBoolean(KEY_IS_AUTH_REQUIRED_ON_EVERY_ENTRY, it)
            }
            update.apiFetchPageSize?.let {
                putInt(KEY_API_FETCH_PAGE_SIZE, it)
            }
            update.isApiFetchPageSizeManuallySet?.let {
                putBoolean(KEY_API_FETCH_PAGE_SIZE_MANUAL, it)
            }
            update.accessibilityPoliciesConsentGiven?.let {
                putBoolean(KEY_ACCESSIBILITY_POLICIES_CONSENT_GIVEN, it)
            }
            update.isCopyTotpOnAutofillEnabled?.let {
                putBoolean(KEY_COPY_TOTP_ON_AUTOFILL_ENABLED, it)
            }
            apply()
        }
    }
}
