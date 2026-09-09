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

package com.passbolt.mobile.android.resourcemoremenu.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.preferences.AccountPreferencesRepository
import com.passbolt.mobile.android.domain.rbac.usecase.GetRbacRulesUseCase
import com.passbolt.mobile.android.domain.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.domain.secrets.usecase.offline.IsResourceMarkedOfflineUseCase
import com.passbolt.mobile.android.ui.OfflineModeSetting
import com.passbolt.mobile.android.ui.RbacRuleModel.ALLOW
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.DescriptionOption.HAS_METADATA_DESCRIPTION
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.DescriptionOption.HAS_NOTE
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.FavouriteOption.ADD_TO_FAVOURITES
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.FavouriteOption.REMOVE_FROM_FAVOURITES
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.OfflineOption
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.contentType
import com.passbolt.mobile.android.ui.isFavourite

class CreateResourceMoreMenuModelUseCase(
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val getRbacRulesUseCase: GetRbacRulesUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val accountPreferencesRepository: AccountPreferencesRepository,
    private val isResourceMarkedOfflineUseCase: IsResourceMarkedOfflineUseCase,
) : AsyncUseCase<CreateResourceMoreMenuModelUseCase.Input, CreateResourceMoreMenuModelUseCase.Output> {
    override suspend fun execute(input: Input): Output {
        val resource = getLocalResourceUseCase.execute(GetLocalResourceUseCase.Input(input.resourceId)).resource
        val rbacModel = getRbacRulesUseCase.execute(Unit).rbacModel
        val isCopyRbacAllowed = rbacModel.passwordCopyRule == ALLOW
        val isShareRbacAllowed = rbacModel.shareViewRule == ALLOW
        val contentType = resource.contentType()
        val offlineOption = offlineOptionFor(input.resourceId)

        return Output(
            ResourceMoreMenuModel(
                title = resource.metadataJsonModel.name,
                canCopy = isCopyRbacAllowed,
                canDelete = resource.permission in WRITE_PERMISSIONS,
                canEdit = resource.permission in WRITE_PERMISSIONS,
                canShare = isShareRbacAllowed && resource.permission == ResourcePermission.OWNER,
                favouriteOption =
                    if (resource.isFavourite()) {
                        REMOVE_FROM_FAVOURITES
                    } else {
                        ADD_TO_FAVOURITES
                    },
                descriptionOptions =
                    buildList {
                        if (contentType.hasMetadataDescription()) {
                            add(HAS_METADATA_DESCRIPTION)
                        }
                        if (contentType.hasNote()) {
                            add(HAS_NOTE)
                        }
                    },
                offlineOption = offlineOption,
            ),
        )
    }

    // the per-entry choice only exists in "selected entries" mode; with "all entries"
    // every secret is cached anyway and with offline mode off there is nothing to mark
    private suspend fun offlineOptionFor(resourceId: String): OfflineOption? {
        val userId = getSelectedAccountUseCase.execute(Unit).selectedAccount ?: return null
        if (accountPreferencesRepository.getAccountFlags(userId).offlineMode != OfflineModeSetting.SELECTED_ENTRIES) {
            return null
        }
        return if (isResourceMarkedOfflineUseCase.execute(IsResourceMarkedOfflineUseCase.Input(resourceId))) {
            OfflineOption.REMOVE_OFFLINE_AVAILABILITY
        } else {
            OfflineOption.MAKE_AVAILABLE_OFFLINE
        }
    }

    data class Input(
        val resourceId: String,
    )

    data class Output(
        val resourceMenuModel: ResourceMoreMenuModel,
    )

    private companion object {
        private val WRITE_PERMISSIONS =
            setOf(
                ResourcePermission.OWNER,
                ResourcePermission.UPDATE,
            )
    }
}
