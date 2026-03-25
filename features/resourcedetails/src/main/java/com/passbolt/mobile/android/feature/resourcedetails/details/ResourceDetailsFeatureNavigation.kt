package com.passbolt.mobile.android.feature.resourcedetails.details

import PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceDetailsNavigationKey.ResourceDetails
import com.passbolt.mobile.android.core.navigation.compose.results.NavigationResultEventBus
import com.passbolt.mobile.android.core.navigation.compose.results.ResultEffect
import com.passbolt.mobile.android.core.navigation.compose.results.ShareCompleteResult

class ResourceDetailsFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<ResourceDetails> { key ->
                val resultBus = NavigationResultEventBus.current

                ResultEffect<ShareCompleteResult> { result ->
                    if (result.shared) {
                        resultBus.sendResult(
                            result = ShareCompleteResult(shared = true),
                        )
                    }
                }

                PassboltTheme {
                    ResourceDetailsScreen(
                        resourceModel = key.resourceModel,
                    )
                }
            }
        }
}
