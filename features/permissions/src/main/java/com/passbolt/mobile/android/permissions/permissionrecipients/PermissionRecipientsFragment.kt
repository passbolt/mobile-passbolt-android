package com.passbolt.mobile.android.permissions.permissionrecipients

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.ISelectionListener
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.adapters.ItemFilter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.fastadapter.select.getSelectExtension
import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.core.extension.clearEndIcon
import com.passbolt.mobile.android.core.extension.gone
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.setSearchEndIconWithListener
import com.passbolt.mobile.android.core.extension.visible
import com.passbolt.mobile.android.core.ui.recyclerview.OverlappingItemDecorator
import com.passbolt.mobile.android.core.ui.recyclerview.OverlappingItemDecorator.Overlap
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.permissions.databinding.FragmentPermissionRecipientsBinding
import com.passbolt.mobile.android.permissions.permissionrecipients.recipientsrecycler.ExistingUsersAndGroupsHeaderItem
import com.passbolt.mobile.android.permissions.permissionrecipients.recipientsrecycler.GenericFilteredByConstraintListener
import com.passbolt.mobile.android.permissions.permissionrecipients.recipientsrecycler.GroupRecipientItem
import com.passbolt.mobile.android.permissions.permissionrecipients.recipientsrecycler.UserRecipientItem
import com.passbolt.mobile.android.permissions.permissions.recycler.PermissionItem
import com.passbolt.mobile.android.permissions.recycler.CounterItem
import com.passbolt.mobile.android.permissions.recycler.GroupItem
import com.passbolt.mobile.android.permissions.recycler.UserItem
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.UserModel
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import timber.log.Timber
import com.passbolt.mobile.android.core.ui.R as CoreUiR

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

