package com.passbolt.mobile.android.scenarios.resourcesedition

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2023 Passbolt SA
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

enum class EditableFieldInput(val hintName: String, val textToReplace: String) {
    ENTER_NAME("Enter Name", "DeleteThisPassword"),
    ENTER_URL("Enter URL", "URLToBeDeleted"),
    ENTER_USERNAME("Enter Username", "UsernameToBeDeleted"),
    ENTER_PASSWORD("Enter password", "PasswordToBeDeleted"),
    ENTER_DESCRIPTION("Enter Description", "Password was generated for testing purposes. Needs to be deleted.")
}
