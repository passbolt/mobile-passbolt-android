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

import com.passbolt.mobile.android.data.preferences.AccountPreferencesFileName
import com.passbolt.mobile.android.data.preferences.KEY_CHROME_NATIVE_AUTOFILL_DIALOG_SHOWN
import com.passbolt.mobile.android.data.preferences.KEY_LAST_USED_HOME_VIEW
import com.passbolt.mobile.android.data.preferences.KEY_OFFLINE_LAST_SYNC_EPOCH_MILLIS
import com.passbolt.mobile.android.data.preferences.KEY_OFFLINE_MODE
import com.passbolt.mobile.android.data.preferences.KEY_USER_SET_HOME_VIEW
import com.passbolt.mobile.android.domain.preferences.AccountFlagsUpdate
import com.passbolt.mobile.android.domain.preferences.AccountPreferencesLocalDataSource
import com.passbolt.mobile.android.domain.preferences.HomeDisplayViewPreferencesUpdate
import com.passbolt.mobile.android.encryptedstorage.EncryptedSharedPreferencesFactory
import com.passbolt.mobile.android.ui.AccountFlagsUiModel
import com.passbolt.mobile.android.ui.DefaultFilterUiModel
import com.passbolt.mobile.android.ui.HomeDisplayViewPreferencesUiModel
import com.passbolt.mobile.android.ui.HomeDisplayViewUiModel
import com.passbolt.mobile.android.ui.OfflineModeSetting
import timber.log.Timber

internal class AccountPreferencesLocalDataSourceImpl(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
) : AccountPreferencesLocalDataSource {
    override fun getHomeDisplayViewPreferences(userId: String): HomeDisplayViewPreferencesUiModel {
        with(sharedPreferences(userId)) {
            return try {
                val lastUsedHomeViewOrdinal = getInt(KEY_LAST_USED_HOME_VIEW, DEFAULT_LAST_USED_FILTER_ORDINAL)
                val lastUsedHomeView = HomeDisplayViewUiModel.entries[lastUsedHomeViewOrdinal]

                val userSetHomeViewOrdinal = getInt(KEY_USER_SET_HOME_VIEW, -1)
                val userSetHomeView =
                    if (userSetHomeViewOrdinal != -1) {
                        DefaultFilterUiModel.entries[userSetHomeViewOrdinal]
                    } else {
                        DefaultFilterUiModel.LAST_USED
                    }

                HomeDisplayViewPreferencesUiModel(
                    lastUsedHomeView = lastUsedHomeView,
                    userSetHomeView = userSetHomeView,
                )
            } catch (e: IndexOutOfBoundsException) {
                Timber.w(e, "Stored home view ordinal is invalid, falling back to defaults")
                HomeDisplayViewPreferencesUiModel(
                    lastUsedHomeView = HomeDisplayViewUiModel.ALL_ITEMS,
                    userSetHomeView = DefaultFilterUiModel.LAST_USED,
                )
            }
        }
    }

    override fun updateHomeDisplayViewPreferences(
        update: HomeDisplayViewPreferencesUpdate,
        userId: String,
    ) {
        with(sharedPreferences(userId).edit()) {
            update.lastUsedHomeView?.let { putInt(KEY_LAST_USED_HOME_VIEW, it.ordinal) }
            update.userSetHomeView?.let { putInt(KEY_USER_SET_HOME_VIEW, it.ordinal) }
            apply()
        }
    }

    override fun getAccountFlags(userId: String): AccountFlagsUiModel {
        with(sharedPreferences(userId)) {
            return AccountFlagsUiModel(
                wasChromeNativeAutofillDialogShown = getBoolean(KEY_CHROME_NATIVE_AUTOFILL_DIALOG_SHOWN, false),
                offlineMode =
                    getString(KEY_OFFLINE_MODE, null)
                        ?.let { stored -> OfflineModeSetting.entries.firstOrNull { it.name == stored } }
                        ?: OfflineModeSetting.OFF,
                offlineLastSyncEpochMillis =
                    getLong(KEY_OFFLINE_LAST_SYNC_EPOCH_MILLIS, NO_OFFLINE_SYNC).takeIf { it != NO_OFFLINE_SYNC },
            )
        }
    }

    override fun updateAccountFlags(
        update: AccountFlagsUpdate,
        userId: String,
    ) {
        with(sharedPreferences(userId).edit()) {
            update.wasChromeNativeAutofillDialogShown?.let { putBoolean(KEY_CHROME_NATIVE_AUTOFILL_DIALOG_SHOWN, it) }
            update.offlineMode?.let { putString(KEY_OFFLINE_MODE, it.name) }
            update.offlineLastSyncEpochMillis?.let { putLong(KEY_OFFLINE_LAST_SYNC_EPOCH_MILLIS, it) }
            if (update.clearOfflineLastSync) {
                remove(KEY_OFFLINE_LAST_SYNC_EPOCH_MILLIS)
            }
            apply()
        }
    }

    private fun sharedPreferences(userId: String) = encryptedSharedPreferencesFactory.get("${AccountPreferencesFileName(userId).name}.xml")

    private companion object {
        private val DEFAULT_LAST_USED_FILTER_ORDINAL = HomeDisplayViewUiModel.ALL_ITEMS.ordinal
        private const val NO_OFFLINE_SYNC = -1L
    }
}
