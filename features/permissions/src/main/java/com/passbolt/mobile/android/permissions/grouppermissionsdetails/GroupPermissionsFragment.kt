package com.passbolt.mobile.android.permissions.grouppermissionsdetails

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.fragment.app.setFragmentResult
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.passbolt.mobile.android.common.dialogs.permissionDeletionConfirmationAlertDialog
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.visible
import com.passbolt.mobile.android.core.ui.recyclerview.OverlappingItemDecorator
import com.passbolt.mobile.android.core.ui.recyclerview.OverlappingItemDecorator.Overlap
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.permissions.databinding.FragmentGroupPermissionsBinding
import com.passbolt.mobile.android.groupdetails.groupmembers.GroupMembersFragment
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.membersrecycler.GroupUserItem
import com.passbolt.mobile.android.permissions.recycler.CounterItem
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.UserModel
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import com.passbolt.mobile.android.core.ui.R as CoreUiR
import com.passbolt.mobile.android.feature.groupdetails.R as GroupDetailsR

class GroupPermissionsFragment :
    BindingScopedAuthenticatedFragment<FragmentGroupPermissionsBinding, GroupPermissionsContract.View>(
        FragmentGroupPermissionsBinding::inflate
    ), GroupPermissionsContract.View {

    override val presenter: GroupPermissionsContract.Presenter by inject()
    private val args: GroupPermissionsFragmentArgs by navArgs()
    private val groupMembersItemAdapter: ItemAdapter<GroupUserItem> by inject(named(GROUP_MEMBER_ITEM_ADAPTER))
    private val counterItemAdapter: ItemAdapter<CounterItem> by inject(named(COUNTER_ITEM_ADAPTER))
    private val fastAdapter: FastAdapter<GenericItem> by inject()
    private val sharedWithDecorator: OverlappingItemDecorator by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(binding.toolbar)
        setListeners()
        setupGroupMembersRecycler()
        presenter.attach(this)
        binding.groupMembersRecycler.doOnLayout {
            binding.groupMembersRecycler.addItemDecoration(sharedWithDecorator)
            presenter.argsRetrieved(
                args.permission,
                args.mode,
                it.width,
                resources.getDimension(CoreUiR.dimen.dp_40)
            )
        }
    }

    private fun setListeners() {
        with(binding) {
            permissionSelect.onPermissionSelectedListener = {
                presenter.onPermissionSelected(it)
            }
            deletePermissionButton.setDebouncingOnClick {
                presenter.deletePermissionClick()
            }
            saveButton.setDebouncingOnClick {
                presenter.saveButtonClick()
            }
            listOf(groupMembersRecyclerClickableArea, groupMembersNavIcon)
                .forEach {
                    it.setDebouncingOnClick { presenter.groupMembersRecyclerClick() }
                }
        }
    }

    override fun onDestroyView() {
        binding.groupMembersRecycler.adapter = null
        presenter.detach()
        super.onDestroyView()
    }

    private fun setupGroupMembersRecycler() {
        fastAdapter.onClickListener = { _, _, _, _ ->
            presenter.groupMembersRecyclerClick()
            true
        }
        with(binding.groupMembersRecycler) {
            layoutManager = object : LinearLayoutManager(context, HORIZONTAL, false) {
                override fun canScrollHorizontally() = false
            }
            adapter = fastAdapter
        }
    }

    override fun showPermission(permission: ResourcePermission) {
        with(binding.permissionLabel) {
            visible()
            text = ResourcePermission.getPermissionTextValue(context, permission)
            setCompoundDrawablesWithIntrinsicBounds(
                ResourcePermission.getPermissionIcon(context, permission),
                null,
                null,
                null
            )
        }
    }

    override fun showPermissionChoices(currentPermission: ResourcePermission) {
        with(binding) {
            permissionSelect.visible()
            permissionSelect.selectPermission(currentPermission, silently = true)
        }
    }

    override fun showSaveLayout() {
        binding.saveLayout.visible()
    }

    override fun showGroupName(groupName: String) {
        binding.nameLabel.text = groupName
    }

    override fun showGroupUsers(
        users: List<UserModel>,
        counterValue: List<String>,
        overlapOffset: Int
    ) {
        sharedWithDecorator.overlap = Overlap(left = overlapOffset)
        FastAdapterDiffUtil.calculateDiff(groupMembersItemAdapter, users.map { GroupUserItem((it)) })
        FastAdapterDiffUtil.calculateDiff(counterItemAdapter, counterValue.map { CounterItem((it)) })
        fastAdapter.notifyAdapterDataSetChanged()
    }

    override fun navigateToGroupMembers(groupId: String) {
        findNavController().navigate(
            GroupDetailsR.id.group_members, GroupMembersFragment.newBundle(groupId),
            NavOptions.Builder()
                .setEnterAnim(CoreUiR.anim.slide_in_right)
                .setExitAnim(CoreUiR.anim.slide_out_left)
                .setPopEnterAnim(CoreUiR.anim.slide_in_left)
                .setPopExitAnim(CoreUiR.anim.slide_out_right)
                .build()
        )
    }

    override fun setUpdatedPermissionResult(permission: PermissionModelUi.GroupPermissionModel) {
        setFragmentResult(
            REQUEST_UPDATE_GROUP_PERMISSION,
            bundleOf(EXTRA_UPDATED_GROUP_PERMISSION to permission)
        )
    }

    override fun setDeletePermissionResult(groupPermission: PermissionModelUi.GroupPermissionModel) {
        setFragmentResult(
            REQUEST_UPDATE_GROUP_PERMISSION,
            bundleOf(EXTRA_DELETED_GROUP_PERMISSION to groupPermission)
        )
    }

    override fun showPermissionDeleteConfirmation() {
        permissionDeletionConfirmationAlertDialog(requireContext()) {
            presenter.permissionDeleteConfirmClick()
        }
            .show()
    }

    override fun navigateBack() {
        findNavController().popBackStack()
    }

    companion object {
        const val REQUEST_UPDATE_GROUP_PERMISSION = "REQUEST_UPDATE_GROUP_PERMISSION"
        const val EXTRA_UPDATED_GROUP_PERMISSION = "UPDATED_GROUP_PERMISSION"
        const val EXTRA_DELETED_GROUP_PERMISSION = "DELETED_GROUP_PERMISSION"
    }
}
