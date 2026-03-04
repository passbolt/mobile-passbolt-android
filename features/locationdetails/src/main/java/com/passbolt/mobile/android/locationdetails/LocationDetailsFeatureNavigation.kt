package com.passbolt.mobile.android.locationdetails

import PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.LocationDetailsNavigationKey.LocationDetails
import com.passbolt.mobile.android.locationdetails.ui.LocationItem
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.navigation.compose.keys.LocationDetailsNavigationKey.LocationItem as NavigationLocationItem

class LocationDetailsFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<LocationDetails> { key ->
                val navigator: AppNavigator = koinInject()

                PassboltTheme {
                    LocationDetailsScreen(
                        locationItem = key.locationItem.toFeatureLocationItem(),
                        itemId = key.itemId,
                        navigation = LocationDetailsNavigator(navigator),
                    )
                }
            }
        }
}

private fun NavigationLocationItem.toFeatureLocationItem(): LocationItem =
    when (this) {
        NavigationLocationItem.RESOURCE -> LocationItem.RESOURCE
        NavigationLocationItem.FOLDER -> LocationItem.FOLDER
    }

private class LocationDetailsNavigator(
    private val navigator: AppNavigator,
) : LocationDetailsNavigation {
    override fun navigateUp() {
        navigator.navigateBack()
    }

    override fun navigateToHome() {
        navigator.popToKey(navigator.backStack.first())
    }
}
