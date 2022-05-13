package com.passbolt.mobile.android.feature.resources.permissions

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.resources.databinding.FragmentResourcePermissionsBinding
import com.passbolt.mobile.android.feature.resources.permissions.recycler.PermissionItem
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

class ResourcePermissionsFragment :
    BindingScopedAuthenticatedFragment<FragmentResourcePermissionsBinding, ResourcePermissionsContract.View>(
        FragmentResourcePermissionsBinding::inflate
    ), ResourcePermissionsContract.View {

    override val presenter: ResourcePermissionsContract.Presenter by inject()
    private val permissionsItemAdapter: ItemAdapter<PermissionItem> by inject(named(PERMISSIONS_ITEM_ADAPTER))
    private val fastAdapter: FastAdapter<GenericItem> by inject()
    private val args: ResourcePermissionsFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(binding.toolbar)
        initPermissionsRecycler()
        presenter.attach(this)
        presenter.argsReceived(args.resourceId)
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

    override fun navigateToGroupPermissionDetails(groupId: String, permission: ResourcePermission) {
        findNavController().navigate(
            ResourcePermissionsFragmentDirections.actionResourcePermissionsFragmentToGroupPermissionsFragment(
                groupId,
                permission
            )
        )
    }

    override fun navigateToUserPermissionDetails(userId: String, permission: ResourcePermission) {
        findNavController().navigate(
            ResourcePermissionsFragmentDirections.actionResourcePermissionsFragmentToUserPermissionsFragment(
                userId,
                permission
            )
        )
    }

    override fun showPermissions(permissions: List<PermissionModelUi>) {
        FastAdapterDiffUtil.calculateDiff(permissionsItemAdapter, permissions.map { PermissionItem(it) })
        fastAdapter.notifyAdapterDataSetChanged()
    }
}
