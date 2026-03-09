package com.passbolt.mobile.android.locationdetails

import PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.LocationDetailsNavigationKey.LocationDetails
import com.passbolt.mobile.android.locationdetails.ui.LocationItem
import com.passbolt.mobile.android.core.navigation.compose.keys.LocationDetailsNavigationKey.LocationItem as NavigationLocationItem

class LocationDetailsFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<LocationDetails> { key ->
                PassboltTheme {
                    LocationDetailsScreen(
                        locationItem = key.locationItem.toFeatureLocationItem(),
                        itemId = key.itemId,
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
