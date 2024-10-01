package com.passbolt.mobile.android.permissions.permissions

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.passbolt.mobile.android.core.extension.gone
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.core.extension.visible
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.ActivityResults
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.permissions.databinding.FragmentResourcePermissionsBinding
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsFragment
import com.passbolt.mobile.android.permissions.permissionrecipients.PermissionRecipientsFragment
import com.passbolt.mobile.android.permissions.permissions.recycler.PermissionItem
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsFragment
import com.passbolt.mobile.android.ui.PermissionModelUi
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import com.passbolt.mobile.android.core.localization.R as LocalizationR
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

@Suppress("TooManyFunctions")
class PermissionsFragment :
    BindingScopedAuthenticatedFragment<FragmentResourcePermissionsBinding, PermissionsContract.View>(
        FragmentResourcePermissionsBinding::inflate
    ), PermissionsContract.View {

    override val presenter: PermissionsContract.Presenter by inject()
    private val permissionsItemAdapter: ItemAdapter<PermissionItem> by inject(named(PERMISSIONS_ITEM_ADAPTER))
    private val fastAdapter: FastAdapter<GenericItem> by inject()
    private val args: PermissionsFragmentArgs by navArgs()
    private val snackbarAnchorView: View
        get() = binding.addPermissionButton
    private val newRecipientsAddedListener = { _: String, bundle: Bundle ->
        presenter.shareRecipientsAdded(
            BundleCompat.getParcelableArrayList(
                bundle,
                PermissionRecipientsFragment.EXTRA_NEW_PERMISSIONS,
                PermissionModelUi::class.java
            )
        )
    }
    private val userPermissionUpdatedListener = { _: String, bundle: Bundle ->
        BundleCompat.getParcelable(
            bundle,
            UserPermissionsFragment.EXTRA_UPDATED_USER_PERMISSION,
            PermissionModelUi.UserPermissionModel::class.java
        )?.let { permission ->
            presenter.userPermissionModified(permission)
        }

        BundleCompat.getParcelable(
            bundle,
            UserPermissionsFragment.EXTRA_DELETED_USER_PERMISSION,
            PermissionModelUi.UserPermissionModel::class.java
        )?.let { permission ->
            presenter.userPermissionDeleted(permission)
        }

        Unit
    }
    private val groupPermissionUpdatedListener = { _: String, bundle: Bundle ->
        BundleCompat.getParcelable(
            bundle,
            GroupPermissionsFragment.EXTRA_UPDATED_GROUP_PERMISSION,
            PermissionModelUi.GroupPermissionModel::class.java
        )?.let { permission ->
            presenter.groupPermissionModified(permission)
        }

        BundleCompat.getParcelable(
            bundle,
            GroupPermissionsFragment.EXTRA_DELETED_GROUP_PERMISSION,
            PermissionModelUi.GroupPermissionModel::class.java
        )?.let { permission ->
            presenter.groupPermissionDeleted(permission)
        }

        Unit
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(binding.toolbar)
        setListeners()
        initPermissionsRecycler()
        presenter.attach(this)
        presenter.argsReceived(args.permissionsItem, args.id, args.mode)
    }

    override fun onResume() {
        super.onResume()
        presenter.resume(this)
    }

    override fun onPause() {
        presenter.pause()
        super.onPause()
    }

    private fun setListeners() {
        with(binding) {
            actionButton.setDebouncingOnClick {
                presenter.actionButtonClick()
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
        mode: PermissionsMode
    ) {
        setFragmentResultListener(
            GroupPermissionsFragment.REQUEST_UPDATE_GROUP_PERMISSION,
            groupPermissionUpdatedListener
        )
        findNavController().navigate(
            PermissionsFragmentDirections.actionResourcePermissionsFragmentToGroupPermissionsFragment(
                permission,
                mode
            )
        )
    }

    override fun navigateToUserPermissionDetails(
        permission: PermissionModelUi.UserPermissionModel,
        mode: PermissionsMode
    ) {
        setFragmentResultListener(
            UserPermissionsFragment.REQUEST_UPDATE_USER_PERMISSIONS,
            userPermissionUpdatedListener
        )
        findNavController().navigate(
            PermissionsFragmentDirections.actionResourcePermissionsFragmentToUserPermissionsFragment(
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
            PermissionsFragmentDirections.actionResourcePermissionsFragmentToPermissionRecipientsFragment(
                userPermissions.toTypedArray(), groupPermissions.toTypedArray()
            )
        )
    }

    override fun navigateToSelfWithMode(resourceId: String, mode: PermissionsMode) {
        findNavController().navigate(
            PermissionsFragmentDirections.actionResourcePermissionsFragmentSelf(
                resourceId, mode, args.navigationOrigin, args.permissionsItem
            )
        )
    }

    override fun showPermissions(permissions: List<PermissionModelUi>) {
        FastAdapterDiffUtil.calculateDiff(permissionsItemAdapter, permissions.map { PermissionItem(it) })
        fastAdapter.notifyAdapterDataSetChanged()
    }

    override fun showSaveButton() {
        showActionButtonLayout()
        binding.actionButton.text = getString(LocalizationR.string.save)
    }

    private fun showActionButtonLayout() {
        with(binding) {
            permissionsRecycler.updatePadding(bottom = resources.getDimension(CoreUiR.dimen.dp_96).toInt())
            actionButtonLayout.visible()
        }
    }

    override fun showEditButton() {
        showActionButtonLayout()
        binding.actionButton.text = getString(LocalizationR.string.resource_permissions_edit_permissions)
    }

    override fun showOneOwnerSnackbar() {
        showSnackbar(
            LocalizationR.string.resource_permissions_one_owner,
            anchorView = snackbarAnchorView,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showAddUserButton() {
        binding.addPermissionButton.visible()
    }

    override fun showEmptyState() {
        binding.emptyState.visible()
    }

    override fun hideEmptyState() {
        binding.emptyState.gone()
    }

    override fun showShareSimulationFailure() {
        showSnackbar(
            LocalizationR.string.resource_permissions_share_simulation_failed,
            anchorView = snackbarAnchorView,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showShareFailure() {
        showSnackbar(
            LocalizationR.string.resource_permissions_share_failed,
            anchorView = snackbarAnchorView,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showSecretFetchFailure() {
        showSnackbar(
            LocalizationR.string.common_fetch_failure,
            anchorView = snackbarAnchorView,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showSecretEncryptFailure() {
        showSnackbar(
            LocalizationR.string.common_encryption_failure,
            anchorView = snackbarAnchorView,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showSecretDecryptFailure() {
        showSnackbar(
            LocalizationR.string.common_decryption_failure,
            anchorView = snackbarAnchorView,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showProgress() {
        showProgressDialog(childFragmentManager)
    }

    override fun hideProgress() {
        hideProgressDialog(childFragmentManager)
    }

    override fun closeWithShareSuccessResult() {
        when (args.navigationOrigin) {
            NavigationOrigin.RESOURCE_DETAILS_SCREEN -> {
                setFragmentResult(REQUEST_UPDATE_PERMISSIONS, bundleOf())
                findNavController().popBackStack()
            }
            NavigationOrigin.HOME_RESOURCE_MORE_MENU -> {
                with(requireActivity()) {
                    setResult(ActivityResults.RESULT_RESOURCE_SHARED)
                    finish()
                }
            }
        }
    }

    override fun showDataRefreshError() {
        showSnackbar(
            LocalizationR.string.common_data_refresh_error,
            anchorView = snackbarAnchorView,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun hideRefreshProgress() {
        binding.fullScreenProgressLayout.gone()
    }

    override fun showRefreshProgress() {
        binding.fullScreenProgressLayout.visible()
    }

    override fun showContentNotAvailable() {
        Toast.makeText(requireContext(), LocalizationR.string.content_not_available, Toast.LENGTH_SHORT).show()
    }

    override fun navigateToHome() {
        requireActivity().startActivity(ActivityIntents.bringHome(requireContext()))
    }

    companion object {
        const val REQUEST_UPDATE_PERMISSIONS = "REQUEST_UPDATE_PERMISSIONS"
    }
}
