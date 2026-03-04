package com.passbolt.mobile.android.folderdetails

import PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.FolderDetailsNavigationKey.FolderDetails
import com.passbolt.mobile.android.core.navigation.compose.keys.LocationDetailsNavigationKey.LocationDetails
import com.passbolt.mobile.android.core.navigation.compose.keys.LocationDetailsNavigationKey.LocationItem
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.Permissions
import com.passbolt.mobile.android.ui.PermissionsItem
import com.passbolt.mobile.android.ui.PermissionsMode
import org.koin.compose.koinInject

class FolderDetailsFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<FolderDetails> { key ->
                val navigator: AppNavigator = koinInject()

                PassboltTheme {
                    FolderDetailsScreen(
                        folderId = key.folderId,
                        navigation = FolderDetailsNavigator(navigator),
                    )
                }
            }
        }
}

private class FolderDetailsNavigator(
    private val navigator: AppNavigator,
) : FolderDetailsNavigation {
    override fun navigateUp() {
        navigator.navigateBack()
    }

    override fun navigateToHome() {
        navigator.popToKey(navigator.backStack.first())
    }

    override fun navigateToFolderPermissions(
        folderId: String,
        mode: PermissionsMode,
    ) {
        navigator.navigateToKey(
            Permissions(folderId, mode, PermissionsItem.FOLDER),
        )
    }

    override fun navigateToFolderLocation(folderId: String) {
        navigator.navigateToKey(
            LocationDetails(LocationItem.FOLDER, folderId),
        )
    }
}
