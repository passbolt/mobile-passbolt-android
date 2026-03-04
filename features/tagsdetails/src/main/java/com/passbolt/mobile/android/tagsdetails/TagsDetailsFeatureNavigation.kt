package com.passbolt.mobile.android.tagsdetails

import PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.TagsDetailsNavigationKey.ResourceTags
import org.koin.compose.koinInject

class TagsDetailsFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<ResourceTags> { key ->
                val navigator: AppNavigator = koinInject()

                PassboltTheme {
                    ResourceTagsScreen(
                        resourceId = key.resourceId,
                        navigation = ResourceTagsNavigator(navigator),
                    )
                }
            }
        }
}

private class ResourceTagsNavigator(
    private val navigator: AppNavigator,
) : ResourceTagsNavigation {
    override fun navigateUp() {
        navigator.navigateBack()
    }

    override fun navigateToHome() {
        navigator.popToKey(navigator.backStack.first())
    }
}
