package com.passbolt.mobile.android.feature.resources.userpermissionsdetails

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import coil.load
import coil.transform.CircleCropTransformation
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.common.FingerprintFormatter
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.resources.R
import com.passbolt.mobile.android.feature.resources.databinding.FragmentUserPermissionsBinding
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.UserModel
import org.koin.android.ext.android.inject

class UserPermissionsFragment :
    BindingScopedAuthenticatedFragment<FragmentUserPermissionsBinding, UserPermissionsContract.View>(
        FragmentUserPermissionsBinding::inflate
    ), UserPermissionsContract.View {

    override val presenter: UserPermissionsContract.Presenter by inject()
    private val args: UserPermissionsFragmentArgs by navArgs()
    private val fingerprintFormatter: FingerprintFormatter by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(binding.toolbar)
        presenter.attach(this)
        presenter.argsRetrieved(args.userId, args.permission)
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

    override fun showUserData(user: UserModel) {
        with(binding) {
            nameLabel.text = String.format("%s %s", user.profile.firstName, user.profile.lastName)
            emailLabel.text = user.userName
            fingerprintLabel.text = fingerprintFormatter.formatWithRawFallback(user.gpgKey.fingerprint)
            avatarImage.load(user.profile.avatarUrl) {
                error(R.drawable.ic_user_avatar)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.ic_user_avatar)
            }
        }
    }
}