package com.passbolt.mobile.android.createfolder

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.core.ui.recyclerview.OverlappingItemDecorator
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput
import com.passbolt.mobile.android.createfolder.databinding.FragmentCreateFolderBinding
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
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

class CreateFolderFragment :
    BindingScopedAuthenticatedFragment<FragmentCreateFolderBinding, CreateFolderContract.View>(
        FragmentCreateFolderBinding::inflate
    ), CreateFolderContract.View {

    override val presenter: CreateFolderContract.Presenter by inject()
    private val args: CreateFolderFragmentArgs by navArgs()
    private val groupPermissionsItemAdapter: ItemAdapter<GroupItem> by inject(named(GROUP_ITEM_ADAPTER))
    private val userPermissionsItemAdapter: ItemAdapter<UserItem> by inject(named(USER_ITEM_ADAPTER))
    private val permissionsCounterItemAdapter: ItemAdapter<CounterItem> by inject(named(COUNTER_ITEM_ADAPTER))
    private val fastAdapter: FastAdapter<GenericItem> by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(binding.toolbar)
        initSharedWithRecycler()
        setListeners()
        presenter.attach(this)
        binding.sharedWithRecycler.doOnLayout {
            presenter.argsRetrieved(
                args.parentFolderId,
                it.width,
                resources.getDimension(R.dimen.dp_40)
            )
        }
    }

    private fun setListeners() {
        with(binding) {
            folderNameInput.setTextChangeListener { presenter.folderNameChanged(it) }
            saveButton.setDebouncingOnClick { presenter.saveClick() }
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

    override fun showFolderNameIsRequired() {
        binding.folderNameInput.setState(
            StatefulInput.State.Error(getString(R.string.create_folder_name_validation))
        )
    }

    override fun clearValidationErrors() {
        binding.folderNameInput.setState(StatefulInput.State.Default)
    }

    override fun showProgress() {
        showProgressDialog(childFragmentManager)
    }

    override fun hideProgress() {
        hideProgressDialog(childFragmentManager)
    }

    override fun showCreateFolderError(errorMessage: String) {
        Snackbar.make(binding.root, getString(R.string.create_folder_error_format, errorMessage), Snackbar.LENGTH_LONG)
            .show()
    }

    override fun showShareFailure(errorMessage: String) {
        Snackbar.make(binding.root, getString(R.string.share_folder_error_format, errorMessage), Snackbar.LENGTH_LONG)
            .show()
    }

    override fun setFolderCreatedResultAndClose(folderName: String) {
        setFragmentResult(
            REQUEST_CREATE_FOLDER,
            bundleOf(EXTRA_CREATED_FOLDER_NAME to folderName)
        )
        findNavController().popBackStack()
    }

    companion object {
        const val REQUEST_CREATE_FOLDER = "REQUEST_CREATE_FOLDER"
        const val EXTRA_CREATED_FOLDER_NAME = "CREATED_FOLDER_NAME"
    }
}
