package com.passbolt.mobile.android.tagsdetails

import PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.TagsDetailsNavigationKey.ResourceTags

class TagsDetailsFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<ResourceTags> { key ->
                PassboltTheme {
                    ResourceTagsScreen(
                        resourceId = key.resourceId,
                    )
                }
            }
        }
}
