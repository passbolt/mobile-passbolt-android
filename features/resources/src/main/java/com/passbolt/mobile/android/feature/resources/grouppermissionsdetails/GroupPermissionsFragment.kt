package com.passbolt.mobile.android.feature.resources.grouppermissionsdetails

import android.os.Bundle
import android.view.View
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.passbolt.mobile.android.core.commongroups.groupmembers.GroupMembersFragment
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.resources.R
import com.passbolt.mobile.android.feature.resources.databinding.FragmentGroupPermissionsBinding
import com.passbolt.mobile.android.feature.resources.grouppermissionsdetails.membersrecycler.GroupUserItem
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.UserModel
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

class GroupPermissionsFragment :
    BindingScopedAuthenticatedFragment<FragmentGroupPermissionsBinding, GroupPermissionsContract.View>(
        FragmentGroupPermissionsBinding::inflate
    ), GroupPermissionsContract.View {

    override val presenter: GroupPermissionsContract.Presenter by inject()
    private val args: GroupPermissionsFragmentArgs by navArgs()
    private val groupMembersItemAdapter: ItemAdapter<GroupUserItem> by inject(named(GROUP_MEMBER_ITEM_ADAPTER))
    private val fastAdapter: FastAdapter<GenericItem> by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(binding.toolbar)
        setupGroupMembersRecycler()
        presenter.attach(this)
        presenter.argsRetrieved(args.groupId, args.permission)
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
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = fastAdapter
        }
    }

    override fun showPermission(permission: ResourcePermission) {
        with(binding.permissionLabel) {
            text = PermissionModelUi.getPermissionTextValue(context, permission)
            setCompoundDrawablesWithIntrinsicBounds(
                PermissionModelUi.getPermissionIcon(context, permission),
                null,
                null,
                null
            )
        }
    }

    override fun showGroupName(groupName: String) {
        binding.nameLabel.text = groupName
    }

    override fun showGroupUsers(users: List<UserModel>) {
        FastAdapterDiffUtil.calculateDiff(groupMembersItemAdapter, users.map { GroupUserItem((it)) })
        fastAdapter.notifyAdapterDataSetChanged()
    }

    override fun navigateToGroupMembers(groupId: String) {
        findNavController().navigate(
            R.id.group_members, GroupMembersFragment.newBundle(groupId),
            NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_right)
                .setExitAnim(R.anim.slide_out_left)
                .setPopEnterAnim(R.anim.slide_in_left)
                .setPopExitAnim(R.anim.slide_out_right)
                .build()
        )
    }
}
