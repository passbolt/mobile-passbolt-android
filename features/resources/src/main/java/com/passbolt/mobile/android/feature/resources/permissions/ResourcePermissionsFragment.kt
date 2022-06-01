package com.passbolt.mobile.android.feature.resources.permissions

import android.os.Bundle
import android.view.View
import androidx.core.view.updatePadding
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.common.extension.visible
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.resources.R
import com.passbolt.mobile.android.feature.resources.databinding.FragmentResourcePermissionsBinding
import com.passbolt.mobile.android.feature.resources.grouppermissionsdetails.GroupPermissionsFragment
import com.passbolt.mobile.android.feature.resources.permissionrecipients.PermissionRecipientsFragment
import com.passbolt.mobile.android.feature.resources.permissions.recycler.PermissionItem
import com.passbolt.mobile.android.feature.resources.userpermissionsdetails.UserPermissionsFragment
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

class ResourcePermissionsFragment :
    BindingScopedAuthenticatedFragment<FragmentResourcePermissionsBinding, ResourcePermissionsContract.View>(
        FragmentResourcePermissionsBinding::inflate
    ), ResourcePermissionsContract.View {

    override val presenter: ResourcePermissionsContract.Presenter by inject()
    private val permissionsItemAdapter: ItemAdapter<PermissionItem> by inject(named(PERMISSIONS_ITEM_ADAPTER))
    private val fastAdapter: FastAdapter<GenericItem> by inject()
    private val args: ResourcePermissionsFragmentArgs by navArgs()
    private val newRecipientsAddedListener = { _: String, bundle: Bundle ->
        presenter.shareRecipientsAdded(
            bundle.getParcelableArrayList(PermissionRecipientsFragment.EXTRA_NEW_PERMISSIONS)
        )
    }
    private val userPermissionUpdatedListener = { _: String, bundle: Bundle ->
        bundle.getParcelable<PermissionModelUi.UserPermissionModel>(
            UserPermissionsFragment.EXTRA_UPDATED_USER_PERMISSION
        )?.let { permission ->
            presenter.userPermissionModified(permission)
        }
        Unit
    }
    private val groupPermissionUpdatedListener = { _: String, bundle: Bundle ->
        bundle.getParcelable<PermissionModelUi.GroupPermissionModel>(
            GroupPermissionsFragment.EXTRA_UPDATED_GROUP_PERMISSION
        )?.let { permission ->
            presenter.groupPermissionModified(permission)
        }
        Unit
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(binding.toolbar)
        setListeners()
        initPermissionsRecycler()
        presenter.attach(this)
        presenter.argsReceived(args.resourceId, args.mode)
    }

    private fun setListeners() {
        with(binding) {
            saveButton.setDebouncingOnClick {
                presenter.saveClick()
            }
            addPermissionButton.setDebouncingOnClick {
                presenter.addPermissionClick()
            }
        }
    }

    override fun onDestroyView() {
        binding.permissionsRecycler.adapter = null
        presenter.detach()
        super.onDestroyView()
    }

    private fun initPermissionsRecycler() {
        with(binding.permissionsRecycler) {
            layoutManager = LinearLayoutManager(context)
            adapter = fastAdapter
        }
        fastAdapter.addEventHook(
            PermissionItem.ItemClick { presenter.permissionClick(it) }
        )
    }

    override fun navigateToGroupPermissionDetails(
        permission: PermissionModelUi.GroupPermissionModel,
        mode: ResourcePermissionsMode
    ) {
        setFragmentResultListener(
            GroupPermissionsFragment.EXTRA_UPDATED_GROUP_PERMISSION_BUNDLE_KEY,
            groupPermissionUpdatedListener
        )
        findNavController().navigate(
            ResourcePermissionsFragmentDirections.actionResourcePermissionsFragmentToGroupPermissionsFragment(
                permission,
                mode
            )
        )
    }

    override fun navigateToUserPermissionDetails(
        permission: PermissionModelUi.UserPermissionModel,
        mode: ResourcePermissionsMode
    ) {
        setFragmentResultListener(
            UserPermissionsFragment.EXTRA_UPDATED_USER_PERMISSION_BUNDLE_KEY,
            userPermissionUpdatedListener
        )
        findNavController().navigate(
            ResourcePermissionsFragmentDirections.actionResourcePermissionsFragmentToUserPermissionsFragment(
                permission,
                mode
            )
        )
    }

    override fun navigateToSelectShareRecipients(
        groupPermissions: List<PermissionModelUi.GroupPermissionModel>,
        userPermissions: List<PermissionModelUi.UserPermissionModel>
    ) {
        setFragmentResultListener(
            PermissionRecipientsFragment.EXTRA_NEW_PERMISSIONS_BUNDLE_KEY,
            newRecipientsAddedListener
        )
        findNavController().navigate(
            ResourcePermissionsFragmentDirections.actionResourcePermissionsFragmentToPermissionRecipientsFragment(
                userPermissions.toTypedArray(), groupPermissions.toTypedArray()
            )
        )
    }

    override fun showPermissions(permissions: List<PermissionModelUi>) {
        FastAdapterDiffUtil.calculateDiff(permissionsItemAdapter, permissions.map { PermissionItem(it) })
        fastAdapter.notifyAdapterDataSetChanged()
    }

    override fun showSaveButton() {
        with(binding) {
            saveLayout.visible()
            permissionsRecycler.updatePadding(bottom = resources.getDimension(R.dimen.dp_96).toInt())
        }
    }

    override fun showAddUserButton() {
        binding.addPermissionButton.visible()
    }
}
