/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2025 Passbolt SA
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

package com.passbolt.mobile.android.scenarios.resource.details

/**
 * Enum representing different types of resources used in tests
 */
enum class TestResourceType(
    val displayName: String,
) {
    SIMPLE_PASSWORD("Simple password"),
    PASSWORD_WITH_DESCRIPTION("Password with description"),
    PASSWORD_DESCRIPTION_TOTP("Password, Description and TOTP"),
    // TODO - These need to be enabled after enabling V5 on cloud's `Betty` user
    // SIMPLE_PASSWORD_DEPRECATED("Simple Password (Deprecated)"),
    // DEFAULT_RESOURCE_TYPE("Default resource type"),
    // DEFAULT_RESOURCE_TYPE_WITH_TOTP("Default resource type with TOTP")
}
