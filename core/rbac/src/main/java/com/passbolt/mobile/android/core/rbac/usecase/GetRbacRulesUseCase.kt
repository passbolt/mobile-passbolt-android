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

package com.passbolt.mobile.android.core.rbac.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.accounts.usecase.SelectedAccountUseCase
import com.passbolt.mobile.android.encryptedstorage.EncryptedSharedPreferencesFactory
import com.passbolt.mobile.android.ui.RbacModel
import com.passbolt.mobile.android.ui.RbacRuleModel

class GetRbacRulesUseCase(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory
) : AsyncUseCase<Unit, GetRbacRulesUseCase.Output>,
    SelectedAccountUseCase {

    override suspend fun execute(input: Unit): Output {
        val fileName = RbacRulesFileName(selectedAccountId).name
        return encryptedSharedPreferencesFactory.get("$fileName.xml").let {
            val isPreviewPasswordAllowed = it.getString(KEY_PREVIEW_PASSWORD, RbacRuleModel.ALLOW.name)!!
            val isCopyPasswordAllowed = it.getString(KEY_COPY_PASSWORD, RbacRuleModel.ALLOW.name)!!
            val isUseTagsAllowed = it.getString(KEY_USE_TAGS, RbacRuleModel.ALLOW.name)!!
            val isUseFoldersAllowed = it.getString(KEY_USE_FOLDERS, RbacRuleModel.ALLOW.name)!!
            val isViewShareAllowed = it.getString(KEY_VIEW_SHARE, RbacRuleModel.ALLOW.name)!!
            Output(
                RbacModel(
                    passwordPreviewRule = RbacRuleModel.valueOf(isPreviewPasswordAllowed),
                    passwordCopyRule = RbacRuleModel.valueOf(isCopyPasswordAllowed),
                    tagsUseRule = RbacRuleModel.valueOf(isUseTagsAllowed),
                    shareViewRule = RbacRuleModel.valueOf(isViewShareAllowed),
                    foldersUseRule = RbacRuleModel.valueOf(isUseFoldersAllowed)
                )
            )
        }
    }

    data class Output(val rbacModel: RbacModel)
}
