package com.passbolt.mobile.android.scenarios.settings

import androidx.annotation.StringRes
import com.passbolt.mobile.android.core.ui.compose.menu.OpenableSettingsItem
import com.passbolt.mobile.android.core.ui.compose.menu.SwitchableSettingsItem
import com.passbolt.mobile.android.core.localization.R as LocalizationR

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
    val hasOpenableIcon: Boolean = true,
    val testTag: String = OpenableSettingsItem.TestTags.ITEM,
) {
    APP_SETTINGS(
        settingsItemTextId = LocalizationR.string.settings_app_settings,
    ),
    ACCOUNTS(
        settingsItemTextId = LocalizationR.string.settings_accounts,
    ),
    TERMS_AND_LICENSES(
        settingsItemTextId = LocalizationR.string.settings_terms_and_licenses,
    ),
    DEBUG_LOGS(
        settingsItemTextId = LocalizationR.string.settings_debug_logs,
    ),
    SIGN_OUT(
        settingsItemTextId = LocalizationR.string.settings_sign_out,
        hasOpenableIcon = false,
    ),
}

enum class AppSettingsItemModel(
    @StringRes val settingsItemTextId: Int,
    val hasOpenableIcon: Boolean = true,
    val testTag: String = OpenableSettingsItem.TestTags.ITEM,
) {
    FINGERPRINT(
        settingsItemTextId = LocalizationR.string.settings_app_settings_fingerprint,
        hasOpenableIcon = false,
        testTag = SwitchableSettingsItem.TestTags.ITEM,
    ),
    AUTOFILL(
        settingsItemTextId = LocalizationR.string.settings_app_settings_autofill,
    ),
    DEFAULT_FILTER(
        settingsItemTextId = LocalizationR.string.settings_app_settings_default_filter,
    ),
    EXPERT_SETTINGS(
        settingsItemTextId = LocalizationR.string.settings_app_settings_expert_settings,
    ),
}

enum class ExpertSettingsItemModel(
    @StringRes val settingsItemTextId: Int,
    val testTag: String = SwitchableSettingsItem.TestTags.ITEM,
) {
    DEVELOPER_MODE(
        settingsItemTextId = LocalizationR.string.settings_app_settings_expert_settings_dev_mode,
    ),
    DEVICE_IS_ROOTED_DIALOG(
        settingsItemTextId = LocalizationR.string.settings_app_settings_expert_settings_hide_root,
    ),
}

enum class DebugLogsItemModel(
    @StringRes val settingsItemTextId: Int,
    val testTag: String,
    val hasOpenableIcon: Boolean,
) {
    ENABLE_DEBUG_LOGS(
        settingsItemTextId = LocalizationR.string.settings_debug_logs_enable_logs,
        testTag = SwitchableSettingsItem.TestTags.ITEM,
        hasOpenableIcon = false,
    ),
    ACCESS_THE_LOGS(
        settingsItemTextId = LocalizationR.string.settings_debug_logs_settings_logs,
        testTag = OpenableSettingsItem.TestTags.ITEM,
        hasOpenableIcon = true,
    ),
    VISIT_HELP_WEBSITE(
        settingsItemTextId = LocalizationR.string.settings_debug_logs_visit_help_website,
        testTag = OpenableSettingsItem.TestTags.ITEM,
        hasOpenableIcon = false,
    ),
}
