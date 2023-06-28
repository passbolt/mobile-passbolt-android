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
import com.passbolt.mobile.android.database.impl.resources.GetLocalResourcesUseCase
import com.passbolt.mobile.android.database.impl.resourcetypes.GetResourceTypeIdToSlugMappingUseCase
import com.passbolt.mobile.android.mappers.ResourcePickerMapper
import com.passbolt.mobile.android.resourcepicker.model.SearchInputEndIconMode
import com.passbolt.mobile.android.serializers.SupportedContentTypes
import com.passbolt.mobile.android.serializers.SupportedContentTypes.PASSWORD_AND_DESCRIPTION_SLUG
import com.passbolt.mobile.android.serializers.SupportedContentTypes.PASSWORD_DESCRIPTION_TOTP_SLUG
import com.passbolt.mobile.android.ui.SelectableResourceModelWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import timber.log.Timber

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
    private lateinit var suggestion: String

    private var resourceList: List<SelectableResourceModelWrapper> = emptyList()
    private var suggestedResourceList: List<SelectableResourceModelWrapper> = emptyList()

    override fun attach(view: ResourcePickerContract.View) {
        super<DataRefreshViewReactivePresenter>.attach(view)
        collectFilteringRefreshes()
    }

    override fun argsRetrieved(suggestion: String) {
        this.suggestion = suggestion
    }

    override fun refreshAction() {
        coroutineScope.launch {
            showResourcesFromDatabase()
        }
    }

    override fun refreshFailureAction() {
        view?.showDataRefreshError()
    }

    override fun refreshSwipe() {
        // TODO
        // add after merge (new mechanism on develop)
    }

    override fun resourcePicked(selectableResourceModel: SelectableResourceModelWrapper, selected: Boolean) {
        resourceList = resourceList
            .map {
                it.copy(
                    isSelected = it.resourceModel.resourceId == selectableResourceModel.resourceModel.resourceId &&
                            selected
                )
            }
        view?.showResources(suggestedResourceList, resourceList)
        view?.enableApplyButton()
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
            GetLocalResourcesUseCase.Input(SupportedContentTypes.allSlugs)
        )
            .resources
            .map { resourcePickerMapper.map(it, selectableResourceTypesIds) }

        suggestedResourceList = resourceList
            .filter { it.resourceModel.name.lowercase() == suggestion.lowercase() }

        if (resourceList.isEmpty()) {
            view?.showEmptyState()
        } else {
            view?.hideEmptyState()
            view?.showResources(suggestedResourceList, resourceList)
        }
    }

    override fun detach() {
        filteringScope.coroutineContext.cancelChildren()
        coroutineScope.coroutineContext.cancelChildren()
        super<DataRefreshViewReactivePresenter>.detach()
    }

    private companion object {
        private val selectableResourceTypesSlugs = listOf(
            PASSWORD_AND_DESCRIPTION_SLUG,
            PASSWORD_DESCRIPTION_TOTP_SLUG
        )
    }
}
