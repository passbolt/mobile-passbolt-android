package com.passbolt.mobile.android.feature.resources.userpermissionsdetails

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import coil.transform.CircleCropTransformation
import com.passbolt.mobile.android.common.FingerprintFormatter
import com.passbolt.mobile.android.common.dialogs.permissionDeletionConfirmationAlertDialog
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.common.extension.visible
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
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
        setListeners()
        presenter.attach(this)
        presenter.argsRetrieved(args.permission, args.mode)
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
                presenter.saveClick()
            }
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
        with(binding.permissionSelect) {
            visible()
            selectPermission(currentPermission, silently = true)
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

    override fun showSaveLayout() {
        binding.saveLayout.visible()
    }

    override fun setUpdatedPermissionResult(userPermission: PermissionModelUi.UserPermissionModel) {
        setFragmentResult(
            REQUEST_UPDATE_USER_PERMISSIONS,
            bundleOf(EXTRA_UPDATED_USER_PERMISSION to userPermission)
        )
    }

    override fun setDeletePermissionResult(userPermission: PermissionModelUi.UserPermissionModel) {
        setFragmentResult(
            REQUEST_UPDATE_USER_PERMISSIONS,
            bundleOf(EXTRA_DELETED_USER_PERMISSION to userPermission)
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
        const val REQUEST_UPDATE_USER_PERMISSIONS = "REQUEST_UPDATE_USER_PERMISSIONS"
        const val EXTRA_UPDATED_USER_PERMISSION = "UPDATED_PERMISSION"
        const val EXTRA_DELETED_USER_PERMISSION = "DELETED_PERMISSION"
    }
}
