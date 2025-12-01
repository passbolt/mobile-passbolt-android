package com.passbolt.mobile.android.groupdetails.groupmembers

import PassboltTheme
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy

class GroupMembersFragment :
    Fragment(),
    GroupMembersNavigation {
    private val bundledGroupId by lifecycleAwareLazy {
        requireNotNull(requireArguments().getString(EXTRA_GROUP_ID))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                PassboltTheme {
                    GroupMembersScreen(
                        groupId = bundledGroupId,
                        navigation = this@GroupMembersFragment,
                    )
                }
            }
        }

    override fun navigateUp() {
        findNavController().navigateUp()
    }

    override fun navigateToMemberDetails(userId: String) {
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
