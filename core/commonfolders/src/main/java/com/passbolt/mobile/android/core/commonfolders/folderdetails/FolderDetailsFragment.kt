package com.passbolt.mobile.android.core.commonfolders.folderdetails

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.view.doOnLayout
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.commonfolders.R
import com.passbolt.mobile.android.commonfolders.databinding.FragmentFolderDetailsBinding
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.ui.recyclerview.OverlappingItemDecorator
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.permissions.permissions.NavigationOrigin
import com.passbolt.mobile.android.permissions.permissions.PermissionsItem
import com.passbolt.mobile.android.permissions.permissions.PermissionsMode
import com.passbolt.mobile.android.permissions.recycler.CounterItem
import com.passbolt.mobile.android.permissions.recycler.GroupItem
import com.passbolt.mobile.android.permissions.recycler.UserItem
import com.passbolt.mobile.android.ui.PermissionModelUi
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

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

class FolderDetailsFragment :
    BindingScopedAuthenticatedFragment<FragmentFolderDetailsBinding, FolderDetailsContract.View>(
        FragmentFolderDetailsBinding::inflate
    ), FolderDetailsContract.View {

    override val presenter: FolderDetailsContract.Presenter by inject()
    private val args: FolderDetailsFragmentArgs by navArgs()
    private val groupPermissionsItemAdapter: ItemAdapter<GroupItem> by inject(named(GROUP_ITEM_ADAPTER))
    private val userPermissionsItemAdapter: ItemAdapter<UserItem> by inject(named(USER_ITEM_ADAPTER))
    private val permissionsCounterItemAdapter: ItemAdapter<CounterItem> by inject(named(COUNTER_ITEM_ADAPTER))
    private val fastAdapter: FastAdapter<GenericItem> by inject()
    private val sharedWithFields
        get() = listOf(binding.sharedWithLabel, binding.sharedWithRecyclerClickableArea, binding.sharedWithNavIcon)
    private val locationFields
        get() = listOf(binding.locationHeader, binding.locationValue, binding.locationNavIcon)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(binding.toolbar)
        initSharedWithRecycler()
        initListeners()
        presenter.attach(this)
        binding.sharedWithRecycler.doOnLayout {
            presenter.argsRetrieved(
                args.folderId,
                it.width,
                resources.getDimension(R.dimen.dp_40)
            )
        }
    }

    private fun initListeners() {
        sharedWithFields.forEach { it.setDebouncingOnClick { presenter.sharedWithClick() } }
        locationFields.forEach { it.setDebouncingOnClick { presenter.locationClick() } }
        fastAdapter.onClickListener = { _, _, _, _ ->
            presenter.sharedWithClick()
            true
        }
    }

    private fun initSharedWithRecycler() {
        with(binding.sharedWithRecycler) {
            layoutManager = object : LinearLayoutManager(context, HORIZONTAL, false) {
                override fun canScrollHorizontally() = false
            }
            adapter = fastAdapter
        }
    }

    override fun showFolderName(name: String) {
        binding.name.text = name
        binding.folderNameValue.text = name
    }

    override fun showFolderSharedIcon() {
        binding.icon.setImageResource(R.drawable.ic_filled_shared_folder_with_bg)
    }

    override fun showFolderIcon() {
        binding.icon.setImageResource(R.drawable.ic_filled_folder_with_bg)
    }

    override fun showFolderLocation(parentFolders: List<String>) {
        binding.locationValue.text = parentFolders.let {
            val mutable = it.toMutableList()
            mutable.add(0, getString(R.string.folder_root))
            mutable.joinToString(separator = " %s ".format(getString(R.string.folder_details_location_separator)))
        }
    }

    override fun showPermissions(
        groupPermissions: List<PermissionModelUi.GroupPermissionModel>,
        userPermissions: List<PermissionModelUi.UserPermissionModel>,
        counterValue: List<String>,
        overlap: Int
    ) {
        val decorator = OverlappingItemDecorator(OverlappingItemDecorator.Overlap(left = overlap))
        binding.sharedWithRecycler.addItemDecoration(decorator)
        FastAdapterDiffUtil.calculateDiff(groupPermissionsItemAdapter, groupPermissions.map { GroupItem(it) })
        FastAdapterDiffUtil.calculateDiff(userPermissionsItemAdapter, userPermissions.map { UserItem(it) })
        FastAdapterDiffUtil.calculateDiff(permissionsCounterItemAdapter, counterValue.map { CounterItem(it) })
        fastAdapter.notifyAdapterDataSetChanged()
    }

    override fun navigateToFolderPermissions(folderId: String, mode: PermissionsMode) {
        val request = NavDeepLinkRequest.Builder
            .fromUri(
                Uri.Builder()
                    .scheme("passbolt")
                    .authority("permissions")
                    .appendPath(PermissionsItem.FOLDER.name)
                    .appendPath(folderId)
                    .appendQueryParameter("mode", mode.name)
                    .appendQueryParameter("navigationOrigin", NavigationOrigin.RESOURCE_DETAILS_SCREEN.name)
                    .build()
            ).build()
        findNavController().navigate(request)
    }

    override fun navigateToFolderLocation(folderId: String) {
        findNavController().navigate(
            FolderDetailsFragmentDirections.actionFolderDetailsFragmentToFolderLocationDetailsFragment(folderId)
        )
    }
}
