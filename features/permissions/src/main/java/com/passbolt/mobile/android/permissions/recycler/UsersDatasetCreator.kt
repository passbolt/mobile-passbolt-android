package com.passbolt.mobile.android.permissions.recycler

import com.passbolt.mobile.android.core.ui.recyclerview.OverlapCalculator
import com.passbolt.mobile.android.ui.UserModel

class UsersDatasetCreator(
    private val membersRecyclerWidth: Int,
    private val membersItemWidth: Float,
) {
    fun prepareDataset(users: List<UserModel>): Output {
        val overlapCalculationResult =
            OverlapCalculator(
                membersRecyclerWidth,
                membersItemWidth,
                users.size,
            ).calculateLeftOverlapOffset()

        return if (overlapCalculationResult.allItemsFit) {
            Output(
                users,
                emptyList(),
                overlapCalculationResult.overlap,
            )
        } else {
            val visibleItems = users.subList(0, overlapCalculationResult.visibleItems - 2)
            val moreItemsCount = users.size - visibleItems.size
            val counterValue =
                if (moreItemsCount < MAX_DISPLAYABLE_COUNT) {
                    moreItemsCount.toString()
                } else {
                    "99+"
                }
            Output(
                visibleItems,
                listOf(counterValue),
                overlapCalculationResult.overlap,
            )
        }
    }

    data class Output(
        val users: List<UserModel>,
        val counterValue: List<String>,
        val overlap: Int,
    )

    private companion object {
        private const val MAX_DISPLAYABLE_COUNT = 99
    }
}
