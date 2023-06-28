package com.passbolt.mobile.android.feature.settings.screen.appsettings.defaultfilter

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.ISelectionListener
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.fastadapter.select.getSelectExtension
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.feature.settings.databinding.FragmentDefaultFilterBinding
import com.passbolt.mobile.android.feature.settings.screen.appsettings.defaultfilter.recycler.DefaultFilterItem
import com.passbolt.mobile.android.ui.DefaultFilterModel
import org.koin.android.ext.android.inject

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

class DefaultFilterFragment :
    BindingScopedFragment<FragmentDefaultFilterBinding>(FragmentDefaultFilterBinding::inflate),
    DefaultFilterContract.View {

    private val presenter: DefaultFilterContract.Presenter by inject()
    private val fastAdapter: FastAdapter<DefaultFilterItem> by inject()
    private val itemAdapter: ItemAdapter<DefaultFilterItem> by inject()
    private val defaultFilterSelectedListener = object : ISelectionListener<DefaultFilterItem> {
        override fun onSelectionChanged(item: DefaultFilterItem, selected: Boolean) {
            presenter.defaultFilterSelectionChanged(item.filterModel, selected)
        }
    }
    private val args: DefaultFilterFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(binding.toolbar)
        initRecycler(savedInstanceState)
        presenter.attach(this)
        presenter.argsRetrieved(args.selectedFilter)
    }

    override fun onResume() {
        super.onResume()
        presenter.viewResume()
    }

    override fun onDestroyView() {
        fastAdapter.getSelectExtension().selectionListener = null
        super.onDestroyView()
    }

    private fun initRecycler(savedInstanceState: Bundle?) {
        fastAdapter.getSelectExtension().apply {
            isSelectable = true
            multiSelect = false
            allowDeselection = false
            selectOnLongClick = false
            selectWithItemUpdate = true
            selectionListener = defaultFilterSelectedListener
            withSavedInstanceState(savedInstanceState, BUNDLE_DEFAULT_FILTER_SELECTION)
        }

        with(binding.defaultFilterRecycler) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fastAdapter
        }
    }

    override fun selectFilterSilently(selectedFilter: DefaultFilterModel) {
        with(fastAdapter.getSelectExtension()) {
            selectionListener = null
            deselect()
            val selectPosition = itemAdapter.adapterItems.indexOfFirst { it.filterModel == selectedFilter }
            select(selectPosition)
            selectionListener = defaultFilterSelectedListener
        }
    }

    override fun showFiltersList(filters: List<DefaultFilterModel>) {
        FastAdapterDiffUtil.calculateDiff(itemAdapter, filters.map { DefaultFilterItem(it) })
        fastAdapter.notifyAdapterDataSetChanged()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val withSavedSelections = fastAdapter.saveInstanceState(outState)
        super.onSaveInstanceState(withSavedSelections)
    }

    companion object {
        private const val BUNDLE_DEFAULT_FILTER_SELECTION = "BUNDLE_DEFAULT_FILTER_SELECTION"
    }
}
