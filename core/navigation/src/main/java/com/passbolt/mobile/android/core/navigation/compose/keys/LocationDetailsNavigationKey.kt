package com.passbolt.mobile.android.core.navigation.compose.keys

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface LocationDetailsNavigationKey : NavKey {
    enum class LocationItem {
        RESOURCE,
        FOLDER,
    }

    @Serializable
    data class LocationDetails(
        val locationItem: LocationItem,
        val itemId: String,
    ) : LocationDetailsNavigationKey
}
