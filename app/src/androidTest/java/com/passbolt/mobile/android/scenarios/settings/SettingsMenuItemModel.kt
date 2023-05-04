package com.passbolt.mobile.android.scenarios.settings

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.passbolt.mobile.android.feature.settings.R

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

enum class SettingsMenuItemModel(
    @StringRes val settingsItemTextId: Int,
    @DrawableRes val settingsItemIconId: Int,
    @IdRes val settingsItemId: Int
) {
    APP_SETTINGS(
        settingsItemTextId = R.string.settings_app_settings,
        settingsItemIconId = R.drawable.ic_app_settings,
        settingsItemId = R.id.appSettings
    ),
    ACCOUNTS(
        settingsItemTextId = R.string.settings_accounts,
        settingsItemIconId = R.drawable.ic_manage_accounts,
        settingsItemId = R.id.accountsSettings
    ),
    TERMS_AND_LICENSES(
        settingsItemTextId = R.string.settings_terms_and_licenses,
        settingsItemIconId = R.drawable.ic_terms,
        settingsItemId = R.id.termsAndLicensesSettings
    ),
    DEBUG_LOGS(
        settingsItemTextId = R.string.settings_debug_logs,
        settingsItemIconId = R.drawable.ic_bug,
        settingsItemId = R.id.debugLogsSettings
    ),
    SIGN_OUT(
        settingsItemTextId = R.string.settings_sign_out,
        settingsItemIconId = R.drawable.ic_sign_out,
        settingsItemId = R.id.signOut
    )
}

enum class AppSettingsItemModel(
    @StringRes val settingsItemTextId: Int,
    @DrawableRes val settingsItemIconId: Int,
    @IdRes val settingsItemId: Int
) {
    FINGERPRINT(
        settingsItemTextId = R.string.settings_app_settings_fingerprint,
        settingsItemIconId = R.drawable.ic_fingerprint,
        settingsItemId = R.id.fingerprintSetting
    ),
    AUTOFILL(
        settingsItemTextId = R.string.settings_app_settings_autofill,
        settingsItemIconId = R.drawable.ic_key,
        settingsItemId = R.id.autofillSetting
    ),
    DEFAULT_FILTER(
        settingsItemTextId = R.string.settings_app_settings_default_filter,
        settingsItemIconId = R.drawable.ic_filter,
        settingsItemId = R.id.defaultFilterSetting
    ),
    EXPERT_SETTINGS(
        settingsItemTextId = R.string.settings_app_settings_expert_settings,
        settingsItemIconId = R.drawable.ic_cog,
        settingsItemId = R.id.expertSettings
    )
}

enum class ExpertSettingsItemModel(
    @StringRes val settingsItemTextId: Int,
    @DrawableRes val settingsItemIconId: Int,
    @IdRes val settingsItemId: Int
) {
    DEVELOPER_MODE(
        settingsItemTextId = R.string.settings_app_settings_expert_settings_dev_mode,
        settingsItemIconId = R.drawable.ic_dev_mode,
        settingsItemId = R.id.developerModeSetting
    ),
    DEVICE_IS_ROOTED_DIALOG(
        settingsItemTextId = R.string.settings_app_settings_expert_settings_hide_root,
        settingsItemIconId = R.drawable.ic_hash,
        settingsItemId = R.id.hideRootWarningSetting
    )
}

enum class DebugLogsItemModel(
    @StringRes val settingsItemTextId: Int,
    @DrawableRes val settingsItemIconId: Int,
    @IdRes val settingsItemId: Int
) {
    ENABLE_DEBUG_LOGS(
        settingsItemTextId = R.string.settings_debug_logs_enable_logs,
        settingsItemIconId = R.drawable.ic_bug,
        settingsItemId = R.id.enableLogsSetting
    ),
    ACCESS_THE_LOGS(
        settingsItemTextId = R.string.settings_debug_logs_settings_logs,
        settingsItemIconId = R.drawable.ic_access_logs,
        settingsItemId = R.id.accessLogsSetting
    ),
    VISIT_HELP_WEBSITE(
        settingsItemTextId = R.string.settings_debug_logs_visit_help_website,
        settingsItemIconId = R.drawable.ic_link,
        settingsItemId = R.id.visitHelpWebsite
    )
}
