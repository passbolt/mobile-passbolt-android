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

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.ISelectionListener
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.fastadapter.select.getSelectExtension
import com.passbolt.mobile.android.common.extension.gone
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.common.extension.visible
import com.passbolt.mobile.android.core.extension.clearEndIcon
import com.passbolt.mobile.android.core.extension.setSearchEndIconWithListener
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.core.ui.initialsicon.InitialsIconGenerator
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.resourcepicker.databinding.FragmentResourcePickerBinding
import com.passbolt.mobile.android.resourcepicker.model.HeaderType
import com.passbolt.mobile.android.resourcepicker.recycler.HeaderItem
import com.passbolt.mobile.android.resourcepicker.recycler.SelectableResourceItem
import com.passbolt.mobile.android.ui.SelectableResourceModelWrapper
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

class ResourcePickerFragment :
    BindingScopedAuthenticatedFragment<FragmentResourcePickerBinding, ResourcePickerContract.View>(
        FragmentResourcePickerBinding::inflate
    ), ResourcePickerContract.View {

    override val presenter: ResourcePickerContract.Presenter by inject()
    private val suggestedHeaderItemAdapter: ItemAdapter<HeaderItem> by inject(
        named(SUGGESTED_HEADER_ITEM_ADAPTER)
    )
    private val suggestedItemsItemAdapter: ItemAdapter<SelectableResourceItem> by inject(
        named(SUGGESTED_ITEMS_ITEM_ADAPTER)
    )
    private val otherItemsItemAdapter: ItemAdapter<HeaderItem> by inject(
        named(OTHER_ITEMS_HEADER_ITEM_ADAPTER)
    )
    private val selectableResourceItemAdapter: ItemAdapter<SelectableResourceItem> by inject(
        named(RESOURCE_ITEM_ADAPTER)
    )
    private val fastAdapter: FastAdapter<GenericItem> by inject()
    private val initialsIconGenerator: InitialsIconGenerator by inject()
    private val args: ResourcePickerFragmentArgs by navArgs()
    private val resourcePickedListener = object : ISelectionListener<GenericItem> {
        override fun onSelectionChanged(item: GenericItem, selected: Boolean) {
            if (item is SelectableResourceItem) {
                presenter.resourcePicked(item.selectableResourceModel, selected)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecycler(savedInstanceState)
        setUpListeners()
        presenter.attach(this)
        presenter.argsRetrieved(args.suggestion)
    }

    override fun onDestroyView() {
        fastAdapter.getSelectExtension().selectionListener = null
        presenter.detach()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        presenter.resume(this)
    }

    override fun onPause() {
        presenter.pause()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val withSavedSelections = fastAdapter.saveInstanceState(outState)
        super.onSaveInstanceState(withSavedSelections)
    }

    private fun initRecycler(savedInstanceState: Bundle?) {
        fastAdapter.getSelectExtension().apply {
            isSelectable = true
            multiSelect = false
            allowDeselection = false
            selectOnLongClick = false
            selectWithItemUpdate = true
            selectionListener = resourcePickedListener
            withSavedInstanceState(savedInstanceState, BUNDLE_PICKED_RESOURCE_SELECTION)
        }

        with(binding.recyclerView) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fastAdapter
            itemAnimator = null
        }
    }

    private fun setUpListeners() {
        with(binding) {
            searchEditText.doAfterTextChanged {
                presenter.searchTextChanged(it.toString())
            }
            swipeRefresh.setOnRefreshListener {
                presenter.refreshSwipe()
            }
            backButton.setDebouncingOnClick {
                findNavController().popBackStack()
            }
        }
    }

    override fun hideRefreshProgress() {
        binding.swipeRefresh.isRefreshing = false
    }

    override fun showRefreshProgress() {
        binding.swipeRefresh.isRefreshing = true
    }

    override fun showResources(
        suggestedResources: List<SelectableResourceModelWrapper>,
        resourceList: List<SelectableResourceModelWrapper>
    ) {
        // suggested header
        FastAdapterDiffUtil.calculateDiff(
            suggestedHeaderItemAdapter,
            if (suggestedResources.isNotEmpty()) {
                listOf(HeaderItem(HeaderType.SUGGESTED))
            } else {
                emptyList()
            }
        )
        // suggested items
        FastAdapterDiffUtil.calculateDiff(
            suggestedItemsItemAdapter,
            suggestedResources.map {
                SelectableResourceItem(it, initialsIconGenerator)
            }
        )
        // other items header
        FastAdapterDiffUtil.calculateDiff(
            otherItemsItemAdapter,
            if (resourceList.isNotEmpty() && suggestedResources.isNotEmpty()) {
                listOf(HeaderItem(HeaderType.OTHER))
            } else {
                emptyList()
            }
        )
        // other items
        FastAdapterDiffUtil.calculateDiff(
            selectableResourceItemAdapter,
            resourceList.map {
                SelectableResourceItem(it, initialsIconGenerator)
            })
        fastAdapter.notifyAdapterDataSetChanged()
    }

    override fun showEmptyState() {
        with(binding) {
            recyclerView.gone()
            emptyListContainer.visible()
            appBar.setExpanded(true)
        }
    }

    override fun hideEmptyState() {
        with(binding) {
            recyclerView.visible()
            emptyListContainer.gone()
        }
    }

    override fun displaySearchClearEndIcon() {
        binding.searchTextInput.setSearchEndIconWithListener(
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_close)!!,
            presenter::searchClearClick
        )
    }

    override fun hideSearchEndIcon() {
        binding.searchTextInput.clearEndIcon()
    }

    override fun showDataRefreshError() {
        showSnackbar(
            messageResId = R.string.common_data_refresh_error,
            backgroundColor = R.color.red
        )
    }

    override fun enableApplyButton() {
        binding.applyButton.isEnabled = true
    }

    override fun clearSearchInput() {
        binding.searchEditText.setText("")
    }

    companion object {
        private const val BUNDLE_PICKED_RESOURCE_SELECTION = "PICKED_RESOURCE_SELECTION"
    }
}
