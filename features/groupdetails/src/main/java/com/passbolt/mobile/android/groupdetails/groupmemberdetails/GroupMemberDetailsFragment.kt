package com.passbolt.mobile.android.groupdetails.groupmemberdetails

import PassboltTheme
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs

class GroupMemberDetailsFragment :
    Fragment(),
    GroupMemberDetailsNavigation {
    private val args: GroupMemberDetailsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                PassboltTheme {
                    GroupMemberDetailsScreen(
                        userId = args.userId,
                        navigation = this@GroupMemberDetailsFragment,
                    )
                }
            }
        }

    override fun navigateUp() {
        findNavController().navigateUp()
    }
}
