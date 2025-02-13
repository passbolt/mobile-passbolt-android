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

package com.passbolt.mobile.android.resourcepicker

import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.core.fulldatarefresh.base.DataRefreshViewReactivePresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcesUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetResourceTypeIdToSlugMappingUseCase
import com.passbolt.mobile.android.mappers.ResourcePickerMapper
import com.passbolt.mobile.android.resourcepicker.model.ConfirmationModel
import com.passbolt.mobile.android.resourcepicker.model.PickResourceAction
import com.passbolt.mobile.android.resourcepicker.model.SearchInputEndIconMode
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.allSlugs
import com.passbolt.mobile.android.ui.ResourcePickerListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

class ResourcePickerPresenter(
    private val getLocalResourcesUseCase: GetLocalResourcesUseCase,
    private val searchableMatcher: SearchableMatcher,
    private val resourcePickerMapper: ResourcePickerMapper,
    private val getResourceTypeIdToSlugMappingUseCase: GetResourceTypeIdToSlugMappingUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : ResourcePickerContract.Presenter,
    DataRefreshViewReactivePresenter<ResourcePickerContract.View>(coroutineLaunchContext) {

    override var view: ResourcePickerContract.View? = null
    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(job + coroutineLaunchContext.ui)
    private val filteringJob = SupervisorJob()
    private val filteringScope = CoroutineScope(filteringJob + coroutineLaunchContext.ui)

    private var currentSearchText = MutableStateFlow("")
    private val searchInputEndIconMode
        get() = if (currentSearchText.value.isBlank()) SearchInputEndIconMode.NONE else SearchInputEndIconMode.CLEAR
    private var suggestionUri: String? = null

    private var resourceList: List<ResourcePickerListItem> = emptyList()
    private var suggestedResourceList: List<ResourcePickerListItem> = emptyList()

    private lateinit var pickedResource: ResourcePickerListItem
    private val pickedResourceResourceTypeId
        get() = UUID.fromString(pickedResource.resourceModel.resourceTypeId)

    override fun attach(view: ResourcePickerContract.View) {
        super<DataRefreshViewReactivePresenter>.attach(view)
        collectFilteringRefreshes()
    }

    override fun argsRetrieved(suggestionUri: String?) {
        this.suggestionUri = suggestionUri
    }

    override fun refreshAction() {
        coroutineScope.launch {
            showResourcesFromDatabase()
            if (::pickedResource.isInitialized) {
                resourcePicked(pickedResource, isSelected = true)
            }
        }
    }

    override fun refreshFailureAction() {
        view?.showDataRefreshError()
    }

    override fun refreshSwipe() {
        fullDataRefreshExecutor.performFullDataRefresh()
    }

    override fun resourcePicked(selectableResourceModel: ResourcePickerListItem, isSelected: Boolean) {
        if (selectableResourceModel.isSelectable && isSelected) {
            pickedResource = selectableResourceModel
            suggestedResourceList =
                suggestedResourceList.updatedAfterSelectedResource(selectableResourceModel, isSelected)
            resourceList = resourceList.updatedAfterSelectedResource(selectableResourceModel, isSelected)
            filterHomeData()
            view?.enableApplyButton()
        }
    }

    private fun List<ResourcePickerListItem>.updatedAfterSelectedResource(
        resource: ResourcePickerListItem,
        isSelected: Boolean
    ) =
        map {
            it.copy(
                isSelected = it.resourceModel.resourceId == resource.resourceModel.resourceId &&
                        isSelected
            )
        }

    override fun searchTextChanged(text: String) {
        currentSearchText.value = text
    }

    override fun searchClearClick() {
        view?.clearSearchInput()
    }

    private fun collectFilteringRefreshes() {
        filteringScope.launch {
            currentSearchText
                .drop(1) // initial empty value
                .collectLatest {
                    Timber.d("New search text received")
                    processSearchIconChange()
                    filterHomeData()
                }
        }
    }

    private fun processSearchIconChange() {
        when (searchInputEndIconMode) {
            SearchInputEndIconMode.NONE -> view?.hideSearchEndIcon()
            SearchInputEndIconMode.CLEAR -> view?.displaySearchClearEndIcon()
        }
    }

    private fun filterHomeData() {
        val filtered = resourceList
            .filter { searchableMatcher.matches(it, currentSearchText.value) }

        if (filtered.isEmpty()) {
            view?.showEmptyState()
        } else {
            view?.hideEmptyState()
            view?.showResources(suggestedResourceList, filtered)
        }
    }

    private suspend fun showResourcesFromDatabase() {
        val selectableResourceTypesIds = getResourceTypeIdToSlugMappingUseCase.execute(Unit)
            .idToSlugMapping
            .filter { it.value in selectableResourceTypesSlugs }
            .keys

        resourceList = getLocalResourcesUseCase.execute(
            GetLocalResourcesUseCase.Input(allSlugs)
        )
            .resources
            .map { resourcePickerMapper.map(it, selectableResourceTypesIds) }

        suggestionUri?.let { suggestionUri ->
            suggestedResourceList = resourceList
                .filter {
                    !it.resourceModel.uri.isNullOrBlank() &&
                            it.resourceModel.uri!!.lowercase() == suggestionUri.lowercase()
                }
        }

        if (resourceList.isEmpty()) {
            view?.showEmptyState()
        } else {
            view?.hideEmptyState()
            view?.showResources(suggestedResourceList, resourceList)
        }
    }

    override fun applyClick() {
        coroutineScope.launch {
            val selectableIdToSlugMapping = getResourceTypeIdToSlugMappingUseCase.execute(Unit)
                .idToSlugMapping
                .filter { it.value in selectableResourceTypesSlugs }

            require(pickedResourceResourceTypeId in selectableIdToSlugMapping.keys)
            val (pickAction, confirmationModel) =
                when (val slug = selectableIdToSlugMapping[pickedResourceResourceTypeId]) {
                    ContentType.PasswordAndDescription.slug, ContentType.V5Default.slug ->
                        PickResourceAction.TOTP_LINK to ConfirmationModel.LinkTotpModel()
                    ContentType.PasswordDescriptionTotp.slug, ContentType.V5DefaultWithTotp.slug ->
                        PickResourceAction.TOTP_REPLACE to ConfirmationModel.ReplaceTotpModel()
                    else -> error("This resource type does not support linking or replacing totplink: $slug")
                }

            view?.showConfirmation(confirmationModel, pickAction)
        }
    }

    override fun otpLinkConfirmed(pickAction: PickResourceAction) {
        view?.setResultAndNavigateBack(pickAction, pickedResource.resourceModel)
    }

    override fun detach() {
        filteringScope.coroutineContext.cancelChildren()
        coroutineScope.coroutineContext.cancelChildren()
        super<DataRefreshViewReactivePresenter>.detach()
    }

    private companion object {
        private val selectableResourceTypesSlugs = listOf(
            ContentType.PasswordAndDescription.slug,
            ContentType.V5Default.slug,
            ContentType.PasswordDescriptionTotp.slug,
            ContentType.V5DefaultWithTotp.slug
        )
    }
}
