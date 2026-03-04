package com.passbolt.mobile.android.feature.resourcedetails.details

import PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.LocationDetailsNavigationKey.LocationDetails
import com.passbolt.mobile.android.core.navigation.compose.keys.LocationDetailsNavigationKey.LocationItem
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.Permissions
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceDetailsNavigationKey.ResourceDetails
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.MainResourceForm
import com.passbolt.mobile.android.core.navigation.compose.keys.TagsDetailsNavigationKey.ResourceTags
import com.passbolt.mobile.android.core.navigation.compose.results.NavigationResultEventBus
import com.passbolt.mobile.android.core.navigation.compose.results.PermissionsShareCompleteResult
import com.passbolt.mobile.android.core.navigation.compose.results.ResourceDetailsCompleteResult
import com.passbolt.mobile.android.core.navigation.compose.results.ResourceFormCompleteResult
import com.passbolt.mobile.android.core.navigation.compose.results.ResultEffect
import com.passbolt.mobile.android.core.navigation.compose.results.ResultEventBus
import com.passbolt.mobile.android.ui.PermissionsItem
import com.passbolt.mobile.android.ui.PermissionsMode
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.ResourceModel
import org.koin.compose.koinInject

class ResourceDetailsFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<ResourceDetails> { key ->
                val navigator: AppNavigator = koinInject()
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
                ResultEffect<PermissionsShareCompleteResult> { result ->
                    if (result.shared) {
                        resultBus.sendResult(
                            result = PermissionsShareCompleteResult(shared = true),
                        )
                    }
                }

                PassboltTheme {
                    ResourceDetailsScreen(
                        resourceModel = key.resourceModel,
                        navigation = ResourceDetailsNavigator(navigator, resultBus),
                    )
                }
            }
        }
}

private class ResourceDetailsNavigator(
    private val navigator: AppNavigator,
    private val resultBus: ResultEventBus,
) : ResourceDetailsNavigation {
    override fun navigateBack() {
        navigator.navigateBack()
    }

    override fun navigateToEditResource(resourceModel: ResourceModel) {
        navigator.navigateToKey(
            MainResourceForm(
                ResourceFormMode.Edit(
                    resourceId = resourceModel.resourceId,
                    resourceName = resourceModel.metadataJsonModel.name,
                ),
            ),
        )
    }

    override fun navigateToResourcePermissions(
        resourceId: String,
        mode: PermissionsMode,
    ) {
        navigator.navigateToKey(
            Permissions(resourceId, mode, PermissionsItem.RESOURCE),
        )
    }

    override fun navigateToResourceTags(resourceId: String) {
        navigator.navigateToKey(
            ResourceTags(resourceId),
        )
    }

    override fun navigateToResourceLocation(resourceId: String) {
        navigator.navigateToKey(
            LocationDetails(LocationItem.RESOURCE, resourceId),
        )
    }

    override fun closeWithDeleteSuccessResult(resourceName: String) {
        resultBus.sendResult(
            result =
                ResourceDetailsCompleteResult(
                    resourceEdited = false,
                    resourceDeleted = true,
                    resourceName = resourceName,
                ),
        )
        navigator.navigateBack()
    }

    override fun setResourceEditedResult(resourceName: String) {
        resultBus.sendResult(
            result =
                ResourceDetailsCompleteResult(
                    resourceEdited = true,
                    resourceDeleted = false,
                    resourceName = resourceName,
                ),
        )
    }
}
