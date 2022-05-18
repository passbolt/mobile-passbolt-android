package com.passbolt.mobile.android.feature.resources.permissionavatarlist

import com.passbolt.mobile.android.core.ui.recyclerview.OverlapCalculator
import com.passbolt.mobile.android.ui.PermissionModelUi

class PermissionsDatasetCreator(
    private val permissionsListWidth: Int,
    private val permissionItemWidth: Float
) {

    fun prepareDataset(permissions: List<PermissionModelUi>): Output {
        require(permissionsListWidth > 0) { "Permissions list width < 0" }
        require(permissionItemWidth > 0) { "Permissions item width < 0" }

        val overlapCalculationResult = OverlapCalculator(
            permissionsListWidth, permissionItemWidth, permissions.size
        )
            .calculateLeftOverlapOffset()

        return if (overlapCalculationResult.allItemsFit) {
            Output(
                permissions.filterIsInstance<PermissionModelUi.GroupPermissionModel>(),
                permissions.filterIsInstance<PermissionModelUi.UserPermissionModel>(),
                emptyList(),
                overlapCalculationResult.overlap
            )
        } else {
            val visibleItems = permissions.subList(0, overlapCalculationResult.visibleItems - 2)
            val moreItemsCount = permissions.size - visibleItems.size
            val counterValue = if (moreItemsCount < 99) {
                moreItemsCount.toString()
            } else {
                "99+"
            }
            Output(
                visibleItems.filterIsInstance<PermissionModelUi.GroupPermissionModel>(),
                visibleItems.filterIsInstance<PermissionModelUi.UserPermissionModel>(),
                listOf(counterValue),
                overlapCalculationResult.overlap
            )
        }
    }

    data class Output(
        val groupPermissions: List<PermissionModelUi.GroupPermissionModel>,
        val userPermissions: List<PermissionModelUi.UserPermissionModel>,
        val counterValue: List<String>,
        val overlap: Int
    )
}
