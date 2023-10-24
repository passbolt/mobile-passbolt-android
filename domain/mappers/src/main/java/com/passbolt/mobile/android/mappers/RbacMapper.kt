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

package com.passbolt.mobile.android.mappers

import com.passbolt.mobile.android.dto.response.RbacPermissionDto
import com.passbolt.mobile.android.ui.RbacModel
import com.passbolt.mobile.android.ui.RbacRuleModel
import com.passbolt.mobile.android.ui.RbacRuleModel.ALLOW
import com.passbolt.mobile.android.ui.RbacRuleModel.DENY
import com.passbolt.mobile.android.ui.RbacRuleModel.UNSUPPORTED_RULE

class RbacMapper {

    fun map(rbacs: List<RbacPermissionDto>) = RbacModel(
        passwordCopyRule = findRuleOrDefault(COPY_PASSWORD_RULE, rbacs, ALLOW),
        passwordPreviewRule = findRuleOrDefault(PREVIEW_PASSWORD_RULE, rbacs, ALLOW),
        tagsUseRule = findRuleOrDefault(USE_TAGS_RULE, rbacs, ALLOW),
        foldersUseRule = findRuleOrDefault(USE_FOLDERS_RULE, rbacs, ALLOW),
        shareViewRule = findRuleOrDefault(VIEW_SHARE_RULE, rbacs, ALLOW)
    )

    private fun findRuleOrDefault(
        ruleName: String,
        rules: List<RbacPermissionDto>,
        defaultRule: RbacRuleModel
    ): RbacRuleModel {
        val rule = rules.find { it.uiAction?.name == ruleName }
        return map(rule?.controlFunction) ?: defaultRule
    }

    private fun map(controlFunction: String?) =
        when (controlFunction) {
            "Allow" -> ALLOW
            "Deny" -> DENY
            null -> null
            else -> UNSUPPORTED_RULE
        }

    private companion object {
        private const val PREVIEW_PASSWORD_RULE = "Secrets.preview"
        private const val COPY_PASSWORD_RULE = "Secrets.copy"
        private const val USE_TAGS_RULE = "Tags.use"
        private const val USE_FOLDERS_RULE = "Folders.use"
        private const val VIEW_SHARE_RULE = "Share.viewList"
    }
}
