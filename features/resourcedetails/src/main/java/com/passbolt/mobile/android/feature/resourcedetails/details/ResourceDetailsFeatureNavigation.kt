package com.passbolt.mobile.android.feature.resourcedetails.details

import PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceDetailsNavigationKey.ResourceDetails

class ResourceDetailsFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<ResourceDetails> { key ->
                PassboltTheme {
                    ResourceDetailsScreen(
                        resourceModel = key.resourceModel,
                    )
                }
            }
        }
}
