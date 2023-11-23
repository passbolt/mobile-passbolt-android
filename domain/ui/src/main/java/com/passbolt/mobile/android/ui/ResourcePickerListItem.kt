package com.passbolt.mobile.android.ui

import com.passbolt.mobile.android.common.search.Searchable

data class ResourcePickerListItem(
    val resourceModel: ResourceModel,
    val selection: Selection,
    val isSelected: Boolean
) : Searchable by resourceModel {

    val isSelectable = selection == Selection.SELECTABLE

    enum class Selection {
        SELECTABLE,
        NOT_SELECTABLE_NO_PERMISSION,
        NOT_SELECTABLE_UNSUPPORTED_RESOURCE_TYPE
    }
}
