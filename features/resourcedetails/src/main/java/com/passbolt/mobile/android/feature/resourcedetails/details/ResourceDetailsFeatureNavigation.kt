package com.passbolt.mobile.android.feature.resourcedetails.details

import PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceDetailsNavigationKey.ResourceDetails
import com.passbolt.mobile.android.core.navigation.compose.results.NavigationResultEventBus
import com.passbolt.mobile.android.core.navigation.compose.results.ResourceDetailsCompleteResult
import com.passbolt.mobile.android.core.navigation.compose.results.ResourceFormCompleteResult
import com.passbolt.mobile.android.core.navigation.compose.results.ResultEffect
import com.passbolt.mobile.android.core.navigation.compose.results.ShareCompleteResult

class ResourceDetailsFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<ResourceDetails> { key ->
                val resultBus = NavigationResultEventBus.current

                ResultEffect<ResourceFormCompleteResult> { result ->
                    if (result.resourceEdited && result.resourceName != null) {
                        resultBus.sendResult(
                            result =
                                ResourceDetailsCompleteResult(
                                    resourceEdited = true,
                                    resourceDeleted = false,
                                    resourceName = result.resourceName,
                                ),
                        )
                    }
                }
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
