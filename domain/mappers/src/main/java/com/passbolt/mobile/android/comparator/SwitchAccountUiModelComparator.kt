package com.passbolt.mobile.android.comparator

import com.passbolt.mobile.android.ui.SwitchAccountUiModel

class SwitchAccountUiModelComparator : Comparator<SwitchAccountUiModel> {

    override fun compare(current: SwitchAccountUiModel, other: SwitchAccountUiModel) =
        order.indexOf(current::class.java).compareTo(order.indexOf(other::class.java))

    private companion object {
        private val order = listOf(
            SwitchAccountUiModel.HeaderItem::class.java,
            SwitchAccountUiModel.AccountItem::class.java,
            SwitchAccountUiModel.ManageAccountsItem::class.java
        )
    }
}
