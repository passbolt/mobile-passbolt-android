package com.passbolt.mobile.android.groupdetails.groupmembers

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.groupdetails.databinding.FragmentGroupMembersBinding
import com.passbolt.mobile.android.groupdetails.groupmembers.recycler.GroupMemberItem
import com.passbolt.mobile.android.ui.UserModel
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

class GroupMembersFragment :
    BindingScopedAuthenticatedFragment<FragmentGroupMembersBinding, GroupMembersContract.View>(
        FragmentGroupMembersBinding::inflate,
    ),
    GroupMembersContract.View {
    override val presenter: GroupMembersContract.Presenter by inject()

    private val groupMemberItemAdapter: ItemAdapter<GroupMemberItem> by inject(named(GROUP_MEMBER_ITEM_ADAPTER))
    private val fastAdapter: FastAdapter<GenericItem> by inject()
    private val bundledGroupId by lifecycleAwareLazy {
        requireNotNull(requireArguments().getString(EXTRA_GROUP_ID))
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(requiredBinding.toolbar)
        initGroupMembersRecycler()
        presenter.attach(this)
        presenter.argsReceived(bundledGroupId)
    }

    override fun onDestroyView() {
        requiredBinding.groupMembersRecycler.adapter = null
        presenter.detach()
        super.onDestroyView()
    }

    private fun initGroupMembersRecycler() {
        with(requiredBinding.groupMembersRecycler) {
            layoutManager = LinearLayoutManager(context)
            adapter = fastAdapter
        }
        fastAdapter.addEventHook(
            GroupMemberItem.ItemClick { presenter.groupMemberClick(it) },
        )
    }

    override fun showGroupMembers(users: List<UserModel>) {
        FastAdapterDiffUtil.calculateDiff(groupMemberItemAdapter, users.map { GroupMemberItem(it) })
        fastAdapter.notifyAdapterDataSetChanged()
    }

    override fun showGroupName(groupName: String) {
        requiredBinding.nameLabel.text = groupName
    }

    override fun navigateToGroupMemberDetails(userId: String) {
        findNavController().navigate(
            GroupMembersFragmentDirections.actionGroupMembersFragmentToGroupMemberDetailsFragment(userId),
        )
    }

    companion object {
        private const val EXTRA_GROUP_ID = "GROUP_ID"

        fun newBundle(groupId: String) =
            bundleOf(
                EXTRA_GROUP_ID to groupId,
            )
    }
}
