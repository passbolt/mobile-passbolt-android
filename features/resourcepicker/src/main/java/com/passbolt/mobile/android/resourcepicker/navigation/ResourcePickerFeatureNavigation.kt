package com.passbolt.mobile.android.resourcepicker.navigation

import PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.ResourcePicker
import com.passbolt.mobile.android.core.navigation.compose.results.NavigationResultEventBus
import com.passbolt.mobile.android.core.navigation.compose.results.ResourcePickerResultEvent
import com.passbolt.mobile.android.core.navigation.compose.results.ResultEventBus
import com.passbolt.mobile.android.resourcepicker.ResourcePickerNavigation
import com.passbolt.mobile.android.resourcepicker.model.PickResourceAction
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerScreen
import com.passbolt.mobile.android.ui.ResourceModel
import org.koin.compose.koinInject

class ResourcePickerFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<ResourcePicker> { key ->
                val navigator: AppNavigator = koinInject()
                val resultBus = NavigationResultEventBus.current

                PassboltTheme {
                    ResourcePickerScreen(
                        suggestionUri = key.suggestionUri,
                        navigation = ResourcePickerNavigator(navigator, resultBus),
                    )
                }
            }
        }
}

private class ResourcePickerNavigator(
    private val navigator: AppNavigator,
    private val resultBus: ResultEventBus,
) : ResourcePickerNavigation {
    override fun navigateUp() {
        navigator.navigateBack()
    }

    override fun navigateBackWithResult(
        pickAction: PickResourceAction,
        resourceModel: ResourceModel,
    ) {
        resultBus.sendResult(
            result =
                ResourcePickerResultEvent(
                    pickAction = pickAction.name,
                    resource = resourceModel,
                ),
        )
        navigator.navigateBack()
    }
}
