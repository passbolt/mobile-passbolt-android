package com.passbolt.mobile.android.core.navigation.compose.keys

import androidx.navigation3.runtime.NavKey
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.serialization.Serializable

sealed interface ResourceDetailsNavigationKey : NavKey {
    @Serializable
    data class ResourceDetails(
        // Pass ResourceModel instead of resourceId to load initial resource details screen
        // even if refresh in progress and Resources table currently empty
        val resourceModel: ResourceModel,
    ) : ResourceDetailsNavigationKey
}
