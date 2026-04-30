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

package com.passbolt.mobile.android.resourcemoremenu

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.idlingresource.CreateMenuModelIdlingResource
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.resourcemoremenu.ResourceMoreMenuBottomSheetIntent.Close
import com.passbolt.mobile.android.resourcemoremenu.ResourceMoreMenuBottomSheetIntent.CopyMetadataDescription
import com.passbolt.mobile.android.resourcemoremenu.ResourceMoreMenuBottomSheetIntent.CopyNote
import com.passbolt.mobile.android.resourcemoremenu.ResourceMoreMenuBottomSheetIntent.CopyPassword
import com.passbolt.mobile.android.resourcemoremenu.ResourceMoreMenuBottomSheetIntent.CopyUrl
import com.passbolt.mobile.android.resourcemoremenu.ResourceMoreMenuBottomSheetIntent.CopyUsername
import com.passbolt.mobile.android.resourcemoremenu.ResourceMoreMenuBottomSheetIntent.Delete
import com.passbolt.mobile.android.resourcemoremenu.ResourceMoreMenuBottomSheetIntent.Edit
import com.passbolt.mobile.android.resourcemoremenu.ResourceMoreMenuBottomSheetIntent.Initialize
import com.passbolt.mobile.android.resourcemoremenu.ResourceMoreMenuBottomSheetIntent.LaunchWebsite
import com.passbolt.mobile.android.resourcemoremenu.ResourceMoreMenuBottomSheetIntent.Share
import com.passbolt.mobile.android.resourcemoremenu.ResourceMoreMenuBottomSheetIntent.ToggleFavourite
import com.passbolt.mobile.android.resourcemoremenu.ResourceMoreMenuBottomSheetSideEffect.Dismiss
import com.passbolt.mobile.android.resourcemoremenu.usecase.CreateResourceMoreMenuModelUseCase
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.DescriptionOption.HAS_METADATA_DESCRIPTION
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.DescriptionOption.HAS_NOTE
import kotlinx.coroutines.launch
import timber.log.Timber

class ResourceMoreMenuBottomSheetViewModel(
    private val createResourceMoreMenuModelUseCase: CreateResourceMoreMenuModelUseCase,
    private val coroutineLaunchContext: CoroutineLaunchContext,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
    private val createMenuModelIdlingResource: CreateMenuModelIdlingResource,
) : SideEffectViewModel<ResourceMoreMenuBottomSheetState, ResourceMoreMenuBottomSheetSideEffect>(
        ResourceMoreMenuBottomSheetState(),
    ) {
    private var menuModel: ResourceMoreMenuModel? = null

    fun onIntent(intent: ResourceMoreMenuBottomSheetIntent) {
        when (intent) {
            is Initialize -> initialize(intent.resourceId)
            Close -> emitSideEffect(Dismiss)
            CopyPassword -> {
                emitSideEffect(ResourceMoreMenuBottomSheetSideEffect.CopyPassword)
                emitSideEffect(Dismiss)
            }
            CopyMetadataDescription -> {
                emitSideEffect(ResourceMoreMenuBottomSheetSideEffect.CopyMetadataDescription)
                emitSideEffect(Dismiss)
            }
            CopyNote -> {
                emitSideEffect(ResourceMoreMenuBottomSheetSideEffect.CopyNote)
                emitSideEffect(Dismiss)
            }
            CopyUrl -> {
                emitSideEffect(ResourceMoreMenuBottomSheetSideEffect.CopyUrl)
                emitSideEffect(Dismiss)
            }
            CopyUsername -> {
                emitSideEffect(ResourceMoreMenuBottomSheetSideEffect.CopyUsername)
                emitSideEffect(Dismiss)
            }
            LaunchWebsite -> {
                emitSideEffect(ResourceMoreMenuBottomSheetSideEffect.LaunchWebsite)
                emitSideEffect(Dismiss)
            }
            Delete -> {
                emitSideEffect(ResourceMoreMenuBottomSheetSideEffect.Delete)
                emitSideEffect(Dismiss)
            }
            Edit -> {
                emitSideEffect(ResourceMoreMenuBottomSheetSideEffect.Edit)
                emitSideEffect(Dismiss)
            }
            Share -> {
                emitSideEffect(ResourceMoreMenuBottomSheetSideEffect.Share)
                emitSideEffect(Dismiss)
            }
            ToggleFavourite -> {
                menuModel?.favouriteOption?.let { option ->
                    emitSideEffect(ResourceMoreMenuBottomSheetSideEffect.ToggleFavourite(option))
                    emitSideEffect(Dismiss)
                }
            }
        }
    }

    private fun initialize(resourceId: String) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            createMenuModelIdlingResource.setIdle(false)
            updateViewState { ResourceMoreMenuBottomSheetState(title = title) }
            dataRefreshTrackingFlow.awaitIdle()

            try {
                menuModel =
                    createResourceMoreMenuModelUseCase
                        .execute(CreateResourceMoreMenuModelUseCase.Input(resourceId))
                        .resourceMenuModel

                menuModel?.let { model ->
                    updateViewState {
                        copy(
                            title = model.title,
                            isLoading = false,
                            showCopyPassword = model.canCopy,
                            showCopyNote = model.descriptionOptions.contains(HAS_NOTE),
                            showCopyMetadataDescription = model.descriptionOptions.contains(HAS_METADATA_DESCRIPTION),
                            showSeparator = model.canDelete || model.canEdit || model.canShare,
                            showDelete = model.canDelete,
                            showEdit = model.canEdit,
                            showShare = model.canShare,
                            favouriteOption = model.favouriteOption,
                        )
                    }
                }
            } catch (exception: NullPointerException) {
                Timber.d("Resource item for the shown menu was deleted: $exception")
                emitSideEffect(Dismiss)
            } finally {
                createMenuModelIdlingResource.setIdle(true)
            }
        }
    }
}
