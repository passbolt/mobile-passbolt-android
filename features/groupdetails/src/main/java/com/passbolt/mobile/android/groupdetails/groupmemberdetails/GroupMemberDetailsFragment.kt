package com.passbolt.mobile.android.groupdetails.groupmemberdetails

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import coil.load
import coil.transform.CircleCropTransformation
import com.passbolt.mobile.android.common.FingerprintFormatter
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.groupdetails.databinding.FragmentGroupMemberDetailsBinding
import com.passbolt.mobile.android.ui.UserModel
import org.koin.android.ext.android.inject
import com.passbolt.mobile.android.core.ui.R as CoreUiR

class GroupMemberDetailsFragment :
    BindingScopedAuthenticatedFragment<FragmentGroupMemberDetailsBinding, GroupMemberDetailsContract.View>(
        FragmentGroupMemberDetailsBinding::inflate
    ), GroupMemberDetailsContract.View {

    override val presenter: GroupMemberDetailsContract.Presenter by inject()
    private val args: GroupMemberDetailsFragmentArgs by navArgs()
    private val fingerprintFormatter: FingerprintFormatter by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(binding.toolbar)
        presenter.attach(this)
        presenter.argsRetrieved(args.userId)
    }

    override fun showUserData(user: UserModel) {
        with(binding) {
            nameLabel.text = String.format("%s %s", user.profile.firstName, user.profile.lastName)
            emailLabel.text = user.userName
            fingerprintLabel.text = fingerprintFormatter.formatWithRawFallback(user.gpgKey.fingerprint)
            avatarImage.load(user.profile.avatarUrl) {
                error(CoreUiR.drawable.ic_user_avatar)
                transformations(CircleCropTransformation())
                placeholder(CoreUiR.drawable.ic_user_avatar)
            }
        }
    }
}