class PermissionRecipientsFragment :
    BindingScopedAuthenticatedFragment<FragmentPermissionRecipientsBinding, PermissionRecipientsContract.View>(
        FragmentPermissionRecipientsBinding::inflate
    ), PermissionRecipientsContract.View {

    override val presenter: PermissionRecipientsContract.Presenter by inject()

    private val groupRecipientItemAdapter: ItemAdapter<GenericItem> by inject(named(GROUP_ITEM_ADAPTER))
    private val userRecipientItemAdapter: ItemAdapter<GenericItem> by inject(named(USER_ITEM_ADAPTER))
    private val existingUsersAndGroupsItemAdapter: ItemAdapter<PermissionItem> by inject(
        named(EXISTING_USERS_AND_GROUPS_ITEM_ADAPTER)
    )
    private val existingUsersAndGroupsHeaderItemAdapter: ItemAdapter<ExistingUsersAndGroupsHeaderItem> by inject(
        named(EXISTING_USERS_AND_GROUPS_HEADER_ITEM_ADAPTER)
    )
    private val usersAndGroupsFastAdapter: FastAdapter<GenericItem> by inject(named(USERS_AND_GROUPS_ADAPTER))
    private val alreadyAddedGroupsItemAdapter: ItemAdapter<GroupItem> by inject(
        named(ALREADY_ADDED_GROUP_ITEM_ADAPTER)
    )
    private val alreadyAddedUsersItemAdapter: ItemAdapter<UserItem> by inject(
        named(ALREADY_ADDED_USER_ITEM_ADAPTER)
    )
    private val alreadyAddedCounterItemAdapter: ItemAdapter<CounterItem> by inject(
        named(ALREADY_ADDED_COUNTER_ITEM_ADAPTER)
    )
    private val alreadyAddedFastAdapter: FastAdapter<GenericItem> by inject(named(ALREADY_ADDED_ADAPTER))
    private val alreadyAddedItemDecorator: OverlappingItemDecorator by inject()

    private val args: PermissionRecipientsFragmentArgs by navArgs()
    private val searchableMatcher: SearchableMatcher by inject()

    private val userOrGroupSelectedListener = object : ISelectionListener<GenericItem> {
        override fun onSelectionChanged(item: GenericItem, selected: Boolean) {
            when (item) {
                is GroupRecipientItem -> presenter.groupRecipientSelectionChanged(item.model, selected)
                is UserRecipientItem -> presenter.userRecipientSelectionChanged(item.model, selected)
                else -> Timber.d("Ignored selecting item: $item")
            }
        }
    }

    private val filteredByConstraintListener = GenericFilteredByConstraintListener(
        onFiltered = { constraint, resultsSize -> presenter.groupsAndUsersItemsFiltered(constraint, resultsSize) },
        onFilterReset = { presenter.groupsAndUsersFilterReset() }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(binding.toolbar)
        setListeners()
        initRecipientsRecycler(savedInstanceState)
        initAlreadyAddedRecycler()
        presenter.attach(this)
        binding.alreadyAddedRecycler.doOnLayout {
            binding.alreadyAddedRecycler.addItemDecoration(alreadyAddedItemDecorator)
            presenter.argsReceived(
                args.groupPermissions.toList(),
                args.userPermissions.toList(),
                it.width,
                resources.getDimension(CoreUiR.dimen.dp_40)
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val withSavedSelections = usersAndGroupsFastAdapter.saveInstanceState(outState)
        super.onSaveInstanceState(withSavedSelections)
    }

    override fun onDestroyView() {
        with(binding) {
            recipientsRecycler.adapter = null
            alreadyAddedRecycler.adapter = null
        }
        usersAndGroupsFastAdapter.getSelectExtension().selectionListener = null
        presenter.detach()
        super.onDestroyView()
    }

    private fun initAlreadyAddedRecycler() {
        binding.alreadyAddedRecycler.apply {
            layoutManager = object : LinearLayoutManager(context, HORIZONTAL, false) {
                override fun canScrollHorizontally() = false
            }
            adapter = alreadyAddedFastAdapter
        }
    }

    private fun setListeners() {
        with(binding) {
            searchEditText.doAfterTextChanged {
                presenter.searchTextChange(it.toString())
            }
            saveButton.setDebouncingOnClick {
                presenter.saveButtonClick()
            }
        }
    }

    private fun initRecipientsRecycler(savedInstanceState: Bundle?) {
        usersAndGroupsFastAdapter.getSelectExtension().apply {
            isSelectable = true
            multiSelect = true
            allowDeselection = true
            selectOnLongClick = false
            selectWithItemUpdate = true
            selectionListener = userOrGroupSelectedListener
            withSavedInstanceState(savedInstanceState, BUNDLE_USERS_AND_GROUPS_ADAPTER_SELECTIONS)
        }

        setupItemFilter(groupRecipientItemAdapter.itemFilter) { item: GenericItem, constraint: CharSequence? ->
            searchableMatcher.matches((item as GroupRecipientItem).model, constraint.toString())
        }
        setupItemFilter(userRecipientItemAdapter.itemFilter) { item: GenericItem, constraint: CharSequence? ->
            searchableMatcher.matches((item as UserRecipientItem).model, constraint.toString())
        }

        with(binding.recipientsRecycler) {
            layoutManager = LinearLayoutManager(context)
            adapter = usersAndGroupsFastAdapter
        }
    }

    private fun setupItemFilter(
        itemFilter: ItemFilter<GenericItem, GenericItem>,
        function: (GenericItem, CharSequence?) -> Boolean
    ) {
        itemFilter.filterPredicate = function
        itemFilter.itemFilterListener = filteredByConstraintListener
    }

    override fun filterGroupsAndUsers(searchText: String) {
        groupRecipientItemAdapter.filter(searchText)
        userRecipientItemAdapter.filter(searchText)
    }

    override fun showExistingUsersAndGroups(list: List<PermissionModelUi>) {
        FastAdapterDiffUtil.calculateDiff(
            existingUsersAndGroupsHeaderItemAdapter,
            if (list.isEmpty()) emptyList() else listOf(ExistingUsersAndGroupsHeaderItem())
        )
        FastAdapterDiffUtil.calculateDiff(
            existingUsersAndGroupsItemAdapter,
            list.map { PermissionItem(it) })
        usersAndGroupsFastAdapter.notifyAdapterDataSetChanged()
    }

    override fun showRecipients(groups: List<GroupModel>, users: List<UserModel>) {
        FastAdapterDiffUtil.calculateDiff(groupRecipientItemAdapter, groups.map { GroupRecipientItem(it) })
        FastAdapterDiffUtil.calculateDiff(userRecipientItemAdapter, users.map { UserRecipientItem(it) })
        usersAndGroupsFastAdapter.notifyAdapterDataSetChanged()
    }

    override fun showPermissions(
        groupPermissions: List<PermissionModelUi.GroupPermissionModel>,
        userPermissions: List<PermissionModelUi.UserPermissionModel>,
        counterValue: List<String>,
        overlap: Int
    ) {
        alreadyAddedItemDecorator.overlap = Overlap(left = overlap)
        FastAdapterDiffUtil.calculateDiff(alreadyAddedGroupsItemAdapter, groupPermissions.map { GroupItem(it) })
        FastAdapterDiffUtil.calculateDiff(alreadyAddedUsersItemAdapter, userPermissions.map { UserItem(it) })
        FastAdapterDiffUtil.calculateDiff(alreadyAddedCounterItemAdapter, counterValue.map { CounterItem(it) })
        alreadyAddedFastAdapter.notifyAdapterDataSetChanged()
    }

    override fun showClearSearchIcon() {
        binding.searchTextInput.setSearchEndIconWithListener(
            ContextCompat.getDrawable(requireContext(), CoreUiR.drawable.ic_close)!!,
            presenter::searchClearClick
        )
    }

    override fun hideClearSearchIcon() {
        binding.searchTextInput.clearEndIcon()
    }

    override fun clearSearch() {
        binding.searchEditText.setText("")
        userRecipientItemAdapter.filter(null)
        groupRecipientItemAdapter.filter(null)
    }

    override fun setSelectedPermissionsResult(selectedPermissions: List<PermissionModelUi>) {
        setFragmentResult(
            EXTRA_NEW_PERMISSIONS_BUNDLE_KEY,
            bundleOf(EXTRA_NEW_PERMISSIONS to ArrayList(selectedPermissions))
        )
    }

    override fun navigateBack() {
        findNavController().popBackStack()
    }

    override fun showEmptyState() {
        binding.emptyState.visible()
    }

    override fun hideEmptyState() {
        binding.emptyState.gone()
    }

    companion object {
        const val EXTRA_NEW_PERMISSIONS_BUNDLE_KEY = "EXTRA_NEW_PERMISSIONS_BUNDLE"
        const val EXTRA_NEW_PERMISSIONS = "EXTRA_NEW_PERMISSIONS"
        private const val BUNDLE_USERS_AND_GROUPS_ADAPTER_SELECTIONS = "BUNDLE_USERS_AND_GROUPS_ADAPTER_SELECTIONS"
    }
}
